package com.akmal.messengerspringbackend.model;

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
 * @created 23/05/2022 - 20:57
 * @project messenger-spring-backend
 * @since 1.0
 */
@Table("threads")
@With
@Builder
@Data
public class Thread {
  @PrimaryKey("thread_id")
  private final UUID threadId;
  @Column("member_ids")
  private final Set<UUID> memberIds;
  @Column("thread_name")
  private final String threadName;
  @Column("thread_picture_thumbnail_url")
  private final String threadPictureThumbnailUrl;
  @Column("thread_picture_url")
  private final String threadPictureUrl;
  
  @Column("is_group_thread")
  private final boolean groupThread;

  public Thread(UUID threadId, Set<UUID> memberIds, String threadName,
      String threadPictureThumbnailUrl, String threadPictureUrl, boolean groupThread) {
    this.threadId = threadId;
    this.memberIds = Optional.ofNullable(memberIds).orElse(Set.of());
    this.threadName = threadName;
    this.threadPictureThumbnailUrl = threadPictureThumbnailUrl;
    this.threadPictureUrl = threadPictureUrl;
    this.groupThread = groupThread;
  }
}
