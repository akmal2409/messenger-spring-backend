package com.akmal.messengerspringbackend.config;

import com.akmal.messengerspringbackend.config.condition.ScriptInitCondition;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.cassandra.SessionFactory;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.config.SessionFactoryFactoryBean;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.cql.session.init.CompositeKeyspacePopulator;
import org.springframework.data.cassandra.core.cql.session.init.ResourceKeyspacePopulator;
import org.springframework.data.cassandra.core.cql.session.init.SessionFactoryInitializer;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.SimpleTupleTypeFactory;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;

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
public class CassandraConfiguration {
  private final CassandraConfigurationProperties cassandraProps;

  /**
   * {@link CqlSessionFactoryBean} is favoured over plain {@link CqlSession}
   * due to its ability to translate the exception and wrap them using common interface
   * of {@link org.springframework.dao.DataAccessException} type
   * @return Cqlsh session bean
   */

  @Bean
  CqlSessionFactoryBean session() {
    final var session = new CqlSessionFactoryBean();
    session.setContactPoints(String.join(",", cassandraProps.getSeeds()));
    session.setKeyspaceName(this.cassandraProps.getKeyspace().getName());
    session.setLocalDatacenter(this.cassandraProps.getDc());
    log.info("Setting cassandra contact points {} in DC: {} and Keyspace: {}",
        String.join(",", cassandraProps.getSeeds()),
        this.cassandraProps.getDc(),
        this.cassandraProps.getKeyspace().getName());

    return session;
  }

  @Bean
  public CassandraMappingContext mappingContext(CqlSession cqlSession) {
    return new CassandraMappingContext();
  }

  @Bean
  CassandraConverter converter(CassandraMappingContext mappingContext, CqlSession cqlSession) {
    final var converter = new MappingCassandraConverter(mappingContext);
    converter.setUserTypeResolver(new SimpleUserTypeResolver(cqlSession,
        CqlIdentifier.fromCql(this.cassandraProps.getKeyspace().getName())));
    converter.setCodecRegistry(cqlSession.getContext().getCodecRegistry());
    return converter;
  }

  @Bean
  SessionFactoryFactoryBean cassandraSessionFactory(CqlSession cqlSession, CassandraConverter converter) {
    final var sessionFactory = new SessionFactoryFactoryBean();
    sessionFactory.setSession(cqlSession);
    sessionFactory.setConverter(converter);
    sessionFactory.setSchemaAction(SchemaAction.NONE);
    return sessionFactory;
  }

  @Bean
  CassandraOperations cassandraTemplate(SessionFactory sessionFactory, CassandraConverter converter) {
    return new CassandraTemplate(sessionFactory, converter);
  }

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
          this.cassandraProps.getSchema()
              .getScripts().stream()
              .map(ClassPathResource::new).toArray(ClassPathResource[]::new));
    }

    ResourceKeyspacePopulator dataPopulator = null;

    if (this.cassandraProps.getData().isInit()) {
      dataPopulator = new ResourceKeyspacePopulator();
      dataPopulator.setSeparator("@@");
      dataPopulator.setScripts();
      dataPopulator.setScripts(
          this.cassandraProps.getData()
              .getScripts().stream()
              .map(ClassPathResource::new).toArray(ClassPathResource[]::new));
    }

    final var compositeKeyspacePopulator = new CompositeKeyspacePopulator();

    if (schemaPopulator != null) compositeKeyspacePopulator.addPopulators(schemaPopulator);
    if (dataPopulator != null) compositeKeyspacePopulator.addPopulators(dataPopulator);

    sessionFactoryInitializer.setKeyspacePopulator(compositeKeyspacePopulator);
    return sessionFactoryInitializer;
  }
}
