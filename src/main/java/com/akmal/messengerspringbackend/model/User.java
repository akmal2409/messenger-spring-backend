package com.akmal.messengerspringbackend.model;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.With;
import org.springframework.data.cassandra.core.mapping.Column;
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
  private final UUID uid;
  @Column("first_name")
  private final String firstName;
  @Column("last_name")
  private final String lastName;
  @Column("email")
  private final String email;
  @Column("contact_ids")
  private final Set<UUID> contactIds;
  @Column("thread_ids")
  private final Set<UUID> threadIds;
  @Column("profile_thumbnail_url")
  private final String profileThumbnailUrl;
  @Column("profile_image_url")
  private final String profileImageUrl;


  public User(UUID uid, String firstName, String lastName, String email,
      Set<UUID> contactIds, Set<UUID> threadIds, String profileThumbnailUrl, String profileImageUrl) {
    this.uid = uid;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.contactIds = Optional.ofNullable(contactIds).orElse(Collections.emptySet());
    this.threadIds = Optional.ofNullable(threadIds).orElse(Collections.emptySet());
    this.profileThumbnailUrl = profileThumbnailUrl;
    this.profileImageUrl = profileImageUrl;
  }
}
