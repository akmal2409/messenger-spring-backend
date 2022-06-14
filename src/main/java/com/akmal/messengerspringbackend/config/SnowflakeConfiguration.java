package com.akmal.messengerspringbackend.config;

import com.akmal.messengerspringbackend.config.ProjectConfigurationProperties;
import com.akmal.messengerspringbackend.snowflake.SimpleSnowflakeGenerator;
import com.akmal.messengerspringbackend.snowflake.SnowflakeGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 01/06/2022 - 20:50
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
public class SnowflakeConfiguration {

  @Bean
  SnowflakeGenerator snowflakeGenerator(ProjectConfigurationProperties projectProps) {
    if (projectProps.getNodeId() != -1) {
      return SimpleSnowflakeGenerator.withCustomEpochAndNodeId(projectProps.getCustomEpochMilli(),
          projectProps.getNodeId());
    } else {
      return SimpleSnowflakeGenerator.withCustomEpoch(projectProps.getCustomEpochMilli());
    }
  }
}
