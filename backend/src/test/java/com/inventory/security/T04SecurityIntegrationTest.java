package com.inventory.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.AccessDeniedHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class T04SecurityIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private Environment environment;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessDeniedHandler accessDeniedHandler;

    @Test
    void requiresAuthenticationForEveryRequestAndReturnsSafeProblemDetails() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .startsWith("application/problem+json"));
        assertTrue(response.headers().firstValue("WWW-Authenticate").orElse("")
                .startsWith("Basic realm=\"Inventory\""));
        assertTrue(response.body().contains("\"status\":401"));
        assertTrue(response.body().contains("Authentication is required."));
        assertFalse(response.body().contains(environment.getProperty("INVENTORY_MANAGER_USERNAME")));
        assertFalse(response.body().contains(environment.getProperty("INVENTORY_MANAGER_PASSWORD")));
        assertFalse(response.body().toLowerCase().contains("stacktrace"));
    }

    @Test
    void acceptsOnlyConfiguredManagerCredentialsAndKeepsPasswordHashedInMemory() throws Exception {
        String username = environment.getProperty("INVENTORY_MANAGER_USERNAME");
        String password = environment.getProperty("INVENTORY_MANAGER_PASSWORD");
        UserDetails manager = userDetailsService.loadUserByUsername(username);
        String basic = Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                        .header("Authorization", "Basic " + basic)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertEquals(1, manager.getAuthorities().size());
        assertTrue(manager.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_INVENTORY_MANAGER")));
        assertNotEquals(password, manager.getPassword());
        assertTrue(passwordEncoder.matches(password, manager.getPassword()));
    }

    @Test
    void rejectsMissingOrBlankEnvironmentCredentialsWithoutEchoingValues() {
        IllegalStateException missingException = assertThrows(IllegalStateException.class,
                () -> SecurityConfiguration.requiredCredential(Map.of(), "INVENTORY_MANAGER_USERNAME"));
        IllegalStateException blankException = assertThrows(IllegalStateException.class,
                () -> SecurityConfiguration.requiredCredential(
                        Map.of("INVENTORY_MANAGER_PASSWORD", "   "), "INVENTORY_MANAGER_PASSWORD"));

        assertTrue(missingException.getMessage().contains("INVENTORY_MANAGER_USERNAME"));
        assertTrue(blankException.getMessage().contains("INVENTORY_MANAGER_PASSWORD"));
        assertFalse(blankException.getMessage().contains("manager"));
    }

    @Test
    void requiresHttpsWhenBindingBeyondLoopback() {
        Environment networkHttp = new MockEnvironment().withProperty("server.address", "0.0.0.0");
        Environment networkHttps = new MockEnvironment()
                .withProperty("server.address", "0.0.0.0")
                .withProperty("server.ssl.enabled", "true");

        assertThrows(IllegalStateException.class,
                () -> SecurityConfiguration.validateTransportBoundary(networkHttp));
        SecurityConfiguration.validateTransportBoundary(networkHttps);
    }

    @Test
    void returnsSafeProblemDetailsForForbiddenRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/restricted");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("private detail"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertTrue(response.getContentAsString().contains("\"status\":403"));
        assertTrue(response.getContentAsString().contains("Access is denied."));
        assertFalse(response.getContentAsString().contains("private detail"));
    }
}