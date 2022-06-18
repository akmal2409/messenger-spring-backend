package com.akmal.messengerspringbackend.service;

import com.akmal.messengerspringbackend.config.kafka.KafkaConfigurationProperties;
import com.akmal.messengerspringbackend.shared.datastructure.Tuple;
import com.akmal.messengerspringbackend.thread.ThreadEventKey;
import com.akmal.messengerspringbackend.thread.ThreadMessageEvent;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.kafka.core.KafkaTemplate;
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
public class MessageDeliveryService {
  private final KafkaTemplate<SpecificRecordBase, SpecificRecordBase> threadEventsTemplate;
  private final KafkaConfigurationProperties kafkaProps;

  @Builder
  static record FanoutMessageMetadata(
      @NotNull UUID threadId,
      @Nullable String threadName,
      @NotNull UUID recipientId,
      long messageId,
      @NotNull UUID authorId,
      @Nullable String authorName,
      int bucket,
      @Nullable String body,
      int systemMessage
  ) {}

  /**
   * Sends message to all users that are not excluded from the delivery and
   * are part of the recipients list.
   * For each user separate message is prepared and inserted into the kafka topic.
   *
   * @param metadataList collection of the following properties:
   * - threadId id of the thread for which fanout is activated.
   * - threadName name of the thread (depending on the side of the chat).
   * - recipients ids of the recipients.
   * - excludeUsersFromDelivery the users, who should not get the message (example: author of the message).
   * - messageBody content of the message.
   * - messageId snowflake of the message.
   * - authorId id of the author.
   * - authorName name of the author (full)      .
   * - bucket the time bucket index where the message was inserted.
   */
  @Async
  public void fanoutMessages(Collection<FanoutMessageMetadata> metadataList) {

    for (FanoutMessageMetadata metadata: metadataList) {
      final var messageRecord =
          this.prepareMessageEvent(metadata);

      this.threadEventsTemplate.send(this.kafkaProps.getTopics().getThreadEvents(),
          messageRecord.e1(), messageRecord.e2());
    }
  }

  private Tuple<SpecificRecordBase, SpecificRecordBase> prepareMessageEvent(
      FanoutMessageMetadata metadata
  ) {
    final var key = ThreadEventKey.newBuilder()
                        .setThreadId(metadata.threadId.toString())
                        .setUid(metadata.recipientId.toString())
                        .build();

    final var value = ThreadMessageEvent.newBuilder()
                          .setMessageId(metadata.messageId)
                          .setAuthorId(metadata.authorId.toString())
                          .setBucket(metadata.bucket)
                          .setBody(metadata.body)
                          .setAuthorName(metadata.authorName)
                          .setThreadName(metadata.threadName)
                          .setToUser(metadata.recipientId.toString())
                          .setThreadId(metadata.threadId.toString())
                          .build();

    return new Tuple<>(key, value);
  }
}
