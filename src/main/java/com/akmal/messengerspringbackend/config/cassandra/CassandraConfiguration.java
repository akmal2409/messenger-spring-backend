package com.akmal.messengerspringbackend.config.cassandra;

import com.akmal.messengerspringbackend.config.condition.ScriptInitCondition;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.cassandra.SessionFactory;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.AbstractSessionConfiguration;
import org.springframework.data.cassandra.config.CompressionType;
import org.springframework.data.cassandra.core.AsyncCassandraOperations;
import org.springframework.data.cassandra.core.AsyncCassandraTemplate;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.cql.QueryOptions;
import org.springframework.data.cassandra.core.cql.WriteOptions;
import org.springframework.data.cassandra.core.cql.session.init.CompositeKeyspacePopulator;
import org.springframework.data.cassandra.core.cql.session.init.ResourceKeyspacePopulator;
import org.springframework.data.cassandra.core.cql.session.init.SessionFactoryInitializer;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 20/05/2022 - 20:26
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class CassandraConfiguration extends AbstractCassandraConfiguration {
  private final CassandraConfigurationProperties cassandraProps;

  /**
   * Global configuration of the reads from Cassandra. Consistency Level is set to LOCAL_QUORUM,
   * which can be overridden on per-request basis. Execution profile is set to custom retries, which
   * defines custom retry strategy, see {@link LocalDcConsistencyDowngradingRetryPolicy} for more
   * information. Lastly, all requests by default are marked as idempotent due to counters not being
   * used.
   *
   * @return {@link QueryOptions} global query configurations.
   */
  @Bean
  QueryOptions queryOptions() {
    return QueryOptions.builder()
        .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
        .executionProfile("custom-retries")
        .idempotent(true)
        .build();
  }

  /**
   * Same configuration as above {@link CassandraConfiguration#queryOptions()} but for writes.
   *
   * @return {@link WriteOptions} global write configurations.
   */
  @Bean
  WriteOptions writeOptions() {
    return WriteOptions.builder()
        .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
        .executionProfile("custom-retries")
        .idempotent(true)
        .build();
  }

  /**
   * The instance is a bean that provides common API and a high level of abstraction on top of the
   * {@link com.datastax.oss.driver.api.core.CqlSession} used for synchronous, blocking
   * communication.
   *
   * @param sessionFactory configured in {@link
   *     AbstractSessionConfiguration#getRequiredSessionFactory()}
   * @param converter entity converter in {@link
   *     AbstractCassandraConfiguration#cassandraConverter()}
   * @return {@link CassandraOperations}
   */
  @Bean
  CassandraOperations cassandraTemplate(
      SessionFactory sessionFactory, CassandraConverter converter) {
    return new CassandraTemplate(sessionFactory, converter);
  }

  /**
   * Same as above {@link CassandraConfiguration#cassandraTemplate(SessionFactory,
   * CassandraConverter)}, however, AsyncOperations is for asynchronous requests between the client
   * and the database.
   *
   * @param sessionFactory configured in {@link
   *     AbstractSessionConfiguration#getRequiredSessionFactory()}
   * @param converter entity converter in {@link
   *     AbstractCassandraConfiguration#cassandraConverter()}
   * @return {@link AsyncCassandraOperations}
   */
  @Bean
  AsyncCassandraOperations asyncCassandraOperations(
      SessionFactory sessionFactory, CassandraConverter converter) {
    return new AsyncCassandraTemplate(sessionFactory, converter);
  }

  /**
   * Standard Spring's resource populators adjusted for spring-data-cassandra's API. There is a
   * support for two scripts, namely schema.cql and data.cql. Both scripts can be enabled and
   * specified in the {@link CassandraConfigurationProperties#getData()} and {@link
   * CassandraConfigurationProperties#getSchema()}.
   *
   * At the start up if one of the scripts is enabled through the the application properties then
   * @Conditional is triggered and the bean is injected into the spring's context with the loaded
   * scipts that are executed in the following order.
   * 1) Schema creation
   * 2) Data population
   *
   * @param sessionFactory {@link AbstractSessionConfiguration#getRequiredSessionFactory()}
   * @return {@link SessionFactoryInitializer}
   */
  @Bean
  @Conditional(ScriptInitCondition.class)
  SessionFactoryInitializer sessionFactoryInitializer(SessionFactory sessionFactory) {
    final var sessionFactoryInitializer = new SessionFactoryInitializer();
    sessionFactoryInitializer.setSessionFactory(sessionFactory);

    ResourceKeyspacePopulator schemaPopulator = null;

    if (this.cassandraProps.getSchema().isInit()) {
      schemaPopulator = new ResourceKeyspacePopulator();
      schemaPopulator.setSeparator(";");
      schemaPopulator.setScripts(
          this.cassandraProps.getSchema().getScripts().stream()
              .map(ClassPathResource::new)
              .toArray(ClassPathResource[]::new));
    }

    ResourceKeyspacePopulator dataPopulator = null;

    if (this.cassandraProps.getData().isInit()) {
      dataPopulator = new ResourceKeyspacePopulator();
      dataPopulator.setSeparator("@@");
      dataPopulator.setScripts();
      dataPopulator.setScripts(
          this.cassandraProps.getData().getScripts().stream()
              .map(ClassPathResource::new)
              .toArray(ClassPathResource[]::new));
    }

    final var compositeKeyspacePopulator = new CompositeKeyspacePopulator();

    if (schemaPopulator != null) compositeKeyspacePopulator.addPopulators(schemaPopulator);
    if (dataPopulator != null) compositeKeyspacePopulator.addPopulators(dataPopulator);

    sessionFactoryInitializer.setKeyspacePopulator(compositeKeyspacePopulator);
    return sessionFactoryInitializer;
  }

  @Override
  protected Resource getDriverConfigurationResource() {
    return this.cassandraProps.getDriverConfig();
  }

  @Override
  public String[] getEntityBasePackages() {
    return new String[] {"com.akmal.messengerspringbackend.model"};
  }

  @Override
  protected String getKeyspaceName() {
    return this.cassandraProps.getKeyspace().getName();
  }

  @Override
  protected String getContactPoints() {
    return String.join(",", this.cassandraProps.getSeeds());
  }

  @Override
  protected String getLocalDataCenter() {
    return this.cassandraProps.getDc();
  }

  @Override
  protected String getSessionName() {
    return this.cassandraProps.getClusterName();
  }

  @Override
  protected CompressionType getCompressionType() {
    return CompressionType.LZ4;
  }
}
