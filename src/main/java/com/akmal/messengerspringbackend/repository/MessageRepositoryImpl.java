package com.akmal.messengerspringbackend.repository;

import com.akmal.messengerspringbackend.dto.v1.ScrollContent;
import com.akmal.messengerspringbackend.model.MessageByUserByThread;
import com.akmal.messengerspringbackend.model.ThreadByUserByLastMessage;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.PagingState;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.cassandra.core.AsyncCassandraOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 28/05/2022 - 16:18
 * @project messenger-spring-backend
 * @since 1.0
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class MessageRepositoryImpl implements MessageRepository {
  private final CassandraOperations cassandraOperations;
  private final AsyncCassandraOperations asyncCassandraOperations;

  @Override
  public ScrollContent<MessageByUserByThread> findAllByUidAndThreadIdAndBucket(
      @NotNull String uid,
      @NotNull UUID threadId,
      int bucket,
      int size,
      @Nullable String pagingState) {
    var statement =
        SimpleStatement.newInstance(
                "SELECT * FROM messages_by_user_by_thread WHERE uid = ? AND thread_id = ? AND bucket = ?",
                uid,
                threadId,
                bucket)
            .setPageSize(size)
            .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
            .setExecutionProfileName("custom-retries");

    if (StringUtils.hasText(pagingState)) {
      ByteBuffer parsedPaginState = null;

      try {
        parsedPaginState = PagingState.fromString(pagingState).getRawPagingState();
      } catch (IllegalArgumentException e) {
        log.error("type=exception; reason=Paging state parsing failed; value={}", pagingState, e);
      }

      statement = statement.setPagingState(parsedPaginState);
    }

    final var resultSet = this.cassandraOperations.execute(statement);
    return this.fetchCurrentPage(resultSet, MessageByUserByThread.class);
  }

  @Override
  public ScrollContent<MessageByUserByThread> findAllBeforeMessageId(
      @NotNull String uid, @NotNull UUID threadId, int bucket, int size, long messageId) {
    final var statement =
        SimpleStatement.newInstance(
                "SELECT * FROM messages_by_user_by_thread WHERE uid = ? AND thread_id = ? AND bucket = ? "
                    + "AND message_id < ?",
                uid,
                threadId,
                bucket,
                messageId)
            .setPageSize(size)
            .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
            .setExecutionProfileName("custom-retries");

    final var resultSet = this.cassandraOperations.execute(statement);

    return this.fetchCurrentPage(resultSet, MessageByUserByThread.class);
  }

  private <T> ScrollContent<T> fetchCurrentPage(ResultSet resultSet, Class<T> clazz) {
    final var content = new LinkedList<T>();
    final var pagingState =
        Optional.ofNullable(resultSet.getExecutionInfo().getSafePagingState())
            .map(PagingState::toString)
            .orElse(null);

    while (resultSet.getAvailableWithoutFetching() > 0) {
      final var row = resultSet.one();

      content.add(this.cassandraOperations.getConverter().read(clazz, row));
    }

    return ScrollContent.of(content, pagingState);
  }

  @Override
  public WriteResult saveMessageForAllThreadMembers(
      @NotNull Collection<MessageByUserByThread> messages,
      @NotNull Collection<ThreadByUserByLastMessage> latestThreads) {

    final var countDownLatch = new CountDownLatch(messages.size() + latestThreads.size());

    for (MessageByUserByThread message : messages) {
      final var messageFuture =
          this.asyncCassandraOperations.insert(
              message,
              InsertOptions.builder()
                  .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
                  .executionProfile("custom-retries")
                  .build());
      // have better error handling - on failure send to kafka retry topic
      messageFuture.addCallback(
          res -> countDownLatch.countDown(), error -> countDownLatch.countDown());
    }

    for (ThreadByUserByLastMessage thread : latestThreads) {
      final var threadFuture =
          this.asyncCassandraOperations.insert(
              thread,
              InsertOptions.builder()
                  .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
                  .executionProfile("custom-retries")
                  .build());
      threadFuture.addCallback(
          res -> countDownLatch.countDown(), error -> countDownLatch.countDown());
    }

    try {
      countDownLatch.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    return null;
  }
}
