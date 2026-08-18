package org.example.customerservice.customer.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
        @NotBlank(message = "Förnamn måste anges")
        String firstName,

        @NotBlank(message = "Efternamn måste anges")
        String lastName,

        @NotBlank(message = "E-post måste anges")
        @Email(message = "E-post måste vara giltig")
        String email,
        String phone) {
}