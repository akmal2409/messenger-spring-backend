package com.akmal.messengerspringbackend.dto.v1;

import java.util.UUID;
import lombok.With;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 28/05/2022 - 16:11
 * @project messenger-spring-backend
 * @since 1.0
 */
@With
public record MessageSentResponseDTO(
    MessageStatus status,
    long messageId,
    UUID threadId,
    String body
) {

}
