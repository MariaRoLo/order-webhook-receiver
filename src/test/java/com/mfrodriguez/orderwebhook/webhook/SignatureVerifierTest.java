package com.mfrodriguez.orderwebhook.webhook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignatureVerifierTest {

    private final SignatureVerifier verifier = new SignatureVerifier("dev-secret");

    @Test
    void signMatchesKnownHmacSha256() {
        // echo -n 'hello' | openssl dgst -sha256 -hmac dev-secret
        assertEquals("4481d0598468fd448fe1a51f0a7bf58163ddd3290290b1c9928960200373d3fe", verifier.sign("hello"));
    }

    @Test
    void acceptsOwnSignatureInAnyCase() {
        String signature = verifier.sign("{\"OrderId\":\"1\"}");
        assertTrue(verifier.isValid("{\"OrderId\":\"1\"}", signature));
        assertTrue(verifier.isValid("{\"OrderId\":\"1\"}", signature.toUpperCase()));
    }

    @Test
    void rejectsTamperedBodyWrongSecretAndMissingHeader() {
        String signature = verifier.sign("{\"OrderId\":\"1\"}");
        assertFalse(verifier.isValid("{\"OrderId\":\"2\"}", signature));
        assertFalse(new SignatureVerifier("other-secret").isValid("{\"OrderId\":\"1\"}", signature));
        assertFalse(verifier.isValid("{\"OrderId\":\"1\"}", null));
    }
}
