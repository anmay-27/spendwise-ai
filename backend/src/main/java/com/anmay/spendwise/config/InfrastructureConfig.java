package com.anmay.spendwise.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class InfrastructureConfig {
  @Bean
  NewTopic transactionEvents() {
    return TopicBuilder.name("transaction-events").partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic notificationEvents() {
    return TopicBuilder.name("notification-events").partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic deadLetters() {
    return TopicBuilder.name("transaction-events.DLT").partitions(3).replicas(1).build();
  }

  @Bean
  org.springframework.kafka.listener.DefaultErrorHandler kafkaErrorHandler(
      org.springframework.kafka.core.KafkaTemplate<String, String> template) {
    return new org.springframework.kafka.listener.DefaultErrorHandler(
        new org.springframework.kafka.listener.DeadLetterPublishingRecoverer(template),
        new org.springframework.util.backoff.FixedBackOff(1000, 4));
  }
}
