package com.akmal.messengerspringbackend.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 20/05/2022 - 20:12
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "project.cassandra")
@Validated
public class CassandraConfigurationProperties {
  @Setter
  @Getter
  @NotNull
  private KeyspaceMetadata keyspace;
  @Getter
  @Setter
  private String dc = "dc1";
  @Getter
  @Setter
  private String rack = "rack1";
  @Getter
  @Setter
  private Collection<String> seeds;

  @Getter
  @Setter
  private ScriptBootstrapConfig schema;
  @Getter
  @Setter
  private ScriptBootstrapConfig data;

  @Getter
  @Setter
  public static class KeyspaceMetadata {
    @NotBlank(message = "Keyspace must not be blank")
    private String name;
    private int replicationFactor;
    private boolean durableWrites;
  }

  @Getter
  @Setter
  static class ScriptBootstrapConfig {
    private boolean init;
    private Collection<String> scripts;

    static ScriptBootstrapConfig empty() {
      final var empty = new ScriptBootstrapConfig();
      empty.init = false;
      return empty;
    }
  }

}
