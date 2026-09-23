package com.mfrodriguez.orderwebhook.order;

import com.mfrodriguez.orderwebhook.webhook.OrderHook;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class OrderEvent {

    @Id
    private String id;
    private String orderId;
    private String state;
    private String domain;
    private Instant lastChange;
    private Instant receivedAt;
    @Column(columnDefinition = "text")
    private String payload;

    protected OrderEvent() {
        // JPA
    }

    public static String idFor(OrderHook hook) {
        return hook.orderId() + ":" + hook.state();
    }

    public static OrderEvent from(OrderHook hook, String payload) {
        OrderEvent event = new OrderEvent();
        event.id = idFor(hook);
        event.orderId = hook.orderId();
        event.state = hook.state();
        event.domain = hook.domain();
        event.lastChange = hook.lastChange();
        event.receivedAt = Instant.now();
        event.payload = payload;
        return event;
    }

    public String getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getState() {
        return state;
    }

    public String getDomain() {
        return domain;
    }

    public Instant getLastChange() {
        return lastChange;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getPayload() {
        return payload;
    }
}
