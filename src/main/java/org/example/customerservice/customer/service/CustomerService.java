package org.example.customerservice.customer.service;

import jakarta.transaction.Transactional;
import org.example.customerservice.customer.model.dto.CreateCustomerRequest;
import org.example.customerservice.customer.model.Customer;
import org.example.customerservice.customer.model.dto.CustomerResponse;
import org.example.customerservice.customer.model.dto.UpdateCustomerRequest;
import org.example.customerservice.customer.repository.CustomerRepository;
import org.example.customerservice.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    RestTemplate restTemplate = new RestTemplate();

    @Value("${booking.service.url}")
    private String bookingServiceUrl;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerResponse> getAllCustomers() {
        logger.info("Fetching all customers from the database");
        List<Customer> customers = customerRepository.findAll();
        logger.info("Successfully retrieved {} customers", customers.size());
        List<CustomerResponse> responseList = new ArrayList<>();
        for (Customer customer : customers) {
            CustomerResponse response = convertToCustomerResponse(customer);
            responseList.add(response);
        }
        return responseList;
    }

    public CustomerResponse getCustomerById(Long id) {
        logger.info("Fetching customer with ID: {}", id);
        Customer customer = customerRepository.findById(id).orElseThrow(
                () -> {
                    logger.warn("Fetch failed: Customer with ID {} not found", id);
                    return new NotFoundException("Kunden hittades inte");
                }
        );
        return convertToCustomerResponse(customer);
    }

    public CustomerResponse getCustomerByEmail(String email) {
        logger.info("Fetching customer with email: {}", email);
        Customer customer = customerRepository.findByEmail(email).orElseThrow(
                () -> {
                    logger.warn("Fetch failed: Customer with email {} not found", email);
                    return new NotFoundException("Kunden hittades inte");
                }
        );
        return convertToCustomerResponse(customer);
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        logger.info("Attempting to create a new customer with email: {}", request.email());
        boolean emailExists = customerRepository.findByEmail(request.email()).isPresent();

        if (emailExists) {
            logger.warn("Customer creation failed: Email {} is already registered", request.email());
            throw new IllegalStateException("E-postadressen är redan registrerad!");
        }

        Customer customer = new Customer(request.firstName(), request.lastName(), request.email(), request.phone());
        Customer savedCustomer = customerRepository.save(customer);
        logger.info("Customer successfully created with ID: {}", savedCustomer.getId());
        return convertToCustomerResponse(savedCustomer);
    }

    @Transactional
    public CustomerResponse updateCustomerById(Long id, UpdateCustomerRequest request) {
        logger.info("Attempting to update customer with ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Update failed: Customer with ID {} not found", id);
                    return new NotFoundException("Kunden hittades inte");
                });

        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setPhone(request.phone());

        Customer updatedCustomer = customerRepository.save(customer);
        logger.info("Customer details successfully updated for ID: {}", id);
        return convertToCustomerResponse(updatedCustomer);
    }

    @Transactional
    public CustomerResponse updateCustomerByEmail(String email, UpdateCustomerRequest request) {
        logger.info("Attempting to update customer details for email: {}", email);

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Update failed: No customer found with email: {}", email);
                    return new NotFoundException("Ingen kund hittades med e-postadressen: " + email);
                });

        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setPhone(request.phone());

        Customer updatedCustomer = customerRepository.save(customer);

        logger.info("Customer details successfully updated for customer ID: {} (Email: {})", updatedCustomer.getId(), email);
        return convertToCustomerResponse(updatedCustomer);
    }

    @Transactional
    public void deleteCustomerById(Long id) {
        logger.info("Attempting to delete customer with ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Delete failed: Customer with ID {} not found", id);
                    return new NotFoundException("Kunden hittades inte");
                });

        checkActiveBookings(id);

        unlinkPastBookings(id);

        customerRepository.delete(customer);
        logger.info("Customer with ID {} was successfully deleted", id);
    }

    private void checkActiveBookings(Long customerId) {
        try {
            Boolean hasActiveBooking = restTemplate.getForObject(
                    bookingServiceUrl + "/api/bookings/active-bookings/" + customerId,
                    Boolean.class
            );

            if (Boolean.TRUE.equals(hasActiveBooking)) {
                logger.warn("Delete failed: Customer with ID {} has active bookings", customerId);
                throw new IllegalStateException("Kunden har aktiva bokningar som måste avbokas innan den kan tas bort");
            }
        } catch (RestClientException e) {
            logger.error("Error communicating with Booking service while checking active bookings for ID {}", customerId, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kunde inte kommunicera med bokningstjänsten");
        }
    }

    private void unlinkPastBookings(Long customerId) {
        try {
            restTemplate.exchange(
                    bookingServiceUrl + "/api/bookings/unlink-bookings/" + customerId,
                    HttpMethod.PUT,
                    HttpEntity.EMPTY,
                    Void.class
            );
        } catch (RestClientException e) {
            logger.error("Error communicating with Booking service while unlinking bookings for ID {}", customerId, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kunde inte avkoppla bokningar hos bokningstjänsten");
        }
    }

//    @Transactional
//    public void deleteCustomerByEmail(String email) {
//        logger.info("Attempting to delete customer with email: {}", email);
//
//        Customer customer = customerRepository.findByEmail(email)
//                .orElseThrow(() -> {
//                    logger.warn("Delete failed: Customer with email {} not found", email);
//                    return new NotFoundException("Kunden hittades inte");
//                });
//
//        Long customerId = customer.getId();
//
//
//        boolean hasActiveBooking = bookingRepository.existsByCustomerIdAndEndDateAfterAndStatus(customerId, LocalDate.now(), BookingStatus.ACTIVE);
//        if (hasActiveBooking) {
//            logger.warn("Delete failed: Customer with email {} (ID {}) has active bookings", email, customerId);
//            throw new IllegalStateException("Kunden har aktiva bokningar som måste avbokas innan den kan tas bort");
//        }
//
//        logger.info("Unlinking past bookings for customer email: {} (ID {})", email, customerId);
//        for (Booking booking : bookingRepository.findByCustomerId(customerId)) {
//            booking.setCustomer(null);
//            bookingRepository.save(booking);
//        }
//
//        customerRepository.delete(customer);
//        logger.info("Customer with email {} was successfully deleted", email);
//    }

    private CustomerResponse convertToCustomerResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone()
        );
    }
}