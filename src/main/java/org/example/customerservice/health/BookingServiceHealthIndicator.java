package org.example.customerservice.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.time.Duration;

@Component
public class BookingServiceHealthIndicator implements HealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(BookingServiceHealthIndicator.class);
    private static final String SERVICE_NAME = "booking-service";
    private static final String HEALTH_PATH = "/actuator/health";
    private final RestClient restClient;

    public BookingServiceHealthIndicator(
            RestClient.Builder restClientBuilder,
            @Value("${booking.service.url}") String bookingServiceUrl) {

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = restClientBuilder
                .baseUrl(bookingServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Health health() {
        try {
            restClient.get()
                    .uri(HEALTH_PATH)
                    .retrieve()
                    .toBodilessEntity();

            return Health.up()
                    .withDetail("service", SERVICE_NAME)
                    .build();

        } catch (Exception e) {
            log.warn("Health check against {} failed", SERVICE_NAME, e);
            return Health.down()
                    .withDetail("service", SERVICE_NAME)
                    .withDetail("reason", e.getClass().getSimpleName())
                    .build();
        }
    }
}