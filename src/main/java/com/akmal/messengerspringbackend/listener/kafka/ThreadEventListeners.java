package com.akmal.messengerspringbackend.listener.kafka;

import com.akmal.messengerspringbackend.dto.v1.TypingEvent;
import com.akmal.messengerspringbackend.service.MessageDeliveryService;
import com.akmal.messengerspringbackend.service.UserPresenceService;
import com.akmal.messengerspringbackend.thread.ThreadEventKey;
import com.akmal.messengerspringbackend.thread.ThreadMessageEvent;
import com.akmal.messengerspringbackend.thread.ThreadPresenceEvent;
import com.akmal.messengerspringbackend.websocket.storage.WebsocketSessionStorage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 07/06/2022 - 19:18
 * @project messenger-spring-backend
 * @since 1.0
 */
@Component
@KafkaListener(
    id = "${project.kafka.group-id}",
    topics = "${project.kafka.topics.thread-events}",
    containerFactory = "kafkaListenerContainerFactoryAvroKeyAvroValue"
)
@Slf4j
@RequiredArgsConstructor
public class ThreadEventListeners {
  private final MessageDeliveryService messageDeliveryService;
  private final UserPresenceService presenceService;
  private final SimpMessagingTemplate wsMessagingTemplate;
  private final WebsocketSessionStorage websocketSessionStorage;

  @KafkaHandler
  public void listenToEvents(@Payload final SpecificRecord threadEvent,
      @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY)  SpecificRecord key) {
      log.info("Received message event {}", (threadEvent));
      ThreadEventKey parsedKey = (ThreadEventKey) key;
      switch(threadEvent) {
        case ThreadMessageEvent e -> this.messageDeliveryService.handleIncomingMessageEvent(parsedKey.getUid().toString(), e);
        case ThreadPresenceEvent e -> this.handlePresenceEvent(parsedKey, e);
        default -> log.info("type=error; reason=Unknown thread event received; value={}; key={}", threadEvent, parsedKey);
      }
  }


  private void handlePresenceEvent(ThreadEventKey key, ThreadPresenceEvent presenceEvent) {
    switch (presenceEvent.getType()) {
      case TYPING: this.notifyUserOfTypingEvent(key.getUid().toString(),
          presenceEvent.getUid().toString(), UUID.fromString(key.getThreadId().toString()));
    }
  }

  /**
   * Sends typing presence notification to the user.
   *
   * @param userId for whom the event was destined.
   * @param authorId the person who triggered the event.
   * @param threadId thread in which the event happened.
   */
  public void notifyUserOfTypingEvent(@NotNull String userId,
      @NotNull String authorId, @NotNull UUID threadId) {

    if (this.websocketSessionStorage.isUserConnected(userId)) {
      this.wsMessagingTemplate.convertAndSendToUser(userId,
          "/queue/typing", new TypingEvent(authorId, threadId.toString()));
    }
  }
}
