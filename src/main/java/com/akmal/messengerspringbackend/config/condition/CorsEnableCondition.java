package com.akmal.messengerspringbackend.config.condition;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 30/06/2022 - 21:08
 * @project messenger-spring-backend
 * @since 1.0
 */
@Slf4j
public class CorsEnableCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return Optional.ofNullable(context.getEnvironment().getProperty("project.cors.enabled"))
               .map(Boolean::parseBoolean)
               .orElse(false);
  }
}
