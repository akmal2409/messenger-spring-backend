package com.akmal.messengerspringbackend.controller.v1.rest;

import com.akmal.messengerspringbackend.dto.v1.UserDTO;
import com.akmal.messengerspringbackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 02/07/2022 - 17:57
 * @project messenger-spring-backend
 * @since 1.0
 */
@RestController
@RequestMapping(UserController.BASE_URL)
@RequiredArgsConstructor
public class UserController {
  public static final String BASE_URL = "/api/v1/users";
  private final UserService userService;

  @GetMapping("/{userId}")
  public UserDTO findById(@PathVariable String userId) {
    return UserDTO.from(this.userService.findUserByUid(userId));
  }
}
