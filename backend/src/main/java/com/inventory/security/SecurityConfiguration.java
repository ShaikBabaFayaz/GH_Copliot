package com.inventory.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(Environment environment, PasswordEncoder passwordEncoder) {
        validateTransportBoundary(environment);
        Map<String, String> processEnvironment = System.getenv();
        String username = requiredCredential(processEnvironment, "INVENTORY_MANAGER_USERNAME");
        String password = requiredCredential(processEnvironment, "INVENTORY_MANAGER_PASSWORD");
        UserDetails inventoryManager = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("INVENTORY_MANAGER")
                .build();
        return new InMemoryUserDetailsManager(inventoryManager);
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Inventory\"");
            writeProblem(response, HttpStatus.UNAUTHORIZED, "Authentication is required.");
        };
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> writeProblem(response,
                HttpStatus.FORBIDDEN, "Access is denied.");
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("INVENTORY_MANAGER"))
                .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(errors -> errors.accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    static String requiredCredential(Map<String, String> environment, String variableName) {
        String value = environment.get(variableName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variableName + " must be set to a non-blank value.");
        }
        return value;
    }

    static void validateTransportBoundary(Environment environment) {
        String address = environment.getProperty("server.address", "127.0.0.1");
        boolean sslEnabled = environment.getProperty("server.ssl.enabled", Boolean.class, false);
        try {
            InetAddress bindAddress = InetAddress.getByName(address);
            if (!bindAddress.isLoopbackAddress() && !sslEnabled) {
                throw new IllegalStateException("HTTPS must be enabled when binding beyond loopback.");
            }
        } catch (UnknownHostException exception) {
            throw new IllegalStateException("server.address must resolve to a valid bind address.", exception);
        }
    }

        private static void writeProblem(HttpServletResponse response, HttpStatus status, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String title = status == HttpStatus.UNAUTHORIZED ? "Unauthorized" : "Forbidden";
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title
                + "\",\"status\":" + status.value() + ",\"detail\":\"" + detail + "\"}");
    }
}