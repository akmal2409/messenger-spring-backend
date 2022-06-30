package com.akmal.messengerspringbackend.service;

import com.akmal.messengerspringbackend.config.kafka.KafkaConfigurationProperties;
import com.akmal.messengerspringbackend.config.websocket.WebSocketConfiguration;
import com.akmal.messengerspringbackend.shared.datastructure.Tuple;
import com.akmal.messengerspringbackend.thread.ThreadEventKey;
import com.akmal.messengerspringbackend.thread.ThreadMessageEvent;
import com.akmal.messengerspringbackend.websocket.dto.MessageEventDto;
import com.akmal.messengerspringbackend.websocket.storage.WebsocketSessionStorage;
import java.util.Collection;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 18/06/2022 - 17:12
 * @project messenger-spring-backend
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageDeliveryService {
  private final KafkaTemplate<SpecificRecordBase, SpecificRecordBase> threadEventsTemplate;
  private final KafkaConfigurationProperties kafkaProps;

  private final WebsocketSessionStorage sessionStorage;
  private final SimpMessagingTemplate wsMessagingTemplate;

  /**
   * Delivers the message to the active user when invoked. In case, the user is not online, i.e.
   * his/her websocket session is not registered in the registry, then the message is dropped. (In
   * the future push notification must be sent). Otherwise, if the user is online and is subscribed
   * to the messages for the particular thread, then we deliver to the queue with full path {@link
   * WebSocketConfiguration#THREAD_TOPIC} + '/{threadId}'. On the other hand, if the user is online
   * but is not subscribed to a particular topic, we deliver the message as a notification to the
   * following queue {@link WebSocketConfiguration#NOTIFICATION_TOPIC}.
   *
   * @param userId - recipient of the message.
   * @param messageEvent
   */
  @Async
  public void handleIncomingMessageEvent(String userId, ThreadMessageEvent messageEvent) {
    if (!this.sessionStorage.isUserConnected(userId)) return;
    else {
      final var threadTopicName =
          WebSocketConfiguration.THREAD_TOPIC.concat(
              String.format("/%s", messageEvent.getThreadId()));
      String destination = null;

      if (this.sessionStorage.isUserSubscribedTo(userId, threadTopicName)) {
        destination = threadTopicName.replace("/user", "");
      } else {
        destination = WebSocketConfiguration.NOTIFICATION_TOPIC.replace("/user", "");
      }

      this.wsMessagingTemplate.convertAndSendToUser(
          userId, destination, MessageEventDto.fromThreadMessageEvent(messageEvent));
    }
  }

  /**
   * Sends message to all users that are not excluded from the delivery and are part of the
   * recipients list. For each user separate message is prepared and inserted into the kafka topic.
   *
   * @param metadataList collection of the following properties: - threadId id of the thread for
   *     which fanout is activated. - threadName name of the thread (depending on the side of the
   *     chat). - recipients ids of the recipients. - excludeUsersFromDelivery the users, who should
   *     not get the message (example: author of the message). - messageBody content of the message.
   *     - messageId snowflake of the message. - authorId id of the author. - authorName name of the
   *     author (full) . - bucket the time bucket index where the message was inserted.
   */
  @Async
  public void fanoutMessages(Collection<FanoutMessageMetadata> metadataList) {

    for (FanoutMessageMetadata metadata : metadataList) {
      final var messageRecord = this.prepareMessageEvent(metadata);

      this.threadEventsTemplate.send(
          this.kafkaProps.getTopics().getThreadEvents(), messageRecord.e1(), messageRecord.e2());
    }
  }

  private Tuple<SpecificRecordBase, SpecificRecordBase> prepareMessageEvent(
      FanoutMessageMetadata metadata) {
    final var key =
        ThreadEventKey.newBuilder()
            .setThreadId(metadata.threadId.toString())
            .setUid(metadata.recipientId)
            .build();

    final var value =
        ThreadMessageEvent.newBuilder()
            .setMessageId(metadata.messageId)
            .setAuthorId(metadata.authorId)
            .setBucket(metadata.bucket)
            .setBody(metadata.body)
            .setAuthorName(metadata.authorName)
            .setThreadName(metadata.threadName)
            .setToUser(metadata.recipientId)
            .setThreadId(metadata.threadId.toString())
            .build();

    return new Tuple<>(key, value);
  }

  @Builder
  record FanoutMessageMetadata(
      @NotNull UUID threadId,
      @Nullable String threadName,
      @NotNull String recipientId,
      long messageId,
      @Nullable String authorId,
      @Nullable String authorName,
      int bucket,
      @Nullable String body,
      int systemMessage) {}
}
