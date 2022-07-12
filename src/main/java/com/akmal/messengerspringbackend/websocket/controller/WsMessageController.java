package com.akmal.messengerspringbackend.websocket.controller;

import com.akmal.messengerspringbackend.dto.v1.MessageSendRequestDTO;
import com.akmal.messengerspringbackend.service.MessageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

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
  public static final String BASE_PATH = "/users/{userId}/threads/{threadId}/messages";
  private final MessageService messageService;

  @MessageMapping(BASE_PATH)
  public void sendMessage(@Payload MessageSendRequestDTO message,
      @DestinationVariable String userId, @DestinationVariable UUID threadId) {

    final var messageDto = this.messageService.sendMessage(threadId, userId, message);

    if (StringUtils.hasText(message.receiptId())) {
      this.messageService.acknowledgeMessage(message.receiptId(), userId, messageDto);
    }
  }

}
