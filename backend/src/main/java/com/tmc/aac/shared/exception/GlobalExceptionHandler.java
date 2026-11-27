package com.tmc.aac.shared.exception;

import com.tmc.aac.shared.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

	/** Safety net for UNIQUE violations not translated by a repository adapter; never leak SQL text. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleIntegrity(DataIntegrityViolationException ex) {
		log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiResponse.fail("REPLAY_NONCE_DETECTED: QR token or transaction has already been used"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
		log.error("Unhandled error", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.fail("Unexpected error"));
	}
}
