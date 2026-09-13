package com.anmay.spendwise.payments;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RazorpayClientTest {
  String sign(String body, String secret) throws Exception {
    var mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void signaturesBindOrderPaymentAndRawWebhookBody() throws Exception {
    var client =
        new RazorpayClient(
            new ObjectMapper(),
            "rzp_test_example",
            "private-test-secret",
            "separate-webhook-secret");
    String signature = sign("order_abc|pay_xyz", "private-test-secret");
    client.verifyCheckout("order_abc", "pay_xyz", signature);
    assertThrows(
        ResponseStatusException.class,
        () -> client.verifyCheckout("order_wrong", "pay_xyz", signature));
    assertThrows(
        ResponseStatusException.class,
        () -> client.verifyCheckout("order_abc", "pay_wrong", signature));
    String body = "{\"event\":\"payment.captured\"}";
    String webhook = sign(body, "separate-webhook-secret");
    client.verifyWebhook(body.getBytes(StandardCharsets.UTF_8), webhook);
    assertThrows(
        ResponseStatusException.class,
        () -> client.verifyWebhook((body + " ").getBytes(StandardCharsets.UTF_8), webhook));
    assertThrows(
        ResponseStatusException.class,
        () -> client.verifyWebhook(body.getBytes(StandardCharsets.UTF_8), signature));
    assertThrows(
        ResponseStatusException.class,
        () -> client.verifyWebhook(body.getBytes(StandardCharsets.UTF_8), null));
  }

  @Test
  void liveKeysAreRejectedAndMissingKeysDisableCheckout() {
    assertThrows(
        IllegalStateException.class,
        () -> new RazorpayClient(new ObjectMapper(), "rzp_live_example", "secret", "webhook"));
    var client = new RazorpayClient(new ObjectMapper(), "", "", "");
    assertFalse(client.configured());
    assertThrows(ResponseStatusException.class, client::requireConfigured);
  }
}
