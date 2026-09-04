package org.example.customerservice.customer.controller;

import jakarta.validation.Valid;

import org.example.customerservice.customer.model.dto.CreateCustomerRequest;
import org.example.customerservice.customer.model.dto.CustomerResponse;
import org.example.customerservice.customer.model.dto.UpdateCustomerRequest;
import org.example.customerservice.customer.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        logger.info("Received HTTP GET request to fetch all customers.");
        List<CustomerResponse> customers = customerService.getAllCustomers();
        return ResponseEntity.ok().body(customers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable long id, Authentication authentication) {
        logger.info("Received HTTP GET request to fetch response with ID: {}", id);
        CustomerResponse response = customerService.getCustomerById(id, authentication.getName());
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/by-email")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(@RequestParam String email, Authentication authentication) {
        logger.info("Received HTTP GET request to fetch customer by email: {}", email);
        CustomerResponse response = customerService.getCustomerByEmail(email, authentication.getName());
        return ResponseEntity.ok().body(response);
    }

    @PutMapping("/email/{email}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable String email, @Valid @RequestBody UpdateCustomerRequest request, Authentication authentication) {
        logger.info("Received PUT request to /api/customers/email/{} to update customer", email);
        CustomerResponse updatedCustomer = customerService.updateCustomerByEmail(email, request, authentication.getName());
        return ResponseEntity.ok(updatedCustomer);
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        logger.info("Received HTTP POST request to create customer with email: {}", request.email());
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest request, Authentication authentication) {
        logger.info("Received HTTP PUT request to update customer with ID: {}", id);
        CustomerResponse updated = customerService.updateCustomerById(id, request, authentication.getName());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomerById(@PathVariable Long id, Authentication authentication) {
        logger.info("Received HTTP DELETE request to delete customer with ID: {}", id);
        customerService.deleteCustomerById(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/email/{email}")
    public ResponseEntity<?> deleteCustomerByEmail(@PathVariable String email, Authentication authentication) {
        logger.info("Received HTTP DELETE request to delete customer with email: {}", email);
        customerService.deleteCustomerByEmail(email, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}