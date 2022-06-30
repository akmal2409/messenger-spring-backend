package com.akmal.messengerspringbackend.repository;

import com.akmal.messengerspringbackend.exception.persistence.DataAccessException;
import com.akmal.messengerspringbackend.exception.persistence.DataReadTimeoutException;
import com.akmal.messengerspringbackend.model.User;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.cassandra.core.AsyncCassandraOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.stereotype.Repository;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 01/06/2022 - 20:54
 * @project messenger-spring-backend
 * @since 1.0
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
  private final CassandraOperations cassandraOperations;
  private final AsyncCassandraOperations asyncCassandraOperations;

  @Override
  public Optional<User> findByUid(String uid) {
    final var user =
        this.cassandraOperations.selectOne(
            SimpleStatement.newInstance("SELECT * FROM users WHERE uid = ?", uid), User.class);

    return Optional.ofNullable(user);
  }

  @Override
  public User save(User user) {
    return this.cassandraOperations
        .insert(
            user,
            InsertOptions.builder()
                .executionProfile("custom-retries")
                .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
                .build())
        .getEntity();
  }

  @Override
  public List<User> findAllByIds(@NotNull Collection<String> ids) {
    final var userFutures = new LinkedList<ListenableFuture<User>>();
    final var countDownLatch = new CountDownLatch(ids.size());
    final var exceptionAtomicReference = new AtomicReference<Throwable>();

    for (final String id: ids) {
      final var future = this.asyncCassandraOperations
                             .selectOne(SimpleStatement.newInstance("SELECT * FROM users WHERE uid = ?", id),
                                 User.class);
      future.addCallback(u -> countDownLatch.countDown(), e -> {
        exceptionAtomicReference.compareAndSet(null, e);
        countDownLatch.countDown();
      });
      userFutures.add(future);
    }

    try {
      countDownLatch.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new DataReadTimeoutException("Could not retrieve list of users due to read timeout.");
    }

    if (exceptionAtomicReference.get() != null) {
      throw new DataAccessException("There was an error while retrieving the list of users", exceptionAtomicReference.get());
    }

    return userFutures.stream()
        .map(future -> future.completable().join())
        .toList();
  }
}
