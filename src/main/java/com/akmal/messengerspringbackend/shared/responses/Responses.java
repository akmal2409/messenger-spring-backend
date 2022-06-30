package com.akmal.messengerspringbackend.shared.responses;

import java.util.Collection;
import java.util.Optional;
import org.springframework.http.ResponseEntity;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 27/06/2022 - 20:11
 * @project messenger-spring-backend
 * @since 1.0
 */
public final class Responses {

  public static <T> ResponseEntity<T> wrap(T item) {
    return Optional.ofNullable(item)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }

  public static <T> ResponseEntity<Collection<T>> wrap(Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(c -> c.isEmpty() ? null : ResponseEntity.ok(c))
        .orElse(ResponseEntity.noContent().build());
  }
}
