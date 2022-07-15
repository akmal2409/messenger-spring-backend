package com.akmal.messengerspringbackend.websocket.controller;

import com.akmal.messengerspringbackend.service.UserPresenceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * Websocket message handler for user presence event such as typing and heartbeat.
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 13/07/2022 - 18:54
 * @project messenger-spring-backend
 * @since 1.0
 */
@Controller
@RequiredArgsConstructor
public class WsThreadPresenceController {

  public static final String BASE_TOPIC_PREFIX = "/users/{userId}/threads/{threadId}";
  private final UserPresenceService userPresenceService;

  @MessageMapping(BASE_TOPIC_PREFIX + "/typing")
  public void handleTypingEvent(
      @DestinationVariable String userId,
      @DestinationVariable UUID threadId
  ) {
    this.userPresenceService.sendTypingEvent(userId, threadId);
  }
}
