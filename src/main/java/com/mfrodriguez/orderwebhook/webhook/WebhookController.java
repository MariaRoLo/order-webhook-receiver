package com.mfrodriguez.orderwebhook.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfrodriguez.orderwebhook.order.OrderEvent;
import com.mfrodriguez.orderwebhook.order.OrderEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class WebhookController {

    private final SignatureVerifier signatureVerifier;
    private final OrderEventRepository orderEventRepository;
    private final ObjectMapper objectMapper;

    public WebhookController(SignatureVerifier signatureVerifier, OrderEventRepository orderEventRepository,
                             ObjectMapper objectMapper) {
        this.signatureVerifier = signatureVerifier;
        this.orderEventRepository = orderEventRepository;
        this.objectMapper = objectMapper;
    }

    // Raw body as String: the signature is computed over the exact bytes sent, so we verify before parsing.
    @PostMapping("/webhooks/orders")
    public ResponseEntity<Map<String, Object>> receive(@RequestBody String body,
                                                       @RequestHeader(value = "X-Signature", required = false) String signature) {
        if (!signatureVerifier.isValid(body, signature)) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid signature"));
        }
        OrderHook hook;
        try {
            hook = objectMapper.readValue(body, OrderHook.class);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "malformed payload"));
        }
        if (hook.orderId() == null || hook.state() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "OrderId and State are required"));
        }

        // Senders retry on timeouts, so the same (order, state) can arrive twice: ack it, don't store it again.
        String eventId = OrderEvent.idFor(hook);
        if (orderEventRepository.existsById(eventId)) {
            return ResponseEntity.ok(Map.of("eventId", eventId, "duplicate", true));
        }
        orderEventRepository.save(OrderEvent.from(hook, body));
        return ResponseEntity.accepted().body(Map.of("eventId", eventId, "duplicate", false));
    }

    @GetMapping("/api/orders/{orderId}/events")
    public List<OrderEvent> events(@PathVariable String orderId) {
        return orderEventRepository.findByOrderIdOrderByReceivedAtAsc(orderId);
    }
}
