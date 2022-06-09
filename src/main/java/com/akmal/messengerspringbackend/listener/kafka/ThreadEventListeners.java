package com.akmal.messengerspringbackend.listener.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 07/06/2022 - 19:18
 * @project messenger-spring-backend
 * @since 1.0
 */
@Component
@KafkaListener(
    id = "${project.kafka.group-id}",
    topics = "${project.kafka.topics.thread-events}"
)
@Slf4j
public class ThreadEventListeners {

  @KafkaHandler
  public void listenToMessages(SpecificRecord threadEvent) {
      log.info("Received message event {}", (threadEvent));
  }

}
