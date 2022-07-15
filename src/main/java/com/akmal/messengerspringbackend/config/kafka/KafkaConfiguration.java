package com.akmal.messengerspringbackend.config.kafka;

import com.akmal.messengerspringbackend.thread.ThreadEventKey;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 05/06/2022 - 18:48
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
@RequiredArgsConstructor
public class KafkaConfiguration {

  private final KafkaConfigurationProperties kafkaProps;

  @Bean
  ConcurrentKafkaListenerContainerFactory<SpecificRecord, SpecificRecord>
      kafkaListenerContainerFactoryAvroKeyAvroValue(
          @Qualifier("consumerFactoryAvroKeyAvroValue") ConsumerFactory<SpecificRecord, SpecificRecord> consumerFactory) {
    final var containerFactory =
        new ConcurrentKafkaListenerContainerFactory<SpecificRecord, SpecificRecord>();
    containerFactory.setConsumerFactory(consumerFactory);
    return containerFactory;
  }

  @Bean
  ConcurrentKafkaListenerContainerFactory<String, SpecificRecord>
  kafkaListenerContainerFactoryStringKeyAvroValue(
      @Qualifier("consumerFactoryStringKeyAvroValue") ConsumerFactory<String, SpecificRecord> consumerFactory) {
    final var containerFactory =
        new ConcurrentKafkaListenerContainerFactory<String, SpecificRecord>();
    containerFactory.setConsumerFactory(consumerFactory);
    return containerFactory;
  }

  @Bean
  ConsumerFactory<SpecificRecord, SpecificRecord> consumerFactoryAvroKeyAvroValue() throws ClassNotFoundException {
    return new DefaultKafkaConsumerFactory<>(this.kafkaProps.consumerProps());
  }

  @Bean
  ConsumerFactory<String, SpecificRecord> consumerFactoryStringKeyAvroValue() throws ClassNotFoundException {
    final var props = this.kafkaProps.consumerProps();
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  ProducerFactory<SpecificRecord, SpecificRecord> producerFactoryAvroKeyAvroValue()
      throws ClassNotFoundException {
    return new DefaultKafkaProducerFactory<>(this.kafkaProps.producerProps());
  }

  @Bean
  ProducerFactory<String, SpecificRecord> producerFactoryStringKeyAvroValue()
      throws ClassNotFoundException {
    final var props = this.kafkaProps.producerProps();
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  KafkaTemplate<SpecificRecord, SpecificRecord> kafkaTemplateAvroKeyAvroValue(
      @Qualifier("producerFactoryAvroKeyAvroValue") ProducerFactory<SpecificRecord, SpecificRecord> factory) {
    return new KafkaTemplate<>(factory);
  }

  @Bean
  KafkaTemplate<String, SpecificRecord> kafkaTemplateStringKeyAvroValue(
      @Qualifier("producerFactoryStringKeyAvroValue") ProducerFactory<String, SpecificRecord> factory) {
    return new KafkaTemplate<>(factory);
  }
}
