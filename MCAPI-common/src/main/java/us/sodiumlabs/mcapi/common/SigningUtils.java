package us.sodiumlabs.mcapi.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static us.sodiumlabs.mcapi.common.ByteBufferUtils.copy;

public final class SigningUtils {
    private SigningUtils() {}

    protected static ByteBuffer hmacSHA256(final ByteBuffer key, final String message) {
        return hmacSHA256(key, ByteBuffer.wrap(message.getBytes()));
    }

    protected static ByteBuffer hmacSHA256(final ByteBuffer key, final ByteBuffer message) {
        try {
            final SecretKeySpec secretKeySpec = new SecretKeySpec(copy(key).array(), "HmacSHA256");
            final Mac sha256Hmac;
            sha256Hmac = Mac.getInstance("HmacSHA256");
            sha256Hmac.init(secretKeySpec);
            return ByteBuffer.wrap(sha256Hmac.doFinal(copy(message).array())).asReadOnlyBuffer();
        } catch (final NoSuchAlgorithmException e) {
            throw new SigningException("Does not support SHA256 HMAC", e);
        } catch (final InvalidKeyException e) {
            throw new SigningException("Invalid key", e);
        }
    }

    protected static ByteBuffer hashSHA256(final ByteBuffer toHash) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return ByteBuffer.wrap(digest.digest(copy(toHash).array())).asReadOnlyBuffer();
        } catch (NoSuchAlgorithmException e) {
            throw new HashingException("Unable to hash", e);
        }
    }

    public static class SigningException extends RuntimeException {
        public SigningException(final String msg, final Exception cause) {
            super(msg, cause);
        }
    }

    public static class HashingException extends RuntimeException {
        public HashingException(final String msg, final Exception cause) {
            super(msg, cause);
        }
    }
}
