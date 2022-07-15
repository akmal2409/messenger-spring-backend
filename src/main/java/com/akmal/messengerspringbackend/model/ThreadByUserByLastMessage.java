package com.akmal.messengerspringbackend.model;

import com.akmal.messengerspringbackend.exception.CorruptedThreadException;
import com.akmal.messengerspringbackend.model.udt.UserUDT;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.With;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Frozen;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 28/05/2022 - 13:52
 * @project messenger-spring-backend
 * @since 1.0
 */
@With
@Builder
@Data
@Table("threads_by_user_by_last_message")
public class ThreadByUserByLastMessage {
  @PrimaryKey private final Key key;
  @Column("message_id")
  private final long messageId;
  @Column("time")
  private final LocalDateTime time;
  @Column("thread_name")
  private final String threadName;
  @Column("thread_picture_thumbnail_url")
  private final String threadPictureThumbnailUrl;
  @Column("message")
  private final String message;
  @Column("author")
  private final @Frozen UserUDT author;
  @Column("is_read")
  private final boolean read;
  @Column("is_system_message")
  private final boolean systemMessage;
  @Column("is_group_thread")
  private final boolean groupThread;
  @Column("member_ids")
  private final Set<String> memberIds;

  /**
   * The method decides which thread name and image to assign to the {@link
   * ThreadByUserByLastMessage} object. Because the data is denormalized we have to compute that
   * ahead of time. Resolving happens in the following manner:
   *
   * <ul>
   *   <li>If it is a group thread then return the name of the thread and its picture because it is
   *       uniform across all participants chats
   *   <li>If the user we are preparing it for is not the author then set the message author's name
   *       and picture
   *   <li>If none of those conditions are true, then it means we are inserting the message for the
   *       current user and since it is not a group thread, we know that there is exactly two
   *       participants, namely the author and on the inverse side the user with who we initiated
   *       the thread. Hence, we find the user UDT object that is not the author and insert return
   *       its details. In case the inverse side has not been found, then we have to remove the
   *       corrupted thread that contains only author and throw {@link CorruptedThreadException} so
   *       that the client can delete the thread from its local cache
   * </ul>
   *
   * @param thread - {@link Thread} model that contains list of participants and thread details
   * @param author - {@link User} model that represents the author of the message
   * @param toUser - {@link UserUDT} model, participant for who we are trying to construct the
   *     object.
   * @return {@link String[]} of size 2, where result[0] contains thread name and result[1] contains
   *     thread picture thumbnail.
   */
  public static String[] getThreadNameAndThumbnail(Thread thread, User author, UserUDT toUser) {
    if (thread.isGroupThread())
      return new String[] {thread.getThreadName(), thread.getThreadPictureThumbnailUrl()};
    else if (!author.getUid().equals(toUser.getUid())) {
      return new String[] {author.getFullName(), author.getProfileThumbnailUrl()};
    }

    // then its one - on - one chat
    final var inverseParticipant =
        thread.getMembers().stream().filter(p -> !author.getUid().equals(p.getUid())).findFirst();

    if (inverseParticipant.isEmpty()) {
      // TODO: corrupted chat fix, remove thread and throw exception
      throw new CorruptedThreadException("Thread is corrupted, not enough of participants");
    }

    return new String[] {
      inverseParticipant.get().getFullName(), inverseParticipant.get().getProfileThumbnailUrl()
    };
  }

  @Data
  @With
  @PrimaryKeyClass
  public static class Key {
    @PrimaryKeyColumn(value = "uid", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private final String uid;

    @PrimaryKeyColumn(value = "thread_id", type = PrimaryKeyType.CLUSTERED, ordinal = 1)
    private final UUID threadId;
  }
}
