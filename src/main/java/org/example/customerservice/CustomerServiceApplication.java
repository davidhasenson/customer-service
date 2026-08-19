package org.example.customerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CustomerServiceApplication {

    public static void main(String[] args) {
//      System.out.println("Söker efter .env i mappen: " + System.getProperty("user.dir"));

        SpringApplication.run(CustomerServiceApplication.class, args);
    }

}
