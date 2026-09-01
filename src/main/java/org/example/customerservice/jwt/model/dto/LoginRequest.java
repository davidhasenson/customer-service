package org.example.customerservice.jwt.model.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Användarnamn måste anges")
        String username,

        @NotBlank (message = "Lösenord måste anges")
        String password

) {
}
