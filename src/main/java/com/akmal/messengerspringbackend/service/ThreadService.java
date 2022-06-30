package com.akmal.messengerspringbackend.service;

import com.akmal.messengerspringbackend.dto.v1.LatestThreadDTO;
import com.akmal.messengerspringbackend.dto.v1.ThreadCreationRequest;
import com.akmal.messengerspringbackend.dto.v1.ThreadDTO;
import com.akmal.messengerspringbackend.dto.v1.UserDetailsDTO;
import com.akmal.messengerspringbackend.exception.IllegalThreadCreationRequest;
import com.akmal.messengerspringbackend.exception.UnauthorizedActionException;
import com.akmal.messengerspringbackend.model.Thread;
import com.akmal.messengerspringbackend.model.ThreadByUserByLastMessage;
import com.akmal.messengerspringbackend.model.ThreadByUserByLastMessage.Key;
import com.akmal.messengerspringbackend.model.User;
import com.akmal.messengerspringbackend.model.udt.UserUDT;
import com.akmal.messengerspringbackend.repository.ThreadRepository;
import com.akmal.messengerspringbackend.repository.UserRepository;
import com.akmal.messengerspringbackend.shared.timeago.TimeAgoConverter;
import com.akmal.messengerspringbackend.snowflake.SnowflakeGenerator;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import io.micrometer.core.instrument.util.StringUtils;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;


