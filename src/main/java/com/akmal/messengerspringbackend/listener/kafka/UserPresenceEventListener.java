package com.akmal.messengerspringbackend.listener.kafka;

import com.akmal.messengerspringbackend.dto.v1.UserPresenceEventDTO;
import com.akmal.messengerspringbackend.user.UserPresenceEvent;
import com.akmal.messengerspringbackend.websocket.storage.WebsocketSessionStorage;
import lombok.RequiredArgsConstructor;
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
 * @created 14/07/2022 - 18:36
 * @project messenger-spring-backend
 * @since 1.0
 */
@KafkaListener(
    topics = "${project.kafka.topics.user-presence}",
    containerFactory = "kafkaListenerContainerFactoryStringKeyAvroValue",
    id = "userPresenceContainerListener"
)
@RequiredArgsConstructor
@Component
public class UserPresenceEventListener {
  private final WebsocketSessionStorage websocketSessionStorage;
  private final SimpMessagingTemplate wsMessagingTemplate;

  @KafkaHandler
  public void handleUserPresenceEvent(
      @Payload UserPresenceEvent userPresenceEvent,
      @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String uid
  ) {
    if (userPresenceEvent != null && uid != null && this.websocketSessionStorage.isUserConnected(uid)) {
      this.wsMessagingTemplate.convertAndSendToUser(uid, "/queue/user-presence",
          UserPresenceEventDTO.fromUserPresenceEvent(userPresenceEvent));
    }
  }
}
