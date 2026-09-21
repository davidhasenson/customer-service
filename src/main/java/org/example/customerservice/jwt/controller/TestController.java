package org.example.customerservice.jwt.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//Test class
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {


    @GetMapping("/hello")
    public String hello() {
        return "Hello from customer service";
    }

    @GetMapping
    public String returnUsername(Authentication authentication) {
        String username = authentication.getName();
        return "Hej " + username + " – du är inloggad!";
    }
}
