package com.anmay.notifications;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;

@SpringBootApplication
public class NotificationApplication {
  public static void main(String[] args) {
    SpringApplication.run(NotificationApplication.class, args);
  }

  @Bean
  org.springframework.kafka.listener.DefaultErrorHandler errorHandler(
      org.springframework.kafka.core.KafkaTemplate<String, String> template) {
    return new org.springframework.kafka.listener.DefaultErrorHandler(
        new org.springframework.kafka.listener.DeadLetterPublishingRecoverer(template),
        new org.springframework.util.backoff.FixedBackOff(1000, 4));
  }

  @Bean
  org.apache.kafka.clients.admin.NewTopic deadLetters() {
    return org.springframework.kafka.config.TopicBuilder.name("notification-events.DLT")
        .partitions(3)
        .replicas(1)
        .build();
  }
}
