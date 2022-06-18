package com.akmal.messengerspringbackend.model.udt;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.With;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 28/05/2022 - 13:59
 * @project messenger-spring-backend
 * @since 1.0
 */
@UserDefinedType("user")
@Data
@Builder
@With
public class UserUDT {
  @Column("uid")
  private final String uid;
  @Column("first_name")
  private final String firstName;
  @Column("last_name")
  private final String lastName;
  @Column("profile_image_url")
  private final String profileImageUrl;
  @Column("profile_thumbnail_url")
  private final String profileThumbnailUrl;

  public String getFullName() {
    return String.format("%s %s", this.firstName, this.lastName);
  }
}
