package com.mfrodriguez.orderwebhook.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:webhooktest")
@AutoConfigureMockMvc
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SignatureVerifier signatureVerifier;

    private static String hook(String orderId, String state) {
        return """
                {"Domain":"Marketplace","OrderId":"%s","State":"%s","LastChange":"2026-09-22T10:00:00Z"}
                """.formatted(orderId, state);
    }

    private ResultActions send(String body, String signature) throws Exception {
        return mockMvc.perform(post("/webhooks/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", signature)
                .content(body));
    }

    @Test
    void storesSignedEventsDedupesRetriesAndListsThemInOrder() throws Exception {
        String created = hook("v100-01", "payment-pending");
        String approved = hook("v100-01", "payment-approved");

        send(created, signatureVerifier.sign(created)).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("v100-01:payment-pending"));
        send(created, signatureVerifier.sign(created)).andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true));
        send(approved, signatureVerifier.sign(approved)).andExpect(status().isAccepted());

        mockMvc.perform(get("/api/orders/v100-01/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].state").value("payment-pending"))
                .andExpect(jsonPath("$[1].state").value("payment-approved"));
    }

    @Test
    void rejectsBadSignatureAndStoresNothing() throws Exception {
        String body = hook("v200-01", "invoiced");

        send(body, signatureVerifier.sign(hook("v200-01", "canceled"))).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/orders/v200-01/events")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsSignedButMalformedPayload() throws Exception {
        String body = "{\"State\":\"invoiced\"}";

        send(body, signatureVerifier.sign(body)).andExpect(status().isBadRequest());
    }
}
