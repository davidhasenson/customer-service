package org.example.customerservice.jwt.conrtoller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


//Testclass
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    public String returnUsername(Authentication authentication) {
        String username = authentication.getName();
        return "Hej " + username + " – du är inloggad!";
    }
}
