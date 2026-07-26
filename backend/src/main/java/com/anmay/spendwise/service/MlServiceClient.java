package com.anmay.spendwise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MlServiceClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public MlServiceClient(ObjectMapper objectMapper,
                           @Value("${app.ml-service-url}") String baseUrl) {
        this.objectMapper = objectMapper;
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://" + normalized;
        }
        this.baseUrl = normalized.replaceAll("/$", "");
    }

    public Prediction predict(Long userId,
                              String merchant,
                              String description,
                              double amount,
                              List<String> availableCategories) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "user_id", userId,
                    "merchant", merchant,
                    "description", description == null ? "" : description,
                    "amount", amount,
                    "available_categories", availableCategories
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/predict-category"))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                return fallback(merchant, description);
            }

            JsonNode node = objectMapper.readTree(response.body());
            return new Prediction(
                    node.path("category").asText("Other"),
                    node.path("confidence").asDouble(0.55),
                    node.path("model").asText("ml-service")
            );
        } catch (Exception ignored) {
            return fallback(merchant, description);
        }
    }

    public void sendFeedback(Long userId,
                             String merchant,
                             String description,
                             String correctedCategory) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "user_id", userId,
                    "merchant", merchant,
                    "description", description == null ? "" : description,
                    "corrected_category", correctedCategory
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/feedback"))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // The simulated payment must still succeed when ML feedback is offline.
        }
    }

    private Prediction fallback(String merchant, String description) {
        String text = (merchant + " " + (description == null ? "" : description))
                .toLowerCase(Locale.ROOT);

        if (containsAny(text,
                "swiggy", "zomato", "restaurant", "cafe", "grocery", "food",
                "pizza", "pizza hut", "dominos", "burger", "kfc", "mcdonald",
                "subway", "starbucks", "bakery", "dinner", "lunch")) {
            return new Prediction("Food", 0.88, "backend-fallback");
        }

        if (containsAny(text,
                "pvr", "netflix", "spotify", "movie", "game", "bookmyshow",
                "concert", "cinema")) {
            return new Prediction("Entertainment", 0.84, "backend-fallback");
        }

        if (containsAny(text,
                "uber", "ola", "metro", "flight", "train", "fuel", "petrol",
                "taxi", "railway", "bus")) {
            return new Prediction("Travel", 0.84, "backend-fallback");
        }

        if (containsAny(text,
                "amazon", "myntra", "flipkart", "mall", "clothes", "fashion",
                "electronics", "shoes")) {
            return new Prediction("Shopping", 0.82, "backend-fallback");
        }

        if (containsAny(text,
                "electricity", "recharge", "internet", "rent", "bill", "airtel",
                "jio", "broadband")) {
            return new Prediction("Bills", 0.84, "backend-fallback");
        }

        if (containsAny(text,
                "pharmacy", "hospital", "doctor", "medicine", "diagnostic",
                "dentist")) {
            return new Prediction("Healthcare", 0.84, "backend-fallback");
        }

        if (containsAny(text,
                "udemy", "coursera", "college", "course", "tuition", "textbook")) {
            return new Prediction("Education", 0.84, "backend-fallback");
        }

        if (containsAny(text,
                "mutual fund", "sip", "stock", "zerodha", "groww", "investment",
                "fixed deposit", "nps")) {
            return new Prediction("Investment", 0.84, "backend-fallback");
        }

        if (containsAny(text, "gift", "birthday present", "bouquet")) {
            return new Prediction("Gifts", 0.82, "backend-fallback");
        }

        if (containsAny(text, "parents", "family support", "sibling")) {
            return new Prediction("Family", 0.82, "backend-fallback");
        }

        return new Prediction("Other", 0.45, "backend-fallback");
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    public record Prediction(
            String category,
            double confidence,
            String model
    ) {
    }
}
