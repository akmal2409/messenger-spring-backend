package com.akmal.messengerspringbackend.config.condition;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * The class represents a custom condition that decides whether to enable
 * the bean or not.
 *
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 20/05/2022 - 21:14
 * @project messenger-spring-backend
 * @since 1.0
 */
@Slf4j
public class ScriptInitCondition implements Condition {


  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    final var env = Objects.requireNonNull(context.getEnvironment(), "Environment "
                                                                         + "was null");

    final boolean initSchema = "true".equals(env.getProperty("project.cassandra.schema.init"));
    final boolean initData = "true".equals(env.getProperty("project.cassandra.data.init"));

    if (initSchema || initData) {
      log.info("Enabling SessionFactoryInitializer bean because either schema.init "
                   + "or data.init was set to true");
      return true;
    }

    return false;
  }
}
