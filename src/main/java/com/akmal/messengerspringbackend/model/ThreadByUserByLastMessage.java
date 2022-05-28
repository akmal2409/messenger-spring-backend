package com.akmal.messengerspringbackend.model;

import com.akmal.messengerspringbackend.model.udt.UserUDT;
import com.datastax.oss.protocol.internal.ProtocolConstants.DataType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.With;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.CassandraType.Name;
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
  @Data
  @With
  @PrimaryKeyClass
  public static class Key {
    @PrimaryKeyColumn(value = "uid", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private final UUID uid;
    @PrimaryKeyColumn(value = "thread_id", type = PrimaryKeyType.CLUSTERED, ordinal = 1)
    private final UUID threadId;
  }

  @PrimaryKey
  private final Key key;
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
}
