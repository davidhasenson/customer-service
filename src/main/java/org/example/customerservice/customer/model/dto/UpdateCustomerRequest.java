package org.example.customerservice.customer.model.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCustomerRequest(
        @NotBlank(message = "Förnamn måste anges")
        String firstName,

        @NotBlank(message = "Efternamn måste anges")
        String lastName,
        String phone
) {
}