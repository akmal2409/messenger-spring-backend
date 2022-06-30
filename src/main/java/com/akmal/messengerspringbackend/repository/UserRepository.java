package com.akmal.messengerspringbackend.repository;

import com.akmal.messengerspringbackend.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 01/06/2022 - 20:53
 * @project messenger-spring-backend
 * @since 1.0
 */
public interface UserRepository {

  List<User> findAllByIds(Collection<String> ids);

  Optional<User> findByUid(String uid);

  User save(User user);
}
