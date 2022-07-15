package com.akmal.messengerspringbackend.websocket.controller;

import com.akmal.messengerspringbackend.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 14/07/2022 - 16:46
 * @project messenger-spring-backend
 * @since 1.0
 */
@Controller
@RequiredArgsConstructor
public class WsUserPresenceController {
  public static final String BASE_PATH = "/users/{userId}";

  private final UserPresenceService userPresenceService;

  @MessageMapping(BASE_PATH + "/heartbeat")
  public void handleHeartBeat(
      @DestinationVariable String userId
  ) {
    this.userPresenceService.sendUserPresenceEvent(userId);
  }
}
