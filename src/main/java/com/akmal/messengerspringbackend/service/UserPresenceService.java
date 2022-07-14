package com.akmal.messengerspringbackend.service;

import com.akmal.messengerspringbackend.config.kafka.KafkaConfigurationProperties;
import com.akmal.messengerspringbackend.dto.v1.TypingEvent;
import com.akmal.messengerspringbackend.exception.EntityNotFoundException;
import com.akmal.messengerspringbackend.model.udt.UserUDT;
import com.akmal.messengerspringbackend.repository.ThreadRepository;
import com.akmal.messengerspringbackend.thread.PresenceEventType;
import com.akmal.messengerspringbackend.thread.ThreadEventKey;
import com.akmal.messengerspringbackend.thread.ThreadPresenceEvent;
import com.akmal.messengerspringbackend.websocket.storage.WebsocketSessionStorage;
import java.util.LinkedList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
  private final KafkaTemplate<SpecificRecordBase, SpecificRecordBase> kafkaTemplate;
  private final KafkaConfigurationProperties kafkaProps;
  private final ThreadRepository threadRepository;
  private final SimpMessagingTemplate wsMessagingTemplate;
  private final WebsocketSessionStorage websocketSessionStorage;

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

      this.kafkaTemplate.send(this.kafkaProps.getTopics().getThreadEvents(),
          presenceEventKey, presenceEvent);
    }
  }

  /**
   * Sends typing presence notification to the user.
   *
   * @param userId for whom the event was destined.
   * @param authorId the person who triggered the event.
   * @param threadId thread in which the event happened.
   */
  public void notifyUserOfTypingEvent(@NotNull String userId,
      @NotNull String authorId, @NotNull UUID threadId) {

    if (this.websocketSessionStorage.isUserConnected(userId)) {
      this.wsMessagingTemplate.convertAndSendToUser(userId,
          "/queue/typing", new TypingEvent(authorId, threadId.toString()));
    }
  }
}
