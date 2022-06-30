package com.akmal.messengerspringbackend.service;

import com.akmal.messengerspringbackend.model.User;
import com.akmal.messengerspringbackend.repository.UserRepository;
import com.akmal.messengerspringbackend.service.idp.IdpUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 18/06/2022 - 17:38
 * @project messenger-spring-backend
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final IdpUserService idpUserService;

  public User getCurrentUser() {
    final var authentication = SecurityContextHolder.getContext().getAuthentication();
    final var uid = authentication.getName(); // mapped from JWT
    return this.findUserByUid(uid);
  }

  public User findUserByUid(String uid) {
    final var userOptional = this.userRepository.findByUid(uid);

    if (userOptional.isPresent()) return userOptional.get();
    // else we have to initialize user object with minimal configuration by calling Okta's API
    // (/user endpoint)

    final var userMetadata = this.idpUserService.getUserMetadataById(uid);
    final var user = User.fromIdpMetadata(userMetadata);
    this.userRepository.save(user);

    return user;
  }
}
