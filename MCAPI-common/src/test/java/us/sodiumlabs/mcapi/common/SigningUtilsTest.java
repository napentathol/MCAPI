package us.sodiumlabs.mcapi.common;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class SigningUtilsTest {
    @Test
    public void hmacSHA256() {
        final ByteBuffer hmac = SigningUtils.hmacSHA256(
            ByteBuffer.wrap("qq".getBytes()),
            "Thank you Mario! But our princess is in another castle!");
        assertEquals("6e68763989914b3f069268615ac7dcfb31684c737642722cccfe8a90d596f7fe",
            BaseUtils.toHex(hmac));
    }

    @Test
    public void hashSHA256() {
        final ByteBuffer hash =
            SigningUtils.hashSHA256(ByteBuffer.wrap("It's dangerous to go alone! Take this.".getBytes()));
        assertEquals("848af86232648bd01fbae74052e2bca09a3a6dc53dba5289f79573e5fd2b9a02", BaseUtils.toHex(hash));
    }
}