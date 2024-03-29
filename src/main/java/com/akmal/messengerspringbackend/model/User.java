package com.akmal.messengerspringbackend.model;

import com.akmal.messengerspringbackend.model.udt.UserUDT;
import com.akmal.messengerspringbackend.shared.idp.IdpUserMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.With;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Frozen;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 21/05/2022 - 16:10
 * @project messenger-spring-backend
 * @since 1.0z
 */
@With
@Builder
@Data
@Table("users")
public class User {
  @PrimaryKey("uid")
  private final String uid;

  @Column("first_name")
  private final String firstName;

  @Column("last_name")
  private final String lastName;

  @Column("email")
  private final String email;

  @Column("contacts")
  private final Set<@Frozen UserUDT> contacts;

  @Column("thread_ids")
  private final Set<UUID> threadIds;

  @Column("profile_thumbnail_url")
  private final String profileThumbnailUrl;

  @Column("profile_image_url")
  private final String profileImageUrl;

  @Column("last_seen_at")
  private final Instant lastSeenAt;

  public User(
      String uid,
      String firstName,
      String lastName,
      String email,
      Set<UserUDT> contacts,
      Set<UUID> threadIds,
      String profileThumbnailUrl,
      String profileImageUrl,
      Instant lastSeenAt) {
    this.uid = uid;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.contacts = Optional.ofNullable(contacts).orElse(Collections.emptySet());
    this.threadIds = Optional.ofNullable(threadIds).orElse(Collections.emptySet());
    this.profileThumbnailUrl = profileThumbnailUrl;
    this.profileImageUrl = profileImageUrl;
    this.lastSeenAt = lastSeenAt;
  }

  public static User fromIdpMetadata(IdpUserMetadata idpUserMetadata) {
    return User.builder()
        .uid(idpUserMetadata.uid())
        .firstName(idpUserMetadata.firstName())
        .lastName(idpUserMetadata.lastName())
        .email(idpUserMetadata.email())
        .contacts(Collections.emptySet())
        .build();
  }

  @JsonIgnore
  public String getFullName() {
    return String.format("%s %s", this.firstName, this.lastName);
  }

  public UserUDT toUDT() {
    return UserUDT.builder()
        .uid(this.uid)
        .firstName(this.firstName)
        .lastName(this.lastName)
        .profileThumbnailUrl(this.profileThumbnailUrl)
        .profileImageUrl(this.profileImageUrl)
        .build();
  }
}
