package com.akmal.messengerspringbackend.service.idp;

import com.akmal.messengerspringbackend.shared.idp.IdpUserMetadata;
import com.okta.sdk.client.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 18/06/2022 - 18:16
 * @project messenger-spring-backend
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class OktaUserService implements IdpUserService {
  private final Client client;

  @Override
  public IdpUserMetadata getUserMetadataById(String uid) {
    final var user = client.getUser(uid);

    return IdpUserMetadata.builder()
        .email(user.getProfile().getEmail())
        .firstName(user.getProfile().getFirstName())
        .lastName(user.getProfile().getLastName())
        .uid(user.getId())
        .build();
  }
}
