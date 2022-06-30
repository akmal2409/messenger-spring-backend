package com.akmal.messengerspringbackend.dto.v1;

import com.akmal.messengerspringbackend.model.udt.UserUDT;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 26/06/2022 - 17:56
 * @project messenger-spring-backend
 * @since 1.0
 */
public record UserDetailsDTO(
    String uid,
    String name,
    String profileImageUrl,
    String profileThumbnailUrl
) {

  public static UserDetailsDTO from(UserUDT userUDT) {
    return new UserDetailsDTO(
        userUDT.getUid(),
        userUDT.getFullName(),
        userUDT.getProfileImageUrl(),
        userUDT.getProfileImageUrl()
    );
  }
}
