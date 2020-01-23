package us.sodiumlabs.mcapi.common;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static java.util.Objects.requireNonNull;
import static us.sodiumlabs.mcapi.common.BaseUtils.fromBase64;
import static us.sodiumlabs.mcapi.common.BaseUtils.toHex;
import static us.sodiumlabs.mcapi.common.ByteBufferUtils.copy;
import static us.sodiumlabs.mcapi.common.ByteBufferUtils.mergeByteBuffers;
import static us.sodiumlabs.mcapi.common.ByteBufferUtils.readFromBuffer;
import static us.sodiumlabs.mcapi.common.ByteBufferUtils.secureCompareBuffers;
import static us.sodiumlabs.mcapi.common.SigningUtils.hashSHA256;
import static us.sodiumlabs.mcapi.common.SigningUtils.hmacSHA256;

public class Signing {
    private final Clock clock;

    public Signing(final Clock clock) {
        this.clock = requireNonNull(clock);
    }

    public ByteBuffer constructSignaturePayload(final ByteBuffer nonce, final String secret) {
        final ByteBuffer nonceCopy = copy(nonce);

        final LocalDateTime now = LocalDateTime.now(clock);
        final ByteBuffer canonical = canonical(nonceCopy, now);
        final ByteBuffer signature = sign(secret, canonical, now);

        return mergeByteBuffers(canonical, signature, nonceCopy);
    }

    public boolean isSignaturePayloadValid(final ByteBuffer payload, final ByteBuffer nonce, final String secret) {
        final ByteBuffer payloadCopy = copy(payload);
        final ByteBuffer nonceCopy = copy(nonce);

        final ByteBuffer canonical = readFromBuffer(payloadCopy);
        final ByteBuffer signature = readFromBuffer(payloadCopy);
        final ByteBuffer providedNonce = readFromBuffer(payloadCopy);

        // if the local date time is invalid, replace it with a valid date time that will always fail so that we don't
        // make it so obvious when attackers do provide a valid date.
        LocalDateTime signingTime;
        try {
            signingTime = readSigningTimeFromBuffer(canonical);
        } catch (final Exception e) {
            signingTime = LocalDateTime.now(clock).minusYears(5);
        }
        final ByteBuffer calculatedCanonical = canonical(nonceCopy, signingTime);
        final ByteBuffer calculatedSignature = sign(secret, canonical, signingTime);

        // Bitwise AND used here so that we always perform every operation
        return signingTime.isAfter(LocalDateTime.now(clock).minusMinutes(5))
            & secureCompareBuffers(canonical, calculatedCanonical)
            & secureCompareBuffers(signature, calculatedSignature)
            & secureCompareBuffers(providedNonce, nonceCopy);
    }

    protected LocalDateTime readSigningTimeFromBuffer(final ByteBuffer canonical) {
        final ByteBuffer canonicalCopy = copy(canonical);
        final String canonicalString = new String(canonicalCopy.array());
        final String date =
            canonicalString.substring(canonicalString.indexOf("date:") + 5, canonicalString.lastIndexOf("\n"));

        return LocalDateTime.ofInstant(Instant.parse(date), ZoneOffset.UTC);
    }

    protected ByteBuffer canonical(final ByteBuffer nonce, final LocalDateTime dateTime) {
        final Instant utcInstant = dateTime.toInstant(ZoneOffset.UTC);
        final String canonicalString = "LOGIN\n" +
            "date:" + utcInstant.toString() + "\n" +
            "nonce:" + toHex(nonce);

        return ByteBuffer.wrap(canonicalString.getBytes()).asReadOnlyBuffer();
    }

    protected ByteBuffer sign(final String secret, final ByteBuffer canonical, final LocalDateTime dateTime) {
        final Instant instant = dateTime.toInstant(ZoneOffset.UTC);

        // Construct string to sign.
        final ByteBuffer toSign = ByteBuffer.wrap(("MCAPI-HMAC-SHA256\n" +
            instant.toString() + "\n" +
            toHex(hashSHA256(canonical))).getBytes());

        // HMAC that shit
        return hmacSHA256(hmacSHA256(fromBase64(secret), instant.toString()), toSign);
    }
}
