package com.tmc.aac.shared.exception;

import com.tmc.aac.shared.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.collect(Collectors.joining("; "));
		if (message.isBlank()) {
			message = "Invalid request payload";
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(message));
	}

	@ExceptionHandler(InvalidQrException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidQr(InvalidQrException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(ex.getMessage()));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(ex.getMessage()));
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ex.getMessage()));
	}

	@ExceptionHandler(ExternalServiceException.class)
	public ResponseEntity<ApiResponse<Void>> handleExternalService(ExternalServiceException ex) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.fail(ex.getMessage()));
	}

	@ExceptionHandler(NotImplementedException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotImplemented(NotImplementedException ex) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(ApiResponse.fail(ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.fail(ex.getMessage() != null ? ex.getMessage() : "Unexpected error"));
	}
}
