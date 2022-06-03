package com.akmal.messengerspringbackend.repository;

import com.akmal.messengerspringbackend.model.User;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 01/06/2022 - 20:53
 * @project messenger-spring-backend
 * @since 1.0
 */
public interface UserRepository {

  Optional<User> findByUid(UUID uid);
}
