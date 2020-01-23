package us.sodiumlabs.mcapi.common;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class BaseUtilsTest {

    @Test
    public void toHex() {
        final byte[] bytes = new byte[] { 0x01, 0x23, 0x45, 0x67, -0x77, -0x55, -0x33, -0x11 };
        final String hex = BaseUtils.toHex(ByteBuffer.wrap(bytes));
        assertEquals("0123456789abcdef", hex);
    }

    @Test
    public void toBase64() {
        final String rep = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        final ByteBuffer byteBuffer = BaseUtils.fromBase64(rep);
        assertEquals("00108310518720928b30d38f41149351559761969b71d79f8218a39259a7a29aabb2dbafc31cb3d35db7e39ebbf3dfbf",
            BaseUtils.toHex(byteBuffer));
        assertEquals(rep, BaseUtils.toBase64(byteBuffer));
    }
}