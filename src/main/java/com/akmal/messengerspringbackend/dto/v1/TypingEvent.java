package com.akmal.messengerspringbackend.dto.v1;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 13/07/2022 - 19:35
 * @project messenger-spring-backend
 * @since 1.0
 */
public record TypingEvent(
    String authorId,
    String threadId
) {

}
