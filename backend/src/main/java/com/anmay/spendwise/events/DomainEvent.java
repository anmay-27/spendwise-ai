package com.anmay.spendwise.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(
    UUID eventId,
    String eventType,
    Long transactionId,
    Long userId,
    Instant timestamp,
    int schemaVersion,
    Map<String, Object> payload) {}
