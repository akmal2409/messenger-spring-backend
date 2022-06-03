package com.akmal.messengerspringbackend.repository;

import com.akmal.messengerspringbackend.model.MessageByUserByThread;
import com.akmal.messengerspringbackend.model.ThreadByUserByLastMessage;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.data.domain.Pageable;

/**
 * The repository is specific to the cassandra data model and
 * therefore, cannot serve as a general contract for an interface.
 *
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 28/05/2022 - 16:18
 * @project messenger-spring-backend
 * @since 1.0
 */
public interface MessageRepository {

  /**
   * Finds all messages by the given user id, thread id and a bucket, which form a partition key
   * that is used to locate the correct node with partition. The messages are sorted by message id
   * which is a snowflake.
   *
   * @param uid - user id (first part of the partition key).
   * @param threadId - identifies the chat (second part of the partition key).
   * @param bucket - a time bucket that is created from the custom epoch.
   * @param pageable - optional pagination properties.
   * @return
   */
  Collection<MessageByUserByThread> findAllByUidAndThreadIdAndBucket(UUID uid,
      UUID threadId, int bucket, Pageable pageable);

  /**
   * Saves the message in a partition for a given user, thread and a bucket.
   * It must perform a batch operation that will insert the message to all the users
   * that are part of the thread.
   *
   * @param message                   - valid message object.
   * @return saved message entity.
   */
  WriteResult saveMessageForAllThreadMembers(Collection<MessageByUserByThread> messages,
      Collection<ThreadByUserByLastMessage> latestThreads);
}
