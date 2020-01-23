package us.sodiumlabs.mcapi.common;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class ByteBufferUtilsTest {

    @Test
    public void mergeByteBuffers() {
        final ByteBuffer first = ByteBuffer.wrap(new byte[] { 0x01 });
        final ByteBuffer second = ByteBuffer.wrap(new byte[] { 0x23, 0x45 });
        final ByteBuffer third = ByteBuffer.wrap(new byte[] { 0x67, -0x77, -0x55, -0x33});

        final ByteBuffer merged = ByteBufferUtils.mergeByteBuffers(first, second, third);
        assertEquals("0000000101000000022345000000046789abcd", BaseUtils.toHex(merged));

        final ByteBuffer detectedFirst = ByteBufferUtils.readFromBuffer(merged);
        final ByteBuffer detectedSecond = ByteBufferUtils.readFromBuffer(merged);
        final ByteBuffer detectedThird = ByteBufferUtils.readFromBuffer(merged);

        assertTrue(ByteBufferUtils.secureCompareBuffers(first, detectedFirst));
        assertFalse(ByteBufferUtils.secureCompareBuffers(second, detectedFirst));
        assertFalse(ByteBufferUtils.secureCompareBuffers(third, detectedFirst));

        assertFalse(ByteBufferUtils.secureCompareBuffers(first, detectedSecond));
        assertTrue(ByteBufferUtils.secureCompareBuffers(second, detectedSecond));
        assertFalse(ByteBufferUtils.secureCompareBuffers(third, detectedSecond));

        assertFalse(ByteBufferUtils.secureCompareBuffers(first, detectedThird));
        assertFalse(ByteBufferUtils.secureCompareBuffers(second, detectedThird));
        assertTrue(ByteBufferUtils.secureCompareBuffers(third, detectedThird));

        assertEquals(19, merged.capacity());
        assertEquals(19, merged.position());
        final ByteBuffer copy = ByteBufferUtils.copy(merged);
        assertEquals(0, copy.position());
        assertEquals(merged.capacity(), copy.capacity());
        assertTrue(ByteBufferUtils.secureCompareBuffers(merged, copy));
    }
}