package org.example.customerservice.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BookingServiceHealthIndicator implements HealthIndicator {

    private final RestClient restClient;

    public BookingServiceHealthIndicator(
            RestClient.Builder restClientBuilder,
            @Value("${booking.service.url}") String bookingServiceUrl) {

        this.restClient = restClientBuilder
                .baseUrl(bookingServiceUrl)
                .build();
    }

    @Override
    public Health health() {
        try {
            restClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .toBodilessEntity();

            return Health.up().build();

        } catch (Exception e) {
            return Health.down().build();
        }
    }
}