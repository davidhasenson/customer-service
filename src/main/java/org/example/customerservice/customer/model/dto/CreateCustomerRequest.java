package org.example.customerservice.customer.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank(message = "Förnamn måste anges")
        String firstName,

        @NotBlank(message = "Efternamn måste anges")
        String lastName,

        @NotBlank(message = "E-post måste anges")
        @Email(message = "E-post måste vara giltig")
        String email,

        String phone,

        @NotBlank(message = "Användarnamn får inte vara tomt")
        String username,

        @NotBlank(message = "Lösenord får inte vara tomt")
        @Size(min = 8, message = "Lösenordet måste vara minst 8 tecken")
        String password

) {
}