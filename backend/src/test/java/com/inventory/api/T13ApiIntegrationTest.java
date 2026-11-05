package com.inventory.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class T13ApiIntegrationTest {
    private static final Pattern STOCK_OF_EIGHT = Pattern.compile("\\\"currentStock\\\"\\s*:\\s*8(?:\\.0+)?");

    @LocalServerPort
    private int port;

    @Autowired
    private Environment environment;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void returnsProblemDetailsForUnauthenticatedInvalidAndMissingResourceRequests() throws Exception {
        HttpResponse<String> unauthenticated = send("GET", "/api/v1/products", null, false);
        assertEquals(401, unauthenticated.statusCode());
        assertTrue(unauthenticated.headers().firstValue("Content-Type").orElse("")
                .startsWith("application/problem+json"));

        HttpResponse<String> invalid = send("POST", "/api/v1/products", "{}", true);
        assertEquals(400, invalid.statusCode());
        assertTrue(invalid.body().contains("\"status\":400"));

        HttpResponse<String> missing = send("GET", "/api/v1/products/" + UUID.randomUUID(), null, true);
        assertEquals(404, missing.statusCode());
        assertTrue(missing.body().contains("\"status\":404"));
    }

    @Test
    void createsProductsWithJacksonThreePriceSerializationAndRejectsDuplicateSku() throws Exception {
        String sku = uniqueSku();
        HttpResponse<String> created = send("POST", "/api/v1/products", productJson(sku), true);

        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"unitPrice\":\"12.50\""));

        HttpResponse<String> duplicate = send("POST", "/api/v1/products", productJson(sku.toLowerCase()), true);
        assertEquals(409, duplicate.statusCode());
        assertTrue(duplicate.body().contains("\"status\":409"));
    }

    @Test
    void listsCategoriesInNameOrder() throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/categories", null, true);

        assertEquals(200, response.statusCode());
        assertEquals("[\"Cleaning & Janitorial\",\"Consumables\",\"Electronics\",\"Furniture\","
            + "\"Office & Stationery\",\"Other\",\"Packaging & Shipping\",\"Tools & Equipment\"]",
            response.body());
    }

    @Test
    void deactivationKeepsMovementHistoryAndRejectsFurtherStockChanges() throws Exception {
        UUID productId = createProduct();
        String movementPath = "/api/v1/products/" + productId + "/movements";

        HttpResponse<String> movement = send("POST", movementPath,
                movementJson("IN", 4, "Opening stock", 0), true);
        assertEquals(200, movement.statusCode());
        assertTrue(movement.body().contains("\"actorUsername\":\""
                + environment.getProperty("INVENTORY_MANAGER_USERNAME") + "\""));
        assertTrue(movement.body().contains("\"occurredAt\":"));

        HttpResponse<String> deactivated = send("PATCH",
                "/api/v1/products/" + productId + "/deactivation", null, true);
        assertEquals(200, deactivated.statusCode());
        assertTrue(deactivated.body().contains("\"active\":false"));

        HttpResponse<String> inactiveProduct = send("GET", "/api/v1/products/" + productId, null, true);
        assertEquals(404, inactiveProduct.statusCode());

        HttpResponse<String> inactiveDetail = send("GET", "/api/v1/products/inactive/" + productId, null, true);
        assertEquals(200, inactiveDetail.statusCode());
        assertTrue(inactiveDetail.body().contains("\"active\":false"));

        HttpResponse<String> inactiveList = send("GET", "/api/v1/products/inactive", null, true);
        assertEquals(200, inactiveList.statusCode());
        assertTrue(inactiveList.body().contains(productId.toString()));

        HttpResponse<String> history = send("GET", movementPath, null, true);
        assertEquals(200, history.statusCode());
        assertTrue(history.body().contains("Opening stock"));

        HttpResponse<String> rejectedMovement = send("POST", movementPath,
                movementJson("IN", 1, "After deactivation", 1), true);
        assertEquals(409, rejectedMovement.statusCode());
    }

    @Test
    void concurrentRequestsWithSameVersionCommitOneMovementAndConflictTheOther() throws Exception {
        UUID productId = createProduct();
        String movementPath = "/api/v1/products/" + productId + "/movements";
        HttpResponse<String> openingStock = send("POST", movementPath,
                movementJson("IN", 10, "Initial stock", 0), true);
        assertEquals(200, openingStock.statusCode());

        CompletableFuture<HttpResponse<String>> first = sendAsync("POST", movementPath,
                movementJson("OUT", 2, "Concurrent issue", 1));
        CompletableFuture<HttpResponse<String>> second = sendAsync("POST", movementPath,
                movementJson("OUT", 2, "Concurrent issue", 1));
        List<Integer> statuses = List.of(first.join().statusCode(), second.join().statusCode());

        assertEquals(1, statuses.stream().filter(status -> status == 200).count());
        assertEquals(1, statuses.stream().filter(status -> status == 409).count());

        HttpResponse<String> currentProduct = send("GET", "/api/v1/products/" + productId, null, true);
        assertTrue(STOCK_OF_EIGHT.matcher(currentProduct.body()).find(), currentProduct.body());

        HttpResponse<String> history = send("GET", movementPath, null, true);
        assertEquals(2, occurrences(history.body(), "\"productId\":"));
    }

    private UUID createProduct() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/products", productJson(uniqueSku()), true);
        assertEquals(201, response.statusCode());
        URI location = URI.create(response.headers().firstValue("Location").orElseThrow());
        String path = location.getPath();
        return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
    }

    private HttpResponse<String> send(String method, String path, String body, boolean authenticated)
            throws Exception {
        return httpClient.send(request(method, path, body, authenticated), HttpResponse.BodyHandlers.ofString());
    }

    private CompletableFuture<HttpResponse<String>> sendAsync(String method, String path, String body) {
        return httpClient.sendAsync(request(method, path, body, true), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest request(String method, String path, String body, boolean authenticated) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json");
        if (authenticated) {
            String username = environment.getProperty("INVENTORY_MANAGER_USERNAME");
            String password = environment.getProperty("INVENTORY_MANAGER_PASSWORD");
            String credentials = Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + credentials);
        }
        return builder.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build();
    }

    private String productJson(String sku) {
        return "{\"name\":\"API test item\",\"sku\":\"" + sku
                + "\",\"category\":\"Electronics\",\"unitPrice\":12.50,"
                + "\"minimumThreshold\":2,\"description\":\"T13 integration test\"}";
    }

    private String movementJson(String type, int quantity, String reason, int expectedVersion) {
        return "{\"type\":\"" + type + "\",\"quantity\":" + quantity
                + ",\"reasonReference\":\"" + reason + "\",\"expectedVersion\":" + expectedVersion + "}";
    }

    private String uniqueSku() {
        return "T13-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private int occurrences(String value, String target) {
        return (value.length() - value.replace(target, "").length()) / target.length();
    }
}