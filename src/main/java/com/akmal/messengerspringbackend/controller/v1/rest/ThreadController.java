package com.akmal.messengerspringbackend.controller.v1.rest;

import com.akmal.messengerspringbackend.dto.v1.LatestThreadDTO;
import com.akmal.messengerspringbackend.dto.v1.ThreadCreationRequest;
import com.akmal.messengerspringbackend.dto.v1.ThreadDTO;
import com.akmal.messengerspringbackend.service.ThreadService;
import com.akmal.messengerspringbackend.shared.responses.Responses;
import java.util.Collection;
import java.util.UUID;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 26/06/2022 - 17:49
 * @project messenger-spring-backend
 * @since 1.0
 */
@RestController
@RequestMapping(ThreadController.BASE_URL)
@RequiredArgsConstructor
public class ThreadController {
  public static final String BASE_URL = "/api/v1/users/{userId}/threads";
  private final ThreadService threadService;

  @GetMapping
  public ResponseEntity<Collection<LatestThreadDTO>> findAllLatestByUser(
      @PathVariable String userId) {
    return Responses.wrap(this.threadService.findAllLatestByUser(userId));
  }

  @GetMapping("/{threadId}")
  public ResponseEntity<ThreadDTO> findById(@PathVariable UUID threadId) {
    return Responses.wrap(this.threadService.findById(threadId));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ThreadDTO createThread(
      @PathVariable String userId,
      @RequestBody @Valid ThreadCreationRequest threadCreationRequest) {
    return this.threadService.createThread(userId, threadCreationRequest);
  }
}
