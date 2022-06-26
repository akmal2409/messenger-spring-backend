package com.akmal.messengerspringbackend.listener.kafka;

import com.akmal.messengerspringbackend.service.MessageDeliveryService;
import com.akmal.messengerspringbackend.thread.ThreadEventKey;
import com.akmal.messengerspringbackend.thread.ThreadMessageEvent;
import com.akmal.messengerspringbackend.thread.ThreadPresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
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
    topics = "${project.kafka.topics.thread-events}"
)
@Slf4j
@RequiredArgsConstructor
public class ThreadEventListeners {
  private final MessageDeliveryService messageDeliveryService;

  @KafkaHandler
  public void listenToEvents(@Payload final SpecificRecord threadEvent,
      @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) final ThreadEventKey key) {
      log.info("Received message event {}", (threadEvent));

      switch(threadEvent) {
        case ThreadMessageEvent e -> this.messageDeliveryService.handleIncomingMessageEvent(key.getUid().toString(), e);
        case ThreadPresenceEvent e -> this.handlePresenceEvent(key, e);
        default -> log.info("type=error; reason=Unknown thread event received; value={}; key={}", threadEvent, key);
      }
  }

  private void handleMessageEvent(ThreadEventKey key, ThreadMessageEvent messageEvent) {
    //TODO: implement message delivery
  }

  private void handlePresenceEvent(ThreadEventKey key, ThreadPresenceEvent presenceEvent) {
    //TODO: update user activity in the DB, notify the other participant if online; else send notification
  }
}
