package us.sodiumlabs.mcapi.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Signing {
    public ByteBuffer canonical(final ByteBuffer nonce, final LocalDateTime dateTime) {
        final Instant utcInstant = dateTime.toInstant(ZoneOffset.UTC);

        return ByteBuffer.wrap(("LOGIN\n" +
            "date:" + utcInstant.toString() + "\n" +
            "nonce:" + toHex(nonce)).getBytes()).asReadOnlyBuffer();
    }

    public ByteBuffer sign(final String secret, final ByteBuffer canonical, final LocalDateTime dateTime) {
        final Instant instant = dateTime.toInstant(ZoneOffset.UTC);

        // Construct string to sign.
        final ByteBuffer toSign = ByteBuffer.wrap(("MCAPI-HMAC-SHA256\n" +
            instant.toString() + "\n" +
            toHex(hashSHA256(canonical))).getBytes());

        // HMAC that shit
        return hmacSHA256(hmacSHA256(secret, instant.toString()), toSign);
    }


    protected final ByteBuffer hmacSHA256(final String key, final String message) {
        return hmacSHA256(ByteBuffer.wrap(key.getBytes()), ByteBuffer.wrap(message.getBytes()));
    }

    protected final ByteBuffer hmacSHA256(final ByteBuffer key, final ByteBuffer message) {
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

    protected final ByteBuffer hashSHA256(final ByteBuffer toHash) {
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

    public final String toHex(final ByteBuffer bb) {
        final ByteBuffer newBb = bb.asReadOnlyBuffer();
        newBb.rewind();

        final StringBuilder hexString = new StringBuilder();
        while(newBb.hasRemaining()) {
            String hex = Integer.toHexString(0xff & newBb.get());
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public final ByteBuffer copy(final ByteBuffer bb) {
        final ByteBuffer newBb = bb.asReadOnlyBuffer();
        newBb.rewind();

        final ByteBuffer copy = ByteBuffer.allocate(newBb.capacity());
        copy.put(newBb);
        copy.rewind();
        return copy;
    }
}
