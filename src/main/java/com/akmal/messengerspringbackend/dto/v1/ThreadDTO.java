package com.akmal.messengerspringbackend.dto.v1;

import com.akmal.messengerspringbackend.model.Thread;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import lombok.Builder;
import lombok.With;
import org.jetbrains.annotations.NotNull;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 27/06/2022 - 20:05
 * @project messenger-spring-backend
 * @since 1.0
 */
@Builder
@With
public record ThreadDTO(
    String threadId,
    String threadName,
    String threadPictureThumbnailUrl,
    String threadPictureUrl,
    boolean groupThread,
    Collection<UserDetailsDTO> members
) {

  public static ThreadDTO from(@NotNull Thread thread) {
    return ThreadDTO
               .builder()
               .threadId(thread.getThreadId().toString())
               .groupThread(thread.isGroupThread())
               .threadName(thread.getThreadName())
               .threadPictureThumbnailUrl(thread.getThreadPictureThumbnailUrl())
               .threadPictureUrl(thread.getThreadPictureUrl())
               .members(
                   Optional.ofNullable(thread.getMembers())
                       .map(members -> members.stream().map(UserDetailsDTO::from).toList())
                       .orElse(Collections.emptyList()))
               .build();
  }
}
