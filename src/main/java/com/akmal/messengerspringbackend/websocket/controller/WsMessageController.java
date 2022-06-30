package com.akmal.messengerspringbackend.websocket.controller;

import com.akmal.messengerspringbackend.dto.v1.MessageSendRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 18/06/2022 - 13:11
 * @project messenger-spring-backend
 * @since 1.0
 */
@Controller
@RequiredArgsConstructor
public class WsMessageController {

  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/send-message")
  public void sendMessageWs2(@Payload MessageSendRequestDTO message) {
    System.out.println("Recieved message " + message);

    this.messagingTemplate.convertAndSendToUser(
        SecurityContextHolder.getContext().getAuthentication().getName(),
        "/queue/notifications",
        message);
  }

  @SubscribeMapping("/topic/greeting")
  public String yes() {
    return "YESSS";
  }
}
