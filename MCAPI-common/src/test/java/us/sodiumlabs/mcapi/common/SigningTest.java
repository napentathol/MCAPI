package us.sodiumlabs.mcapi.common;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static us.sodiumlabs.mcapi.common.BaseUtils.toBase64;
import static us.sodiumlabs.mcapi.common.ByteBufferUtils.copy;
import static us.sodiumlabs.mcapi.common.ByteBufferUtils.secureCompareBuffers;

public class SigningTest {
    @Test
    public void test_canonical() {
        final ByteBuffer nonce = ByteBuffer.wrap(new byte[] {0x01, 0x23, 0x45, 0x67});
        final LocalDateTime time = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        final Signing signing = new Signing(Clock.system(ZoneOffset.ofHours(8)));

        final ByteBuffer out = copy(signing.canonical(nonce, time));
        assertEquals(new String(out.array()), "LOGIN\n" +
            "date:2000-01-01T00:00:00Z\n" +
            "nonce:01234567");
    }

    @Test
    public void test_sign() {
        final String key = "abcdefghijklmnopqrstuvwxyz0123456789+/XY";
        final ByteBuffer nonce = ByteBuffer.wrap(new byte[] {0x01, 0x23, 0x45, 0x67});
        final LocalDateTime time = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        final Signing signing = new Signing(Clock.system(ZoneOffset.ofHours(8)));

        final ByteBuffer signature = signing.sign(key, signing.canonical(nonce, time), time);
        assertEquals("ct4MBlViB2s97czqmWfm+9eL96nCa23khzYtwabttno", toBase64(signature));
    }

    @Test
    public void test_construct_and_validate_UTCplus8() {
        final String key = "abcdefghijklmnopqrstuvwxyz0123456789+/XY";
        final ByteBuffer nonce = ByteBuffer.wrap(new byte[] {0x01, 0x23, 0x45, 0x67});

        final Signing signing = new Signing(Clock.system(ZoneOffset.ofHours(8)));

        final ByteBuffer payload = signing.constructSignaturePayload(nonce, key);
        assertEquals(0, payload.position());

        assertTrue(signing.isSignaturePayloadValid(payload, nonce, key));
        assertEquals(0, payload.position());
    }

    @Test
    public void test_construct_and_validate_UTCminus8() {
        final String key = "abcdefghijklmnopqrstuvwxyz0123456789+/XY";
        final ByteBuffer nonce = ByteBuffer.wrap(new byte[] {0x01, 0x23, 0x45, 0x67});

        final Signing signing = new Signing(Clock.system(ZoneOffset.ofHours(-8)));

        final ByteBuffer payload = signing.constructSignaturePayload(nonce, key);
        assertEquals(0, payload.position());

        assertTrue(signing.isSignaturePayloadValid(payload, nonce, key));
        assertEquals(0, payload.position());
    }

    @Test
    public void test_construct_UTCminus8_UTCplus8_same() {
        final String key = "abcdefghijklmnopqrstuvwxyz0123456789+/XY";
        final ByteBuffer nonce = ByteBuffer.wrap(new byte[] {0x01, 0x23, 0x45, 0x67});
        final Instant now = Instant.now();

        // fix the clock offset
        final Clock clock1 = Clock.fixed(now.minus(8, ChronoUnit.HOURS), ZoneOffset.ofHours(8));
        final Signing signing1 = new Signing(clock1);

        final ByteBuffer payload1 = signing1.constructSignaturePayload(nonce, key);
        assertEquals(0, payload1.position());

        // fix the clock offset
        final Clock clock2 = Clock.fixed(now.plus(8, ChronoUnit.HOURS), ZoneOffset.ofHours(-8));
        final Signing signing2 = new Signing(clock2);

        final ByteBuffer payload2 = signing2.constructSignaturePayload(nonce, key);
        assertEquals(0, payload2.position());

        assertTrue(secureCompareBuffers(payload1, payload2));
        assertEquals(0, payload1.position());
        assertEquals(0, payload2.position());
    }
}