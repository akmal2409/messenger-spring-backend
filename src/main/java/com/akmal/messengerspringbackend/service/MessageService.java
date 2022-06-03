package com.akmal.messengerspringbackend.service;

import com.akmal.messengerspringbackend.dto.v1.MessageSendRequestDTO;
import com.akmal.messengerspringbackend.dto.v1.MessageSentResponseDTO;
import com.akmal.messengerspringbackend.dto.v1.MessageStatus;
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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ThreadRepository threadRepository;
  private final SnowflakeGenerator snowflakeGenerator;
  private final BucketingManager bucketingManager;

  /**
   * The method composes {@link MessageByUserByThread} and {@link ThreadByUserByLastMessage} objects
   * and inserts them into cassandra database. MessageByUserByThread has to be inserted for each
   * participant of the conversation, while ThreadByUserByLastMessage represents the last active threads
   * and has to be inserted for each user separately. Due to the data being denormalized we
   * have to identify the thread name, see
   * {@link ThreadByUserByLastMessage#getThreadNameAndThumbnail(Thread, User, UserUDT)} for the documentation.
   *
   * @param threadId - conversation id.
   * @param authorId - user who sent the message.
   * @param messageSendRequest - DTO object that contains threadId and body.
   * @return {@link MessageSentResponseDTO} that contains body and status of the message.
   */
  public MessageSentResponseDTO sendMessage(
      UUID threadId,
      UUID authorId,
      MessageSendRequestDTO messageSendRequest) {
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


}
