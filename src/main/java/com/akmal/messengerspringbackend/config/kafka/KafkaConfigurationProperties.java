package com.akmal.messengerspringbackend.config.kafka;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.subject.TopicNameStrategy;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 05/06/2022 - 18:48
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "project.kafka")
public class KafkaConfigurationProperties {
  @Getter @Setter
  private String partitionAssignmentStrategy = CooperativeStickyAssignor.class.getName();

  @Getter @Setter private String autoOffsetReset = "earliest";

  @Getter @Setter private String keyDeserializer = StringDeserializer.class.getName();
  @Getter @Setter private String valueDeserializer = StringDeserializer.class.getName();

  @Getter @Setter private String keySerializer = StringSerializer.class.getName();
  @Getter @Setter private String valueSerializer = StringSerializer.class.getName();

  @Getter @Setter private String groupId = "instance-1";
  @Getter @Setter private String bootstrapServers = "localhost:9092";

  @Getter @Setter private SchemaRegistryConfig schemaRegistry = SchemaRegistryConfig.withDefaults();

  @Getter @Setter private TopicNames topics;

  Map<String, Object> consumerProps() throws ClassNotFoundException {
    final var props = new HashMap<String, Object>();
    props.put(
        ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, this.partitionAssignmentStrategy);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, this.autoOffsetReset);

    final Class<?> valueDeSerializerClass =
        Thread.currentThread().getContextClassLoader().loadClass(valueDeserializer);
    final Class<?> keyDeSerializerClass =
        Thread.currentThread().getContextClassLoader().loadClass(keyDeserializer);

    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeSerializerClass);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeSerializerClass);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, this.groupId);
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);

    if (this.schemaRegistry.enabled) {
      props.put(
          AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION, this.schemaRegistry.useLatestVersion);
      props.put(
          AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS,
          this.schemaRegistry.autoRegisterSchemas);
      props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.schemaRegistry.url);
      props.put(
          KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG,
          this.schemaRegistry.specificAvroReader);
      props.put(
          AbstractKafkaSchemaSerDeConfig.KEY_SUBJECT_NAME_STRATEGY,
          TopicNameStrategy.class.getName());
      props.put(
          AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY,
          TopicNameStrategy.class.getName());
    }

    return props;
  }

  Map<String, Object> producerProps() throws ClassNotFoundException {
    final var props = new HashMap<String, Object>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Class.forName(this.valueSerializer));
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, Class.forName(this.keySerializer));
    props.put(ProducerConfig.ACKS_CONFIG, "all");

    if (this.schemaRegistry.enabled) {
      props.put(
          AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION, this.schemaRegistry.useLatestVersion);
      props.put(
          AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS,
          this.schemaRegistry.autoRegisterSchemas);
      props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.schemaRegistry.url);
      props.put(
          AbstractKafkaSchemaSerDeConfig.KEY_SUBJECT_NAME_STRATEGY,
          TopicNameStrategy.class.getName());
      props.put(
          AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY,
          TopicNameStrategy.class.getName());
    }
    return props;
  }

  @Data
  public static class TopicNames {
    private String threadEvents;
    private String userPresence;
  }

  @Data
  @Builder
  static class SchemaRegistryConfig {
    private boolean enabled;
    private String url;
    private boolean useLatestVersion;
    private boolean autoRegisterSchemas;
    private boolean specificAvroReader;

    private static SchemaRegistryConfig withDefaults() {
      return SchemaRegistryConfig.builder()
          .url("http://localhost:8081")
          .useLatestVersion(true)
          .autoRegisterSchemas(false)
          .enabled(true)
          .specificAvroReader(true)
          .build();
    }
  }
}
