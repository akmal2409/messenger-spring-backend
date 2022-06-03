package com.akmal.messengerspringbackend.repository;

import com.akmal.messengerspringbackend.model.MessageByUserByThread;
import com.akmal.messengerspringbackend.model.ThreadByUserByLastMessage;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.servererrors.UnavailableException;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.AsyncCassandraOperations;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.data.cassandra.core.cql.ExecutionProfileResolver;
import org.springframework.data.cassandra.core.cql.WriteOptions;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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
  public Collection<MessageByUserByThread> findAllByUidAndThreadIdAndBucket(
      UUID uid, UUID threadId, int bucket, Pageable pageable) {

    return  this.cassandraOperations.select(
        SimpleStatement.newInstance(
            "SELECT * FROM messages_by_user_thread WHERE uid = ? AND thread_id = ? AND bucket = ?",
            uid,
            threadId,
            bucket),
        MessageByUserByThread.class);
  }

  @Override
  public WriteResult saveMessageForAllThreadMembers(Collection<MessageByUserByThread> messages,
      Collection<ThreadByUserByLastMessage> latestThreads) {

    final var countDownLatch = new CountDownLatch(messages.size() + latestThreads.size());

    for (MessageByUserByThread message: messages) {
      final var messageFuture = this.asyncCassandraOperations
                                    .insert(message, InsertOptions.builder()
                                                         .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
                                                         .build());
      // have better error handling - on failure send to kafka retry topic
      messageFuture.addCallback(res -> countDownLatch.countDown(), error -> countDownLatch.countDown());
    }

    for (ThreadByUserByLastMessage thread: latestThreads) {
      final var threadFuture = this.asyncCassandraOperations
                                   .insert(thread, InsertOptions.builder()
                                                       .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
                                                       .build());
      threadFuture.addCallback(res -> countDownLatch.countDown(), error -> countDownLatch.countDown());
    }

    try {
      countDownLatch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

//    final CassandraBatchOperations batchOperations = this.cassandraOperations.batchOps();
//    batchOperations.withTimestamp(Instant.now().toEpochMilli());
//
//    for (MessageByUserByThread message: messages) {
//      batchOperations.insert(message, WriteOptions.builder()
//                                          .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
//                                          .build());
//    }
//
//    for (ThreadByUserByLastMessage thread: latestThreads) {
//      batchOperations.insert(thread, WriteOptions.builder()
//                                         .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
//                                         .build());
//    }
//
//    try {
//      final WriteResult result = batchOperations.execute();
//      System.out.println(result.wasApplied());
//      return result;
//    } catch (UnavailableException e) {
//      log.error("type=error message=\"Batch of messages failed. Not enough replicas\" consistency_level={} tracing_id={}",
//          e.getConsistencyLevel(), e.getExecutionInfo().getTracingId(), e);
//    }

    return null;
  }
}
