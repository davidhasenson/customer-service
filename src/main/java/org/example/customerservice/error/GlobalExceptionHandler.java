package org.example.customerservice.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@CrossOrigin(origins = "*")
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, Object> responseBody = createBaseResponse(HttpStatus.BAD_REQUEST, request);

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        responseBody.put("message", "Valideringen misslyckades");
        responseBody.put("errors", validationErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {

        Map<String, Object> responseBody = createBaseResponse(HttpStatus.BAD_REQUEST, request);
        responseBody.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFoundException(
            NotFoundException ex, HttpServletRequest request) {

        Map<String, Object> responseBody = createBaseResponse(HttpStatus.NOT_FOUND, request);
        responseBody.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseBody);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {

        Map<String, Object> responseBody = createBaseResponse(HttpStatus.BAD_REQUEST, request);
        responseBody.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {

        logger.error("Databasfel vid {} {}", request.getMethod(), request.getRequestURI(), ex);

        Map<String, Object> responseBody = createBaseResponse(HttpStatus.SERVICE_UNAVAILABLE, request);
        responseBody.put("message", "Kan inte nå databasen just nu. Försök igen om en liten stund.");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(responseBody);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {

        logger.error("Fel vid kommunikation med annan tjänst vid {} {}", request.getMethod(), request.getRequestURI(), ex);

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        Map<String, Object> responseBody = createBaseResponse(status, request);
        responseBody.put("message", ex.getReason());

        return ResponseEntity.status(status).body(responseBody);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbiddenException(
            ForbiddenException ex, HttpServletRequest request) {

        Map<String, Object> responseBody = createBaseResponse(HttpStatus.FORBIDDEN, request);
        responseBody.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(responseBody);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(
            Exception ex, HttpServletRequest request) {
        logger.error("Ett oväntat fel inträffade vid {} {}", request.getMethod(), request.getRequestURI(), ex);

        Map<String, Object> responseBody = createBaseResponse(HttpStatus.INTERNAL_SERVER_ERROR, request);
        responseBody.put("message", "Ett oväntat fel inträffade. Försök igen senare.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
    }

    private Map<String, Object> createBaseResponse(HttpStatus status, HttpServletRequest request) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("timestamp", Instant.now().toString());
        responseBody.put("status", status.value());
        responseBody.put("error", status.getReasonPhrase());
        responseBody.put("path", request.getRequestURI());
        return responseBody;
    }
}