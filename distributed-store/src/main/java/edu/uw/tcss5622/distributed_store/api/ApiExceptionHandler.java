package edu.uw.tcss5622.distributed_store.api;

import edu.uw.tcss5622.distributed_store.cluster.PeerConfigurationException;
import edu.uw.tcss5622.distributed_store.cluster.PeerRequestException;
import edu.uw.tcss5622.distributed_store.cluster.PeerUnavailableException;
import edu.uw.tcss5622.distributed_store.replication.QuorumUnavailableException;
import edu.uw.tcss5622.distributed_store.store.KeyNotFoundException;
import edu.uw.tcss5622.distributed_store.store.StoreValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

	@ExceptionHandler(KeyNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleMissingKey(final KeyNotFoundException exception) {
		log.debug("Key was not found.", exception);
		return error(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(StoreValidationException.class)
	public ResponseEntity<ErrorResponse> handleValidation(final StoreValidationException exception) {
		log.debug("Store validation failed.", exception);
		return error(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableRequest(final HttpMessageNotReadableException exception) {
		log.debug("Request body could not be read.", exception);
		return error(HttpStatus.BAD_REQUEST, "Request body must contain a string value.");
	}

	@ExceptionHandler(PeerRequestException.class)
	public ResponseEntity<String> handlePeerResponse(final PeerRequestException exception) {
		log.debug("Peer returned an error response.", exception);
		return ResponseEntity.status(exception.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(exception.getResponseBody());
	}

	@ExceptionHandler(PeerUnavailableException.class)
	public ResponseEntity<ErrorResponse> handleUnavailablePeer(final PeerUnavailableException exception) {
		log.debug("Peer is unavailable.", exception);
		return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
	}

	@ExceptionHandler(QuorumUnavailableException.class)
	public ResponseEntity<ErrorResponse> handleUnavailableQuorum(final QuorumUnavailableException exception) {
		log.debug("Quorum is unavailable.", exception);
		return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
	}

	@ExceptionHandler(PeerConfigurationException.class)
	public ResponseEntity<ErrorResponse> handlePeerConfiguration(final PeerConfigurationException exception) {
		log.debug("Peer configuration is invalid.", exception);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
	}

	private ResponseEntity<ErrorResponse> error(final HttpStatus status, final String message) {
		return ResponseEntity.status(status).body(new ErrorResponse(message));
	}
}
