package com.akmal.messengerspringbackend.repository;

import com.akmal.messengerspringbackend.model.User;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.stereotype.Repository;

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
}
