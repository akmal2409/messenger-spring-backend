package com.akmal.messengerspringbackend.service;

import com.akmal.messengerspringbackend.dto.v1.MessageDTO;
import com.akmal.messengerspringbackend.dto.v1.MessageSendRequestDTO;
import com.akmal.messengerspringbackend.dto.v1.MessageSentResponseDTO;
import com.akmal.messengerspringbackend.dto.v1.MessageStatus;
import com.akmal.messengerspringbackend.dto.v1.ScrollContent;
import com.akmal.messengerspringbackend.exception.EntityNotFoundException;
import com.akmal.messengerspringbackend.model.MessageByUserByThread;
import com.akmal.messengerspringbackend.model.MessageByUserByThread.Key;
import com.akmal.messengerspringbackend.model.Thread;
import com.akmal.messengerspringbackend.model.ThreadByUserByLastMessage;
import com.akmal.messengerspringbackend.model.User;
import com.akmal.messengerspringbackend.model.udt.UserUDT;
import com.akmal.messengerspringbackend.repository.MessageRepository;
import com.akmal.messengerspringbackend.repository.ThreadRepository;
import com.akmal.messengerspringbackend.repository.UserRepository;
import com.akmal.messengerspringbackend.shared.BucketingManager;
import com.akmal.messengerspringbackend.snowflake.SnowflakeGenerator;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 28/05/2022 - 16:17
 * @project messenger-spring-backend
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
  private static final int FETCH_SIZE = 25;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ThreadRepository threadRepository;
  private final SnowflakeGenerator snowflakeGenerator;
  private final BucketingManager bucketingManager;

  /**
   * The method fetched the collection of messages with size <= {@link MessageService#FETCH_SIZE}.
   * The method resolves the data access path based on the following strategy:
   * <ul>
   *   <li>
   *    If beforeMessageId is provided and the bucket is valid, then we use the
   *    {@link MessageRepository#findAllBeforeMessageId(UUID, UUID, int, int, long)} contract
   *    to find all messages in a bucket with message id strictly smaller than the provided one.
   *   </li>
   *   <li>
   *     If only bucket is provided and optionally the pagingState (scroll state - a series of bytes,
   *     that helps the DSE driver to find the particular offset it stopped reading at before), then we
   *     use {@link MessageRepository#findAllByUidAndThreadIdAndBucket(UUID, UUID, int, int, String)}
   *     contract to find all messages limited by the {@link MessageService#FETCH_SIZE} in the database
   *     based on the user id, thread id and a bucket (+ optional pagingState).
   *   </li>
   *   <li>
   *     If none of the above-mentioned arguments were present it defaults to the bucket based on
   *     the current timestamp and uses the same contract as in the second resolution strategy (the one above).
   *   </li>
   * </ul>
   *
   * However, some buckets might not have enough of data to satisfy the size == {@link MessageService#FETCH_SIZE}
   * due to the small amount of data in the time bucket or simply paging state was applied that was just
   * at the end of the time bucket. Therefore, following resolution algorithm has been developed, see
   * {@link MessageService#aggregateStartingFromBucket(UUID, UUID, Integer, ScrollContent)} because
   * current method uses that resolution.
   *
   * On the other hand, there are also several conditions for the above-mentioned algorithm not
   * to start execution such as: size has been satisfied or the next bucket (currentBucket - 1) is out
   * of positive range (< 0) which simply indicates that there is no data.
   *
   * @param uid
   * @param threadId
   * @param bucket
   * @param beforeMessageId
   * @param pagingState
   * @return
   */
  @Contract(
      pure = true,
      value = "null,null,_,_,_ -> fail; null,_,_,_,_ -> fail; _,null,_,_,_ -> fail")
  public ScrollContent<MessageDTO> findAllByUserAndThreadAndBucket(
      @NotNull UUID uid,
      @NotNull UUID threadId,
      @Nullable Integer bucket,
      @Nullable Long beforeMessageId,
      @Nullable String pagingState) {
    Integer resolvedBucket = bucket;

    ScrollContent<MessageByUserByThread> messages;

    if (beforeMessageId != null && resolvedBucket != null) {
      messages = this.findAllBeforeMessageId(uid, threadId, resolvedBucket, beforeMessageId);
    } else if (resolvedBucket != null && resolvedBucket >= 0) {
      messages = this.findAllInBucket(uid, threadId, resolvedBucket, pagingState);
    } else {
      resolvedBucket = this.bucketingManager.makeBucket();
      messages = this.findAllInBucket(uid, threadId, resolvedBucket, null);
    }

    resolvedBucket--; // if we reached the FETCH_SIZE then technically bucket might contain
    // some data, however, this variable is used for further aggregation and if we did not manage
    // to accumulate enough of messages, then we have to look in the earlier buckets, however,
    // we must verify that the earlier bucket exists, if it doesn't then we have to return what we have

    if (messages.content().size() == FETCH_SIZE || resolvedBucket < 0) return this.mapScrollContentToDTO(messages);


    return this.mapScrollContentToDTO(
        this.aggregateStartingFromBucket(uid, threadId, resolvedBucket, messages));
  }

  private ScrollContent<MessageByUserByThread> findAllBeforeMessageId(
      @NotNull UUID uid,
      @NotNull UUID threadId,
      @NotNull Integer bucket,
      @NotNull Long fromMessageId) {

    return this.messageRepository.findAllBeforeMessageId(
        uid, threadId, bucket, FETCH_SIZE, fromMessageId);
  }

  private ScrollContent<MessageByUserByThread> findAllInBucket(
      @NotNull UUID uid,
      @NotNull UUID threadId,
      @NotNull Integer bucket,
      @Nullable String pagingState) {

    return this.messageRepository.findAllByUidAndThreadIdAndBucket(
        uid, threadId, bucket, FETCH_SIZE, pagingState);
  }

  /**
   * A custom algorithm that can scans iteratevely other buckets in case the initial content
   * size is smaller than {@link MessageService#FETCH_SIZE}.
   *
   * The algorithm works in the following way:
   * <ul>
   *   <li>
   *     Firstly, it extracts the timestamp from the thread id, which under the hood is
   *     a 128bit {@link Uuids#timeBased()} in Cassandra. Hence, using the {@link Uuids#unixTimestamp(UUID)}
   *     we can get the number of milliseconds from the epoch (1970 Jan). Thereafter, we
   *     have to adjust it with respect to the custom epoch, which is used to create time buckets
   *     and snowflakes. Once the timestamp is extracted, we have already bucket number that
   *     we haven't yet explored (in the method calling aggregation we deliberately decrement the
   *     bucket number and pass it here. Therefore, we can create a range of buckets
   *     from the thread creation time till the last unexplored bucket (inclusive).
   *   </li>
   *   <li>
   *     Thereafter, we can iteratively go through the buckets in the reverse way collecting
   *     the messages until we either hit a dead end or we have enough of data. We have to also record
   *     the pagination state for the last set of records that we have fetched and included because
   *     that will help us to resolve the next set of data.
   *   </li>
   * </ul>
   * @param uid
   * @param threadId
   * @param bucket
   * @param messages
   * @return
   */
  private ScrollContent<MessageByUserByThread> aggregateStartingFromBucket(
      @NotNull UUID uid,
      @NotNull UUID threadId,
      @NotNull Integer bucket,
      ScrollContent<MessageByUserByThread> messages) {
    final long threadCreatedTimestamp =
        this.bucketingManager.adjustTimestampToCustomEpoch(Uuids.unixTimestamp(threadId));

    final List<Integer> buckets =
        this.bucketingManager.makeBucketsFromTimestampTillBucket(
            threadCreatedTimestamp, Math.max(bucket - 1, 0));

    int i = buckets.size() - 1;
    int messagesToFetch = FETCH_SIZE - messages.content().size();
    String startPagingState = messages.pagingState();
    String lastPagingState = null;

    final List<MessageByUserByThread> aggregatedMessages = new LinkedList<>(messages.content());

    while (i >= 0 && aggregatedMessages.size() < FETCH_SIZE) {
      int currentBucket = buckets.get(i--);
      ScrollContent<MessageByUserByThread> scrollContent =
          this.messageRepository.findAllByUidAndThreadIdAndBucket(
              uid, threadId, currentBucket, messagesToFetch, startPagingState);

      aggregatedMessages.addAll(scrollContent.content());
      lastPagingState = scrollContent.pagingState();
      startPagingState = null; // if paging state was provided, apply it for the first bucket only
      messagesToFetch -= scrollContent.content().size();
    }

    return ScrollContent.of(aggregatedMessages, lastPagingState);
  }

  /**
   * The method composes {@link MessageByUserByThread} and {@link ThreadByUserByLastMessage} objects
   * and inserts them into cassandra database. MessageByUserByThread has to be inserted for each
   * participant of the conversation, while ThreadByUserByLastMessage represents the last active
   * threads and has to be inserted for each user separately. Due to the data being denormalized we
   * have to identify the thread name, see {@link
   * ThreadByUserByLastMessage#getThreadNameAndThumbnail(Thread, User, UserUDT)} for the
   * documentation.
   *
   * @param threadId - conversation id.
   * @param authorId - user who sent the message.
   * @param messageSendRequest - DTO object that contains threadId and body.
   * @return {@link MessageSentResponseDTO} that contains body and status of the message.
   */
  public MessageSentResponseDTO sendMessage(
      UUID threadId, UUID authorId, MessageSendRequestDTO messageSendRequest) {
    final var thread =
        this.threadRepository
            .findByThreadId(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread was not found"));
    final var currentUser =
        this.userRepository
            .findByUid(UUID.fromString("fef0d7a7-8af6-46d1-bbcd-94f6483d3645")) // temporary
            .orElseThrow(() -> new EntityNotFoundException("Current user was not found"));

    final Collection<MessageByUserByThread> messages = new LinkedList<>();
    final Collection<ThreadByUserByLastMessage> threads = new LinkedList<>();
    final long messageId = this.snowflakeGenerator.nextId();
    final int bucket = this.bucketingManager.makeBucket(messageId);

    for (UserUDT participant : thread.getMembers()) {
      final String[] threadNameAndThumbnail =
          ThreadByUserByLastMessage.getThreadNameAndThumbnail(thread, currentUser, participant);

      messages.add(
          MessageByUserByThread.builder()
              .authorId(currentUser.getUid())
              .body(messageSendRequest.body())
              .key(new Key(participant.getUid(), thread.getThreadId(), bucket, messageId))
              .build());

      threads.add(
          ThreadByUserByLastMessage.builder()
              .messageId(messageId)
              .author(currentUser.toUDT())
              .threadName(threadNameAndThumbnail[0])
              .threadPictureThumbnailUrl(threadNameAndThumbnail[1])
              .message(messageSendRequest.body())
              .key(new ThreadByUserByLastMessage.Key(participant.getUid(), thread.getThreadId()))
              .time(LocalDateTime.now())
              .build());
    }

    this.messageRepository.saveMessageForAllThreadMembers(messages, threads);

    return new MessageSentResponseDTO(
        MessageStatus.SENT, messageId, thread.getThreadId(), messageSendRequest.body());
  }

  private ScrollContent<MessageDTO> mapScrollContentToDTO(
      ScrollContent<MessageByUserByThread> scrollContent) {
    return ScrollContent.of(
        scrollContent.stream().map(MessageByUserByThread::toDTO).toList(),
        scrollContent.pagingState());
  }
}
