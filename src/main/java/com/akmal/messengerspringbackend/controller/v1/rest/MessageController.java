package com.akmal.messengerspringbackend.controller.v1.rest;

import com.akmal.messengerspringbackend.dto.v1.MessageSendRequestDTO;
import com.akmal.messengerspringbackend.dto.v1.MessageSentResponseDTO;
import com.akmal.messengerspringbackend.service.MessageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 28/05/2022 - 16:09
 * @project messenger-spring-backend
 * @since 1.0
 */
@RestController
@RequestMapping(MessageController.BASE_API)
@RequiredArgsConstructor
public class MessageController {
  public static final String BASE_API = "/api/v1/users/{userId}/threads/{threadId}/messages";
  private final MessageService messageService;

  @PostMapping
  public MessageSentResponseDTO sendMessage(
      @PathVariable UUID userId,
      @PathVariable UUID threadId,
      @RequestBody MessageSendRequestDTO messageRequest) {
    return this.messageService.sendMessage(threadId, userId, messageRequest);
  }
}
