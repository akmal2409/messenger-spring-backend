package com.akmal.messengerspringbackend.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.akmal.messengerspringbackend.dto.v1.MessageDTO;
import com.akmal.messengerspringbackend.dto.v1.ScrollContent;
import com.akmal.messengerspringbackend.model.MessageByUserByThread;
import com.akmal.messengerspringbackend.model.MessageByUserByThread.Key;
import com.akmal.messengerspringbackend.repository.MessageRepository;
import com.akmal.messengerspringbackend.repository.ThreadRepository;
import com.akmal.messengerspringbackend.repository.UserRepository;
import com.akmal.messengerspringbackend.shared.BucketingManager;
import com.akmal.messengerspringbackend.snowflake.SnowflakeGenerator;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 05/06/2022 - 14:12
 * @project messenger-spring-backend
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
  private static final int FETCH_SIZE = 25;
  private static final String USER_ID = "fef0d7a7-8af6-46d1-bbcd-94f6483d3645";
  private static final UUID THREAD_ID = Uuids.startOf(1640995200000L);

  @Mock private MessageRepository messageRepository;
  @Mock private UserRepository userRepository;
  @Mock private ThreadRepository threadRepository;
  @Mock private SnowflakeGenerator snowflakeGenerator;
  @Mock private BucketingManager bucketingManager;

  @InjectMocks private MessageService messageService;

  @Captor private ArgumentCaptor<String> pagingStateCaptor;
  @Captor private ArgumentCaptor<Integer> bucketCaptor;

  @Test
  @DisplayName("Should find all messages before provided message id in a bucket successfully")
  void shouldFindAllMessagesBeforeMessageId() {
    // given
    final ScrollContent<MessageByUserByThread> expectedMessages =
        this.generateMessages(FETCH_SIZE, 1, 0, USER_ID, THREAD_ID);
    final ScrollContent<MessageDTO> expectedDTOs = this.mapScrollContentToDTO(expectedMessages);

    // when
    when(this.messageRepository.findAllBeforeMessageId(
            any(String.class), any(UUID.class), anyInt(), anyInt(), anyLong()))
        .thenReturn(expectedMessages);

    // then
    final ScrollContent<MessageDTO> actualMessages =
        this.messageService.findAllByUserAndThreadAndBucket(
            USER_ID, THREAD_ID, 0, (long) (FETCH_SIZE + 1), "");

    assertThat(actualMessages).extracting(ScrollContent::content).isEqualTo(expectedDTOs.content());
    verify(messageRepository, times(1))
        .findAllBeforeMessageId(USER_ID, THREAD_ID, 0, FETCH_SIZE, FETCH_SIZE + 1);
    verify(messageRepository, never())
        .findAllByUidAndThreadIdAndBucket(any(), any(), anyInt(), anyInt(), any());
    verify(bucketingManager, never()).makeBucket();
  }

  // test if the beforeMessageId is provided but bucket is not, default to bucket generation
  @Test
  @DisplayName(
      "Should default to .makeBucket() to generate a bucket if before message id "
          + "was provided but no bucket specified")
  void shouldDefaultToBucketGenerationIfNoBucketProvidedBeforeMessageId() {
    // given
    final ScrollContent<MessageByUserByThread> expectedMessages =
        this.generateMessages(FETCH_SIZE, 1, 0, USER_ID, THREAD_ID);
    final ScrollContent<MessageDTO> expectedDTOs = this.mapScrollContentToDTO(expectedMessages);
    final var generatedBucket = 0;

    // when
    when(this.messageRepository.findAllByUidAndThreadIdAndBucket(
            USER_ID, THREAD_ID, generatedBucket, FETCH_SIZE, null))
        .thenReturn(expectedMessages);
    when(this.bucketingManager.makeBucket()).thenReturn(generatedBucket);

    // then
    final ScrollContent<MessageDTO> actualMessages =
        this.messageService.findAllByUserAndThreadAndBucket(
            USER_ID, THREAD_ID, null, (long) (FETCH_SIZE + 1), "PAGING_STATE");

    assertThat(actualMessages).extracting(ScrollContent::content).isEqualTo(expectedDTOs.content());

    verify(messageRepository)
        .findAllByUidAndThreadIdAndBucket(
            any(), any(), bucketCaptor.capture(), anyInt(), pagingStateCaptor.capture());

    assertThat(bucketCaptor.getValue()).isEqualTo(generatedBucket);

    assertThat(pagingStateCaptor.getValue()).isNull();

    verify(this.bucketingManager, times(1)).makeBucket();
    verify(this.messageRepository, times(1))
        .findAllByUidAndThreadIdAndBucket(USER_ID, THREAD_ID, 0, FETCH_SIZE, null);

    verify(this.messageRepository, never())
        .findAllBeforeMessageId(any(), any(), anyInt(), anyInt(), anyLong());
  }

  // test if bucket is provided and not beforeMessageId, then it will fetch for a bucket
  @Test
  @DisplayName(
      "Should find all the records in a bucket if the bucket is provided and "
          + "beforeMessageId is null")
  void shouldFindAllIfBucketProvidedAndNotBeforeMessageId() {
    // given
    final ScrollContent<MessageByUserByThread> expectedMessages =
        this.generateMessages(FETCH_SIZE, 1, 0, USER_ID, THREAD_ID);
    final ScrollContent<MessageDTO> expectedDTOs = this.mapScrollContentToDTO(expectedMessages);

    // when
    when(this.messageRepository.findAllByUidAndThreadIdAndBucket(
            USER_ID, THREAD_ID, 0, FETCH_SIZE, "PAGING_STATE"))
        .thenReturn(expectedMessages);

    // then
    final ScrollContent<MessageDTO> actualMessages =
        this.messageService.findAllByUserAndThreadAndBucket(
            USER_ID, THREAD_ID, 0, null, "PAGING_STATE");

    verify(this.messageRepository)
        .findAllByUidAndThreadIdAndBucket(
            any(), any(), anyInt(), anyInt(), pagingStateCaptor.capture());

    assertThat(pagingStateCaptor.getValue()).isEqualTo("PAGING_STATE");
    assertThat(actualMessages).extracting(ScrollContent::content).isEqualTo(expectedDTOs.content());

    verify(this.messageRepository, times(1))
        .findAllByUidAndThreadIdAndBucket(any(), any(), anyInt(), anyInt(), any());
    verify(this.bucketingManager, never()).makeBucket();
    verify(this.messageRepository, never())
        .findAllBeforeMessageId(any(), any(), anyInt(), anyInt(), anyLong());
  }

  @Test
  @DisplayName(
      "Should find all messages for the current bucket when no "
          + "fromMessageId is provided and bucket number")
  void shouldFindAllNoArgsProvided() {
    // given
    final ScrollContent<MessageByUserByThread> expectedMessages =
        this.generateMessages(FETCH_SIZE, 1, 0, USER_ID, THREAD_ID);
    final ScrollContent<MessageDTO> expectedDTOs = this.mapScrollContentToDTO(expectedMessages);

    // when
    when(
        this.messageRepository.findAllByUidAndThreadIdAndBucket(
            USER_ID, THREAD_ID, 0, FETCH_SIZE, null)).thenReturn(expectedMessages);

    // then
    final ScrollContent<MessageDTO> actualMessages =
        this.messageService.findAllByUserAndThreadAndBucket(USER_ID, THREAD_ID, null, null, null);

    assertThat(actualMessages).extracting(ScrollContent::content).isEqualTo(expectedDTOs.content());

    verify(this.messageRepository, times(1))
        .findAllByUidAndThreadIdAndBucket(USER_ID, THREAD_ID, 0, FETCH_SIZE, null);
    verify(this.bucketingManager, times(1)).makeBucket();
    verify(this.messageRepository, never())
        .findAllBeforeMessageId(any(), any(), anyInt(), anyInt(), anyLong());
  }

  @Test
  void sendMessage() {}

  private ScrollContent<MessageByUserByThread> generateMessages(
      int numberOfMessages, long messageIdStart, int bucket, String uid, UUID threadId) {
    List<MessageByUserByThread> messages = new LinkedList<>();

    for (int i = 0; i < numberOfMessages; i++) {
      messages.add(
          MessageByUserByThread.builder()
              .key(new Key(uid, threadId, bucket, messageIdStart))
              .body("Message=" + messageIdStart++)
              .authorId(uid)
              .build());
    }

    return ScrollContent.of(messages, "PAGING_STATE");
  }

  private ScrollContent<MessageDTO> mapScrollContentToDTO(
      ScrollContent<MessageByUserByThread> scrollContent) {
    return ScrollContent.of(
        scrollContent.stream().map(m -> m.toDTO(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC))).toList(),
        scrollContent.pagingState());
  }
}
