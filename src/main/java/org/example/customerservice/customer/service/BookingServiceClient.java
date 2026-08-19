package org.example.customerservice.customer.service;

import org.example.customerservice.error.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BookingServiceClient{

    private final String bookingServiceUrl = "http://localhost:8080/api";

    private final RestClient restClient;

    public BookingServiceClient (){
        this.restClient = RestClient.create();
    }

    public boolean hasActiveBookings(Long customerId){

        try{
            return Boolean.TRUE.equals(restClient
                    .get()
                    .uri(bookingServiceUrl +
                            "/bookings/active/" + customerId)
                    .retrieve()
                    .body(Boolean.class));
        }
        catch (Exception e){
            throw new BadRequestException("Vi kunde inte kontrollera bokningar"
                    + "just nu, försök igen senare");
        }
    }
}
