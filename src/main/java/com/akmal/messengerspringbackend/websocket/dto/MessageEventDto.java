package com.akmal.messengerspringbackend.websocket.dto;

import com.akmal.messengerspringbackend.thread.ThreadMessageEvent;
import java.util.UUID;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 26/06/2022 - 16:30
 * @project messenger-spring-backend
 * @since 1.0
 */
public record MessageEventDto(
    long messageId,
    String threadId,
    String threadName,
    int bucket,
    String authorId,
    String authorName,
    String body,
    boolean read,
    boolean edited,
    boolean systemMessage
) {

  public static MessageEventDto fromThreadMessageEvent(ThreadMessageEvent messageEvent) {
    return new MessageEventDto(
        messageEvent.getMessageId(),
        messageEvent.getThreadId().toString(),
        messageEvent.getThreadName().toString(),
        messageEvent.getBucket(),
        messageEvent.getAuthorId().toString(),
        messageEvent.getAuthorName().toString(),
        messageEvent.getBody().toString(),
        messageEvent.getRead(),
        messageEvent.getEdited(),
        messageEvent.getSystemMessage()
    );
  }
}
