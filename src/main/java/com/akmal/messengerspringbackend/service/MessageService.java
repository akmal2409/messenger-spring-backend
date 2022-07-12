package com.akmal.messengerspringbackend.service;

import com.akmal.messengerspringbackend.dto.v1.MessageAcknowledgement;
import com.akmal.messengerspringbackend.dto.v1.MessageDTO;
import com.akmal.messengerspringbackend.dto.v1.MessageSendRequestDTO;
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
import com.akmal.messengerspringbackend.service.MessageDeliveryService.FanoutMessageMetadata;
import com.akmal.messengerspringbackend.shared.BucketingManager;
import com.akmal.messengerspringbackend.shared.util.ImmutableLists;
import com.akmal.messengerspringbackend.snowflake.SnowflakeGenerator;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
  private final SimpMessagingTemplate wsMessagingTemplate;
  private final ThreadRepository threadRepository;
  private final SnowflakeGenerator snowflakeGenerator;
  private final BucketingManager bucketingManager;
  private final MessageDeliveryService messageDeliveryService;

  @Qualifier("asyncExecutor")
  @Autowired
  private TaskExecutor asyncTaskExecutor;

  private final UserService userService;

  /**
   * The method fetched the collection of messages with size <= {@link MessageService#FETCH_SIZE}.
   * The method resolves the data access path based on the following strategy:
   *
   * <ul>
   *   <li>If beforeMessageId is provided and the bucket is valid, then we use the {@link
   *       MessageRepository#findAllBeforeMessageId(String, UUID, int, int, long)} contract to find
   *       all messages in a bucket with message id strictly smaller than the provided one.
   *   <li>If only bucket is provided and optionally the pagingState (scroll state - a series of
   *       bytes, that helps the DSE driver to find the particular offset it stopped reading at
   *       before), then we use {@link MessageRepository#findAllByUidAndThreadIdAndBucket(String,
   *       UUID, int, int, String)} contract to find all messages limited by the {@link
   *       MessageService#FETCH_SIZE} in the database based on the user id, thread id and a bucket
   *       (+ optional pagingState).
   *   <li>If none of the above-mentioned arguments were present it defaults to the bucket based on
   *       the current timestamp and uses the same contract as in the second resolution strategy
   *       (the one above).
   * </ul>
   *
   * However, some buckets might not have enough of data to satisfy the size == {@link
   * MessageService#FETCH_SIZE} due to the small amount of data in the time bucket or simply paging
   * state was applied that was just at the end of the time bucket. Therefore, following resolution
   * algorithm has been developed, see {@link MessageService#aggregateStartingFromBucket(String,
   * UUID, Integer, ScrollContent)} because current method uses that resolution.
   *
   * <p>On the other hand, there are also several conditions for the above-mentioned algorithm not
   * to start execution such as: size has been satisfied or the next bucket (currentBucket - 1) is
   * out of positive range (< 0) which simply indicates that there is no data.
   *
   * @param uid user id for whom we are retrieving messages
   * @param threadId id of a thread for which we are retrieving messages
   * @param bucket time bucket of the message
   * @param beforeMessageId optional parameter to find all messages published before certain message
   * @param pagingState optional parameter to continue fetching the next set of records (reverse virtual scrolling)
   * @return a page of messages sorted from the newest to the oldest restricted by the FETCH_SIZE property
   * in {@link MessageRepository}
   */
  @Contract(
      pure = true,
      value = "null,null,_,_,_ -> fail; null,_,_,_,_ -> fail; _,null,_,_,_ -> fail")
  public ScrollContent<MessageDTO> findAllByUserAndThreadAndBucket(
      @NotNull String uid,
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
    // we must verify that the earlier bucket exists, if it doesn't then we have to return what we
    // have

    if (messages.content().size() == FETCH_SIZE || resolvedBucket < 0)
      return this.mapScrollContentToDTO(messages);

    return this.mapScrollContentToDTO(
        this.aggregateStartingFromBucket(uid, threadId, resolvedBucket, messages));
  }

  private ScrollContent<MessageByUserByThread> findAllBeforeMessageId(
      @NotNull String uid,
      @NotNull UUID threadId,
      @NotNull Integer bucket,
      @NotNull Long fromMessageId) {

    return this.messageRepository.findAllBeforeMessageId(
        uid, threadId, bucket, FETCH_SIZE, fromMessageId);
  }

  private ScrollContent<MessageByUserByThread> findAllInBucket(
      @NotNull String uid,
      @NotNull UUID threadId,
      @NotNull Integer bucket,
      @Nullable String pagingState) {

    return this.messageRepository.findAllByUidAndThreadIdAndBucket(
        uid, threadId, bucket, FETCH_SIZE, pagingState);
  }

  /**
   * A custom algorithm that can scans iteratevely other buckets in case the initial content size is
   * smaller than {@link MessageService#FETCH_SIZE}.
   *
   * <p>The algorithm works in the following way:
   *
   * <ul>
   *   <li>Firstly, it extracts the timestamp from the thread id, which under the hood is a 128bit
   *       {@link Uuids#timeBased()} in Cassandra. Hence, using the {@link
   *       Uuids#unixTimestamp(UUID)} we can get the number of milliseconds from the epoch (1970
   *       Jan). Thereafter, we have to adjust it with respect to the custom epoch, which is used to
   *       create time buckets and snowflakes. Once the timestamp is extracted, we have already
   *       bucket number that we haven't yet explored (in the method calling aggregation we
   *       deliberately decrement the bucket number and pass it here. Therefore, we can create a
   *       range of buckets from the thread creation time till the last unexplored bucket
   *       (inclusive).
   *   <li>Thereafter, we can iteratively go through the buckets in the reverse way collecting the
   *       messages until we either hit a dead end or we have enough of data. We have to also record
   *       the pagination state for the last set of records that we have fetched and included
   *       because that will help us to resolve the next set of data.
   * </ul>
   *
   * @param uid
   * @param threadId
   * @param bucket
   * @param messages
   * @return
   */
  private ScrollContent<MessageByUserByThread> aggregateStartingFromBucket(
      @NotNull String uid,
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
    String lastPagingState = null;

    final List<MessageByUserByThread> aggregatedMessages = new LinkedList<>(messages.content());

    while (i >= 0 && aggregatedMessages.size() < FETCH_SIZE) {
      int currentBucket = buckets.get(i--);
      ScrollContent<MessageByUserByThread> scrollContent =
          this.messageRepository.findAllByUidAndThreadIdAndBucket(
              uid, threadId, currentBucket, messagesToFetch, null);

      aggregatedMessages.addAll(scrollContent.content());
      lastPagingState = scrollContent.pagingState();
      messagesToFetch -= scrollContent.content().size();
    }

    return ScrollContent.of(aggregatedMessages, lastPagingState);
  }

  public ScrollContent<MessageDTO> findAllByUserAndThreadAndBucketMarkAsRead(
      @NotNull String uid,
      @NotNull UUID threadId,
      @Nullable Integer bucket,
      @Nullable Long beforeMessageId,
      @Nullable String pagingState) {
    var scrollContent =
        this.findAllByUserAndThreadAndBucket(uid, threadId, bucket, beforeMessageId, pagingState);

    if (!StringUtils.hasText(pagingState) && !scrollContent.content().isEmpty()) {
      // means we are loading the first page of the results and hence need to check
      // and mark last read message

      // we have to acknowledge the latest seen message, which itself acknowledges all the previous
      // ones.
      final var messageToMark = scrollContent.content().get(0); // it is sorted, latest first

      if (!messageToMark.read()) {
        scrollContent = scrollContent.withContent(
            ImmutableLists.appendAtIndex(scrollContent.content(), 0, messageToMark.withRead(true)));
        this.asyncTaskExecutor.execute(
            () ->
                markMessageAsRead(
                    uid, threadId, messageToMark.bucket(), messageToMark.messageId()));
      }
    }

    return scrollContent;
  }
  @Async
  public void markMessageAsRead(String uid, UUID threadId, int bucket, long messageId) {
    this.threadRepository.updateIsReadThreadByUserByMessage(threadId, uid, true);
    this.messageRepository.updateIsRead(uid, threadId, bucket, messageId, true);
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
   * @return {@link MessageDTO} that contains body and status of the message.
   */
  public MessageDTO sendMessage(
      UUID threadId, String authorId, MessageSendRequestDTO messageSendRequest) {
    final var thread =
        this.threadRepository
            .findByThreadId(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread was not found"));

    final var author = this.userService.findUserByUid(authorId);

    final Collection<MessageByUserByThread> messages = new LinkedList<>();
    final Collection<ThreadByUserByLastMessage> threads = new LinkedList<>();
    final Set<String> excludedFromDelivery = new HashSet<>(Collections.singletonList(authorId));
    final Collection<FanoutMessageMetadata> fanoutMetadata = new LinkedList<>();

    final long messageId = this.snowflakeGenerator.nextId();
    final int bucket = this.bucketingManager.makeBucket(messageId);

    for (UserUDT participant : thread.getMembers()) {
      final String[] threadNameAndThumbnail =
          ThreadByUserByLastMessage.getThreadNameAndThumbnail(thread, author, participant);

      messages.add(
          MessageByUserByThread.builder()
              .authorId(author.getUid())
              .body(messageSendRequest.body())
              .read(authorId.equals(participant.getUid()))
              .key(new Key(participant.getUid(), thread.getThreadId(), bucket, messageId))
              .build());

      threads.add(
          ThreadByUserByLastMessage.builder()
              .messageId(messageId)
              .author(author.toUDT())
              .threadName(threadNameAndThumbnail[0])
              .threadPictureThumbnailUrl(threadNameAndThumbnail[1])
              .message(messageSendRequest.body())
              .read(
                  authorId.equals(
                      participant
                          .getUid())) // if we are persisting for the author of the message, it
                                      // means the message has been read
              .key(new ThreadByUserByLastMessage.Key(participant.getUid(), thread.getThreadId()))
              .time(LocalDateTime.now())
              .build());

      if (!excludedFromDelivery.contains(participant.getUid())) {
        fanoutMetadata.add(
            FanoutMessageMetadata.builder()
                .authorName(author.getFullName())
                .authorId(authorId)
                .body(messageSendRequest.body())
                .recipientId(participant.getUid())
                .bucket(bucket)
                .messageId(messageId)
                .threadId(threadId)
                .threadName(threadNameAndThumbnail[0])
                .build());
      }
    }

    //noinspection ResultOfMethodCallIgnored
    this.messageRepository.saveMessageForAllThreadMembers(messages, threads);
    this.messageDeliveryService.fanoutMessages(fanoutMetadata); // async execution

    return new MessageDTO(
        messageId, threadId.toString(), bucket, authorId, messageSendRequest.body(),
        LocalDateTime.now(), true, false, false);
  }

  public void acknowledgeMessage(String receiptId, String userId, MessageDTO messageDTO) {
    final var destination = String.format("/queue/threads/%s/acks", messageDTO.threadId());

    this.wsMessagingTemplate.convertAndSendToUser(
        userId, destination, new MessageAcknowledgement(messageDTO, receiptId, true)
    );
  }

  private ScrollContent<MessageDTO> mapScrollContentToDTO(
      ScrollContent<MessageByUserByThread> scrollContent) {

    return ScrollContent.of(
        scrollContent.stream().map(m -> {
          Instant instant = this.snowflakeGenerator.toInstant(m.getKey().getMessageId());

          return m.toDTO(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
        }).toList(),
        scrollContent.pagingState());
  }
}
