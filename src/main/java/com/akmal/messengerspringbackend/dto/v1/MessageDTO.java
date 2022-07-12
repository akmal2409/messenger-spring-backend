package com.akmal.messengerspringbackend.dto.v1;

import java.time.LocalDateTime;
import lombok.With;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 03/06/2022 - 19:32
 * @project messenger-spring-backend
 * @since 1.0
 */
@With
public record MessageDTO(
    long messageId,
    String threadId,
    int bucket,
    String authorId,
    String body,
    LocalDateTime timestamp,
    boolean read,
    boolean edited,
    boolean systemMessage
) {

}
