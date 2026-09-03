package org.example.customerservice.customer.model.dto;

public record CustomerResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        String phone
) {
}