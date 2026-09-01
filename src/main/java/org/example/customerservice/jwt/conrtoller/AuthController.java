package org.example.customerservice.jwt.conrtoller;

import org.example.customerservice.customer.repository.CustomerRepository;
import org.example.customerservice.jwt.model.dto.LoginRequest;
import org.example.customerservice.jwt.service.JwtService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwt;
    private final CustomerRepository customerRepository;

    AuthController(JwtService j, CustomerRepository customerRepository) {
        this.jwt = j;
        this.customerRepository = customerRepository;
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest dto) {
      if(  customerRepository.existsByUsernameAndPassword(dto.username(), dto.password())){
            return jwt.generateToken(dto.username());
        }
        throw new RuntimeException("Fel inloggning");
    }
}