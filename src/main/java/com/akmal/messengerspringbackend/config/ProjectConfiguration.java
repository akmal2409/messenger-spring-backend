package com.akmal.messengerspringbackend.config;

import com.akmal.messengerspringbackend.shared.timeago.TimeAgoConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 26/06/2022 - 19:14
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
public class ProjectConfiguration {

  @Bean
  TimeAgoConverter timeAgoConverter() {
    return TimeAgoConverter.withDefaults();
  }
}
