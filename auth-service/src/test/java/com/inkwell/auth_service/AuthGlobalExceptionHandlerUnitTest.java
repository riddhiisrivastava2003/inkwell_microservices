package com.inkwell.auth_service;

import com.inkwell.auth_service.exception.BadRequestException;
import com.inkwell.auth_service.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthGlobalExceptionHandlerUnitTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBadRequestShouldReturn400() {
        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(new BadRequestException("Invalid input"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input", response.getBody().get("error"));
    }

    @Test
    void handleGenericShouldReturn500() {
        ResponseEntity<Map<String, String>> response = handler.handleGeneric(new RuntimeException("Unexpected"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unexpected", response.getBody().get("error"));
    }
}

