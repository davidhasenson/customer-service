package org.example.customerservice.customer.seeder;


import org.example.customerservice.customer.model.Customer;
import org.example.customerservice.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value = 2)
public class CustomerSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CustomerSeeder.class);

    private CustomerRepository customerRepository;

    public CustomerSeeder(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Checking if database needs to be seeded with customers...");
        if (customerRepository.count() == 0) {
            logger.info("Database is empty. Seeding 10 test customers from Middle-earth...");

            customerRepository.save(new Customer("Frodo", "Baggins", "frodo@theshire.me", "0761112233", "frodo", "password123"));
            customerRepository.save(new Customer("Samwise", "Gamgee", "sam@theshire.me", "0762223344", "samwise", "password123"));
            customerRepository.save(new Customer("Gandalf", "the Grey", "gandalf@istari.org", "0700000000", "gandalf", "password123"));
            customerRepository.save(new Customer("Aragorn", "Elessar", "strider@gondor.gov", "0721118888", "aragorn", "password123"));
            customerRepository.save(new Customer("Legolas", "Greenleaf", "legolas@mirkwood.net", "0734445555", "legolas", "password123"));
            customerRepository.save(new Customer("Gimli", "Son of Glóin", "gimli@erebor.min", "0703333333", "gimli", "password123"));
            customerRepository.save(new Customer("Boromir", "of Gondor", "boromir@gondor.gov", "0722229999", "boromir", "password123"));
            customerRepository.save(new Customer("Smeagol", "Gollum", "my.precious@cave.com", "0706666666", "gollum", "password123"));
            customerRepository.save(new Customer("Lady", "Galadriel", "galadriel@lothlorien.com", "0737777777", "galadriel", "password123"));
            customerRepository.save(new Customer("Lord", "Elrond", "elrond@rivendell.com", "0738888888", "elrond", "password123"));

            logger.info("Customer database seeding completed successfully.");
        } else {
            logger.info("Database already contains customer data. Skipping seeding.");
        }
    }

}