package org.example.customerservice.customer.repository;

import org.example.customerservice.customer.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByUsername(String username);
    boolean existsByUsername(String username);
}