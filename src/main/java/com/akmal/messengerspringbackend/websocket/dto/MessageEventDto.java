package com.akmal.messengerspringbackend.websocket.dto;

import com.akmal.messengerspringbackend.thread.ThreadMessageEvent;
import java.time.LocalDateTime;
import lombok.With;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 26/06/2022 - 16:30
 * @project messenger-spring-backend
 * @since 1.0
 */
@With
public record MessageEventDto(
    long messageId,
    String threadId,
    String threadName,
    int bucket,
    String authorId,
    String authorName,
    String body,
    LocalDateTime timestamp,
    boolean read,
    boolean edited,
    boolean systemMessage
) {

  public static MessageEventDto fromThreadMessageEvent(ThreadMessageEvent messageEvent, LocalDateTime timestamp) {
    return new MessageEventDto(
        messageEvent.getMessageId(),
        messageEvent.getThreadId().toString(),
        messageEvent.getThreadName().toString(),
        messageEvent.getBucket(),
        messageEvent.getAuthorId().toString(),
        messageEvent.getAuthorName().toString(),
        messageEvent.getBody().toString(),
        timestamp,
        messageEvent.getRead(),
        messageEvent.getEdited(),
        messageEvent.getSystemMessage()
    );
  }
}
