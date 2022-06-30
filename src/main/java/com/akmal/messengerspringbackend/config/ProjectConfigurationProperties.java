package com.akmal.messengerspringbackend.config;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 01/06/2022 - 20:32
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "project")
@Getter
@Setter
public class ProjectConfigurationProperties {
  private long customEpochMilli = Instant.EPOCH.toEpochMilli();
  private int nodeId = -1;
}
