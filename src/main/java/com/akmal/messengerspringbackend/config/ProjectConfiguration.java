package com.akmal.messengerspringbackend.config;

import com.akmal.messengerspringbackend.shared.timeago.TimeAgoConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

  @Bean
  public AsyncTaskExecutor asyncExecutor() {
    final var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(7);
    executor.setMaxPoolSize(40);
    executor.setQueueCapacity(11);
    executor.setThreadNamePrefix("async-executor-");
    executor.initialize();
    return executor;
  }
}
