package com.akmal.messengerspringbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraReactiveRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraRepositoriesAutoConfiguration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(
    exclude = {
      CassandraAutoConfiguration.class,
      CassandraDataAutoConfiguration.class,
      CassandraReactiveDataAutoConfiguration.class,
      CassandraRepositoriesAutoConfiguration.class,
      CassandraReactiveRepositoriesAutoConfiguration.class
    })
@EnableAsync
@EnableKafka
public class MessengerSpringBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(MessengerSpringBackendApplication.class, args);
  }

}
