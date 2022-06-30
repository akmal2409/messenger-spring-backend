package com.akmal.messengerspringbackend.repository;

import com.akmal.messengerspringbackend.exception.persistence.DataAccessException;
import com.akmal.messengerspringbackend.exception.persistence.DataWriteTimeoutException;
import com.akmal.messengerspringbackend.model.Thread;
import com.akmal.messengerspringbackend.model.ThreadByUserByLastMessage;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.cassandra.core.AsyncCassandraOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.EntityWriteResult;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.stereotype.Repository;
import org.springframework.util.concurrent.ListenableFuture;

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
  private final AsyncCassandraOperations asyncCassandraOperations;

  @Override
  public Optional<Thread> findByThreadId(UUID threadId) {
    return Optional.ofNullable(
        this.cassandraOperations.selectOne(
            SimpleStatement.newInstance("SELECT * FROM threads WHERE thread_id = ?", threadId),
            Thread.class));
  }

  @Override
  public List<ThreadByUserByLastMessage> findThreadByLastMessageByUser(String uid) {
    return this.cassandraOperations.select(
        SimpleStatement.newInstance(
            "SELECT * FROM threads_by_user_by_last_message WHERE uid = ?", uid),
        ThreadByUserByLastMessage.class);
  }

  @Override
  public Thread save(Thread thread) {
    return this.cassandraOperations.insert(thread);
  }

  @Override
  public List<ThreadByUserByLastMessage> saveAllThreadByUserByLastMessage(
      @NotNull List<ThreadByUserByLastMessage> threads) {
    final var countDownLatch = new CountDownLatch(threads.size());
    final var lastException = new AtomicReference<Throwable>();
    final var futures =
        new LinkedList<ListenableFuture<EntityWriteResult<ThreadByUserByLastMessage>>>();

    for (ThreadByUserByLastMessage thread : threads) {
      final var future =
          this.asyncCassandraOperations.insert(
              thread,
              InsertOptions.builder()
                  .executionProfile("custom-retries")
                  .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
                  .build());
      futures.add(future);

      future.addCallback(
          t -> countDownLatch.countDown(),
          e -> {
            lastException.set(e);
            countDownLatch.countDown();
          });
    }

    try {
      countDownLatch.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new DataWriteTimeoutException("The threads could not be saved due to an timeout", e);
    }

    if (lastException.get() != null) {
      throw new DataAccessException(
          "The threads could not be saved due to the persistence exception", lastException.get());
    }

    return futures.stream()
        .map(future -> future.completable().join())
        .map(EntityWriteResult::getEntity)
        .toList();
  }
}
