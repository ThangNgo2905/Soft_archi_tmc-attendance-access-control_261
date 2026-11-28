package com.tmc.aac.shared.exception;

import com.tmc.aac.shared.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> missingResource(Exception ex) {
        return ResponseEntity.status(404).body(ApiResponse.fail("NOT_FOUND"));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<ApiResponse<Void>> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail("INVALID_REQUEST"));
    }
    @ExceptionHandler(ScanConflict.class)
    ResponseEntity<ApiResponse<Void>> conflict(ScanConflict ex) {
        return ResponseEntity.status(409).body(ApiResponse.fail("SCAN_ID_CONFLICT"));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) {
        LOG.error("Edge request failed ({})", ex.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(ApiResponse.fail("LOCAL_PROCESSING_FAILED"));
    }
}
