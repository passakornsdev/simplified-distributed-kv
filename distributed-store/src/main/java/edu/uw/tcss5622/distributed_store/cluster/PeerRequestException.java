package edu.uw.tcss5622.distributed_store.cluster;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;

@Getter
@RequiredArgsConstructor
public class PeerRequestException extends RuntimeException {

	private final HttpStatusCode statusCode;
	private final String responseBody;
}
