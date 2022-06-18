package com.akmal.messengerspringbackend.service.idp;

import com.akmal.messengerspringbackend.shared.idp.IdpUserMetadata;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 18/06/2022 - 18:20
 * @project messenger-spring-backend
 * @since 1.0
 */
public interface IdpUserService {

  IdpUserMetadata getUserMetadataById(String uid);
}
