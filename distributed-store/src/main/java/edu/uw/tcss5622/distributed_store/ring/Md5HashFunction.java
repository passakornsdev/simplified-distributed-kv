package edu.uw.tcss5622.distributed_store.ring;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class Md5HashFunction implements HashFunction {

	@Override
	public long hash(final String value) {
		Objects.requireNonNull(value, "value must not be null.");
		try {
			final byte[] digest = MessageDigest.getInstance("MD5")
					.digest(value.getBytes(StandardCharsets.UTF_8));
			return ByteBuffer.wrap(digest).getLong();
		} catch (final NoSuchAlgorithmException exception) {
			throw new IllegalStateException("MD5 is not available.", exception);
		}
	}
}
