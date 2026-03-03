package com.vulscan.dashboard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler(java.lang.Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(java.lang.Exception e) {
        logger.severe("Exception caught: " + e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", e.getMessage());
        response.put("type", e.getClass().getSimpleName());
        
        if (e instanceof MaxUploadSizeExceededException) {
            response.put("message", "File is too large");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (e instanceof IllegalArgumentException) {
            response.put("error", "Bad Request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (e instanceof BadCredentialsException) {
            response.put("error", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (e instanceof AccessDeniedException) {
            response.put("error", "Forbidden");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        if (e instanceof AuthenticationException) {
            response.put("error", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
