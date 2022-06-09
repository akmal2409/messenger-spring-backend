package com.akmal.messengerspringbackend.config.kafka;

import com.akmal.messengerspringbackend.thread.ThreadEventKey;
import com.akmal.messengerspringbackend.thread.ThreadMessageEvent;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
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
@EnableKafka
public class KafkaConfiguration {

  private final KafkaConfigurationProperties kafkaProps;

  @Bean
  ConcurrentKafkaListenerContainerFactory<ThreadEventKey, SpecificRecord> kafkaListenerContainerFactory(
      ConsumerFactory<ThreadEventKey, SpecificRecord> consumerFactory
  ) {
    final var containerFactory = new ConcurrentKafkaListenerContainerFactory<ThreadEventKey, SpecificRecord>();
    containerFactory.setConsumerFactory(consumerFactory);
    return containerFactory;
  }

  @Bean
  ConsumerFactory<ThreadEventKey, SpecificRecord> consumerFactory() throws ClassNotFoundException {
    return new DefaultKafkaConsumerFactory<>(this.kafkaProps.consumerProps());
  }

  @Bean
  ProducerFactory<ThreadEventKey, SpecificRecord> producerFactory() throws ClassNotFoundException {
    return new DefaultKafkaProducerFactory<>(this.kafkaProps.producerProps());
  }

  @Bean
  KafkaTemplate<ThreadEventKey, SpecificRecord> kafkaTemplate(ProducerFactory<ThreadEventKey, SpecificRecord> factory) {
    return new KafkaTemplate<>(factory);
  }
}
