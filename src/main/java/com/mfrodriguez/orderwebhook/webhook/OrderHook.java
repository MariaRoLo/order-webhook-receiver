package com.mfrodriguez.orderwebhook.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/** Payload shape of a VTEX Orders Feed/Hook notification. */
public record OrderHook(
        @JsonProperty("Domain") String domain,
        @JsonProperty("OrderId") String orderId,
        @JsonProperty("State") String state,
        @JsonProperty("LastChange") Instant lastChange) {
}
