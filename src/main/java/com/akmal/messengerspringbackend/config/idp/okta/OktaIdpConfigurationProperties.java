package com.akmal.messengerspringbackend.config.idp.okta;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 18/06/2022 - 17:50
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "project.idp.okta")
class OktaIdpConfigurationProperties {
  @Getter @Setter private String apiKey;
  @Getter @Setter private String orgUrl;
}
