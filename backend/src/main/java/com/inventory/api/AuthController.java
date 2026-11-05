package com.inventory.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @GetMapping("/me")
    public AuthenticatedUserResponse currentUser(Authentication authentication) {
        return new AuthenticatedUserResponse(authentication.getName(), "INVENTORY_MANAGER");
    }

    public record AuthenticatedUserResponse(String username, String role) {
    }
}