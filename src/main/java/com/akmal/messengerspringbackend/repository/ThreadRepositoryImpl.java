package com.akmal.messengerspringbackend.repository;

import com.akmal.messengerspringbackend.model.Thread;
import com.akmal.messengerspringbackend.model.ThreadByUserByLastMessage;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Repository;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 02/06/2022 - 18:39
 * @project messenger-spring-backend
 * @since 1.0
 */
@Repository
@RequiredArgsConstructor
public class ThreadRepositoryImpl implements ThreadRepository {
  private final CassandraOperations cassandraOperations;

  @Override
  public Optional<Thread> findByThreadId(UUID threadId) {
    return Optional.ofNullable(
        this.cassandraOperations.selectOne(SimpleStatement.newInstance("SELECT * FROM threads WHERE thread_id = ?",
            threadId), Thread.class));
  }

  @Override
  public List<ThreadByUserByLastMessage> findThreadByLastMessageByUser(UUID uid) {
    return this.cassandraOperations
               .select(SimpleStatement
                       .newInstance("SELECT * FROM threads_by_user_by_last_message WHERE uid = ?",
                           uid),
                   ThreadByUserByLastMessage.class);
  }
}
