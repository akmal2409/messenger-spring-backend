package com.akmal.messengerspringbackend.dto.v1;

import java.util.Collection;
import javax.validation.constraints.NotEmpty;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 27/06/2022 - 20:22
 * @project messenger-spring-backend
 * @since 1.0
 */
public record ThreadCreationRequest(
    @NotEmpty(message = "List of invitees must not be empty") Collection<String> inviteeIds,
    String threadName,
    boolean groupThread
) {

}
