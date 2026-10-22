package edu.uw.tcss5622.monolith_store.api;

import edu.uw.tcss5622.monolith_store.store.KeyNotFoundException;
import edu.uw.tcss5622.monolith_store.store.StoreValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(KeyNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleMissingKey(final KeyNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(StoreValidationException.class)
	public ResponseEntity<ErrorResponse> handleValidation(final StoreValidationException exception) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableRequest() {
		return error(HttpStatus.BAD_REQUEST, "Request body must contain a string value.");
	}

	private ResponseEntity<ErrorResponse> error(final HttpStatus status, final String message) {
		return ResponseEntity.status(status).body(new ErrorResponse(message));
	}
}