/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 26/06/2022 - 16:44
 * @project messenger-spring-backend
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThreadService {
  private final ThreadRepository threadRepository;
  private final TimeAgoConverter timeAgoConverter;
  private final SnowflakeGenerator snowflakeGenerator;
  private final UserRepository userRepository;
  private final UserService userService;

  /**
   * The method returns threads sorted based on the message id, which is a
   * strictly increasing unique identifier that is based on the timestamp as well as
   * the node id and the seq number.
   *
   * @param uid user id for whom to fetch the latest threads.
   * @return list of the latest threads for a given user sorted by messageId (i.e. timestamp).
   */
  @Contract(pure = true)
  public List<LatestThreadDTO> findAllLatestByUser(@NotNull String uid) {
    return this.threadRepository
               .findThreadByLastMessageByUser(uid)
               .stream()
               .map(this::mapToLatestThread)
               .sorted(Comparator.comparingLong(LatestThreadDTO::lastMessageId).reversed())
               .toList();
  }

  /**
   * A utility function that detrmines whether the user is listed as a member in a thread.
   * It fetched the thread by id and compares the members list.
   *
   * @param uid user id to check.
   * @param threadId thread id to look in.
   * @return true if the user is a member of a thread.
   */
  public boolean isUserThreadParticipant(@NotNull String uid, @NotNull String threadId) {
    return this.threadRepository.findByThreadId(UUID.fromString(threadId))
               .map(thread ->
                        thread.getMembers().stream().anyMatch(member -> member.getUid().equals(uid)))
               .orElse(false);

  }

  public ThreadDTO findById(@NotNull UUID threadId) {
    return this.threadRepository
               .findByThreadId(threadId)
               .map(ThreadDTO::from)
               .orElse(null);
  }

  /**
   * Creates a new thread in the primary database and fans out the creation message upon successful
   * set of operations.
   * Firstly, it validates the thread creation request, the participant list must not be empty
   * and if the thread is declared as a group thread, it must be specified so ahead of time. However,
   * if the thread is specified as a group thread but the count of participants is smaller than 2,
   * then the exception is thrown.
   *
   * After validation, it fetches the list of users provided by the client and validates whether
   * the thread author is in their contact list. If not the exception is thrown.
   *
   * If the thread is a group thread and the thread name is not specified, the exception is thrown.
   *
   * Otherwise, the strategy for determining the thread name tailored for each specific user is described
   * in details in {@link ThreadByUserByLastMessage#getThreadNameAndThumbnail(Thread, User, UserUDT)}.
   *
   * Lastly, if that succeeds, we fan out the system message to all the participants about the thread
   * creation, threby making the thread appear in the latest thread list.
   * @param userId id of the thread creator
   * @param threadCreationRequest a DTO object containing meta information about the future thread.
   * @return created {@link Thread} with a generated ID and a thread name relative to the side of the chat.
   * For example if the user A created a one-on-one chat with the user B, the user A will receive a new
   * thread with a full name of the user B.
   */
  public ThreadDTO createThread(@NotNull String userId, @NotNull ThreadCreationRequest threadCreationRequest) {
    if (threadCreationRequest.inviteeIds().isEmpty()) {
      throw new IllegalThreadCreationRequest("At least one invitee must be provided.");
    } else if (threadCreationRequest.groupThread() && threadCreationRequest.inviteeIds().size() > 1) {
      throw new IllegalThreadCreationRequest("More than 1 invitees were provided while the thread "
                                                 + "was specified to be a one on one thread.");
    }

    final var users = this.userRepository.findAllByIds(threadCreationRequest.inviteeIds());
    final var author = this.userService.findUserByUid(userId);

    if (users.isEmpty()) {
      throw new IllegalThreadCreationRequest("Thread must have more than 1 participant");
    } else if (!this.isUserInContactListOf(users, userId)) {
      throw new UnauthorizedActionException("You must be in contact list of every invited participant");
    }

    String threadName = null;

    if (threadCreationRequest.groupThread()) {
      if (StringUtils.isEmpty(threadCreationRequest.threadName())) {
        throw new IllegalThreadCreationRequest("For a group thread you must supply the thread name.");
      }
      threadName = threadCreationRequest.threadName();
    }

    final var members = Stream.concat(users.stream(), Stream.of(author))
                            .map(User::toUDT)
                            .collect(Collectors.toSet());

    final var newThread = Thread.builder()
                              .groupThread(threadCreationRequest.groupThread())
                              .threadName(threadName)
                              .threadId(Uuids.timeBased())
                              .threadPictureThumbnailUrl(null)
                              .threadPictureUrl(null)
                              .members(members)
                              .build();
    this.threadRepository.save(newThread);

    this.fanoutThreadCreationMessage(newThread, author);

    final String[] threadAndThumbnail = ThreadByUserByLastMessage.getThreadNameAndThumbnail(newThread,
        author, author.toUDT());

    log.info("");

    return ThreadDTO.from(newThread)
               .withThreadName(threadAndThumbnail[0])
               .withThreadPictureThumbnailUrl(threadAndThumbnail[1]);
  }

  private void fanoutThreadCreationMessage(Thread thread, User author) {
    final var threads = new LinkedList<ThreadByUserByLastMessage>();
    final var messageId = this.snowflakeGenerator.nextId();

    for (UserUDT member: thread.getMembers()) {
      final var threadNameAndThumbnail = ThreadByUserByLastMessage.getThreadNameAndThumbnail(thread, author, member);
      final var welcomeMessage = author.getUid().equals(member.getUid()) ?
                                     "You have started the conversation"
                                     : String.format("%s has started the conversation", author.getFullName());
      final var threadByUserByLastMessage = ThreadByUserByLastMessage.builder()
                                                .message(welcomeMessage)
                                                .systemMessage(true)
                                                .messageId(messageId)
                                                .threadName(threadNameAndThumbnail[0])
                                                .threadPictureThumbnailUrl(threadNameAndThumbnail[1])
                                                .key(new Key(member.getUid(), thread.getThreadId()))
                                                .build();
      threads.add(threadByUserByLastMessage);
    }

    this.threadRepository.saveAllThreadByUserByLastMessage(threads);
  }

  private boolean isUserInContactListOf(@NotNull Collection<User> contacts, @NotNull String userId) {
    return contacts.stream()
               .allMatch(contact -> contact.getContacts().stream().anyMatch(u -> u.getUid().equals(userId)));
  }

  @Contract(pure = true)
  private LatestThreadDTO mapToLatestThread(@NotNull ThreadByUserByLastMessage thread) {
    final var lastMessageTimestamp = this.snowflakeGenerator.toInstant(thread.getMessageId());

    return new LatestThreadDTO(
        thread.getKey().getThreadId().toString(),
        thread.getKey().getUid(),
        thread.getMessageId(),
        this.timeAgoConverter.convert(lastMessageTimestamp),
        thread.getThreadName(),
        thread.getThreadPictureThumbnailUrl(),
        thread.getMessage(),
        Optional.ofNullable(thread.getAuthor()).map(UserDetailsDTO::from).orElse(null),
        thread.isRead(),
        thread.isSystemMessage());
  }
}
