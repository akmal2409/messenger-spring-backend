package com.akmal.messengerspringbackend.model;

import java.util.UUID;
import lombok.Data;
import lombok.With;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
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
@Data
@Table("threads_by_user_by_last_message")
public class ThreadByUserByLastMessage {
  @Data
  @With
  @PrimaryKeyClass
  static class Key {
    @PrimaryKeyColumn(value = "uid", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private final UUID uid;
    @PrimaryKeyColumn(value = "thread_id", type = PrimaryKeyType.CLUSTERED, ordinal = 1)
    private final UUID threadId;
  }

  @PrimaryKey
  private final Key key;

}
