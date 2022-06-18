package com.akmal.messengerspringbackend.shared.idp;

import lombok.Builder;

/**
 * Represents common set of fields that should be returned from the
 * identity provider.
 *
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 18/06/2022 - 18:04
 * @project messenger-spring-backend
 * @since 1.0
 */
@Builder
public record IdpUserMetadata(
    String uid,
    String firstName,
    String lastName,
    String email
) {

}
