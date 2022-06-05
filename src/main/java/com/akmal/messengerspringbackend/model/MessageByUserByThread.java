package com.akmal.messengerspringbackend.model;

import com.akmal.messengerspringbackend.dto.v1.MessageDTO;
import java.io.Serializable;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.With;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 23/05/2022 - 21:08
 * @project messenger-spring-backend
 * @since 1.0
 */
@Table("messages_by_user_by_thread")
@With
@Builder
@Data
public class MessageByUserByThread {

  @With
  @Data
  @PrimaryKeyClass
  public static class Key implements Serializable {
    @PrimaryKeyColumn(value = "uid", type = PrimaryKeyType.PARTITIONED)
    private final UUID uid;
    @PrimaryKeyColumn(value = "thread_id", type = PrimaryKeyType.PARTITIONED)
    private final UUID threadId;
    @PrimaryKeyColumn(value = "bucket", type = PrimaryKeyType.PARTITIONED)
    private final int bucket;
    @PrimaryKeyColumn(value = "message_id", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private final long messageId;
  }

  @PrimaryKey
  private final Key key;
  @Column("author_id")
  private final UUID authorId;
  @Column("body")
  private final String body;
  @Column("is_read")
  private final boolean read;
  @Column("is_edited")
  private final boolean edited;
  @Column("is_system_message")
  private final boolean systemMessage;

  public MessageDTO toDTO() {
    return new MessageDTO(
        this.key.messageId,
        this.key.threadId.toString(),
        this.key.bucket,
        this.authorId.toString(),
        this.body,
        this.read,
        this.edited,
        this.isSystemMessage()
    );
  }
}
