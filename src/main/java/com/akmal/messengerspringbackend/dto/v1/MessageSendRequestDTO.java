package com.akmal.messengerspringbackend.dto.v1;

import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.With;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 28/05/2022 - 16:03
 * @project messenger-spring-backend
 * @since 1.0
 */

@With
public record MessageSendRequestDTO(
    String body,
    @NotNull(message = "Thread ID is missing") UUID threadId
) {

}
