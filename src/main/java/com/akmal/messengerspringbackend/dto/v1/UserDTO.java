package com.akmal.messengerspringbackend.dto.v1;

import com.akmal.messengerspringbackend.model.User;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 02/07/2022 - 17:58
 * @project messenger-spring-backend
 * @since 1.0
 */
public record UserDTO(
    String uid,
    String firstName,
    String lastName,
    String email,
    String profileThumbnailUrl,
    String profileImageUrl
) {

  public static UserDTO from(User user) {
    return new UserDTO(
        user.getUid(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getProfileThumbnailUrl(),
        user.getProfileImageUrl()
    );
  }
}
