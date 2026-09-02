package org.example.customerservice.jwt.conrtoller;

import org.example.customerservice.customer.repository.CustomerRepository;
import org.example.customerservice.jwt.model.dto.LoginRequest;
import org.example.customerservice.jwt.service.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwt;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    AuthController(JwtService j, CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.jwt = j;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest dto) {
        var customer = customerRepository.findByUsername(dto.username())
                .orElseThrow(() -> new RuntimeException("Fel inloggning"));

        if (passwordEncoder.matches(dto.password(), customer.getPassword())) {
            return jwt.generateToken(dto.username());
        }
        throw new RuntimeException("Fel inloggning");
    }
}