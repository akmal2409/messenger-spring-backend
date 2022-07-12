package com.akmal.messengerspringbackend;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 30/06/2022 - 21:07
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "project.cors")
public class CorsConfigurationProperties {
  @Getter @Setter
  private String[] allowedOrigins;
  @Getter @Setter
  private String[] allowedMethods;
  @Getter @Setter
  private String[] allowedHeaders;
  @Getter @Setter
  private boolean enabled = false;
}
