package com.akmal.messengerspringbackend.service;

import com.akmal.messengerspringbackend.config.kafka.KafkaConfigurationProperties;
import com.akmal.messengerspringbackend.exception.EntityNotFoundException;
import com.akmal.messengerspringbackend.model.udt.UserUDT;
import com.akmal.messengerspringbackend.repository.ThreadRepository;
import com.akmal.messengerspringbackend.repository.UserRepository;
import com.akmal.messengerspringbackend.thread.PresenceEventType;
import com.akmal.messengerspringbackend.thread.ThreadEventKey;
import com.akmal.messengerspringbackend.thread.ThreadPresenceEvent;
import com.akmal.messengerspringbackend.user.UserPresenceEvent;
import com.akmal.messengerspringbackend.websocket.storage.WebsocketSessionStorage;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 13/07/2022 - 19:20
 * @project messenger-spring-backend
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class UserPresenceService {
  @Qualifier("kafkaTemplateAvroKeyAvroValue") private final KafkaTemplate<SpecificRecord, SpecificRecord> kafkaAvroKeyAvroValueTemplate;
  @Qualifier("kafkaTemplateStringKeyAvroValue") private final KafkaTemplate<String, SpecificRecord> kafkaStringKeyAvroValueTemplate;

  private final KafkaConfigurationProperties kafkaProps;
  private final ThreadRepository threadRepository;
  private final WebsocketSessionStorage websocketSessionStorage;
  private final UserService userService;
  private final UserRepository userRepository;

  /**
   * Fans out the typing presence event to all members of the thread excluding the current user.
   *
   * @param userId user from whom the event originated.
   * @param threadId for which the event is scoped.
   */
  public void sendTypingEvent(@NotNull String userId, @NotNull UUID threadId) {
    final var thread = this.threadRepository.findByThreadId(threadId)
                           .orElseThrow(() -> new EntityNotFoundException(String.format("Thread with given id %s was not found", threadId)));

    for (UserUDT member: thread.getMembers()) {
      if (member.getUid().equals(userId)) continue; //exclude from delivery the current user

      final var presenceEvent = ThreadPresenceEvent.newBuilder()
                                    .setUid(userId)
                                    .setType(PresenceEventType.TYPING)
                                    .build();

      final var presenceEventKey = ThreadEventKey.newBuilder()
                                       .setUid(member.getUid())
                                       .setThreadId(threadId.toString())
                                       .build();

      this.kafkaAvroKeyAvroValueTemplate.send(this.kafkaProps.getTopics().getThreadEvents(),
          presenceEventKey, presenceEvent);
    }
  }

  public void sendUserPresenceEvent(String userId) {
    final var user = this.userService.findUserByUid(userId);
    final Instant now = Instant.now();

    this.userRepository.updateLastSeenAtByUserId(user.getUid(), now);

    final var presenceEvent = UserPresenceEvent.newBuilder()
                                  .setUserId(userId)
                                  .setLastSeenAt(now.toEpochMilli())
                                  .build();

    for (UserUDT contact: user.getContacts()) {
      final var future = this.kafkaStringKeyAvroValueTemplate.send(this.kafkaProps.getTopics().getUserPresence(), contact.getUid(), presenceEvent);
    }
  }
}
