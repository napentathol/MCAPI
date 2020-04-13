package us.sodiumlabs.mcapi.common;

import java.nio.ByteBuffer;
import java.util.Random;

public final class ByteBufferUtils {
    private ByteBufferUtils() {}

    public static ByteBuffer copy(final ByteBuffer bb) {
        final ByteBuffer newBb = bb.asReadOnlyBuffer();
        newBb.rewind();

        final ByteBuffer copy = ByteBuffer.allocate(newBb.capacity());
        copy.put(newBb);
        copy.rewind();
        return copy;
    }

    public static ByteBuffer mergeByteBuffers(final ByteBuffer ... buffers) {
        int size = 0;
        for(final ByteBuffer b: buffers) {
            size += b.capacity() + Integer.BYTES;
        }
        final ByteBuffer out = ByteBuffer.allocate(size);
        for(final ByteBuffer b: buffers) {
            writeToBuffer(out, b);
        }
        out.rewind();
        return out;
    }

    public static void writeToBuffer(final ByteBuffer out, final ByteBuffer in) {
        final ByteBuffer inCopy = copy(in);
        out.putInt(inCopy.capacity());
        out.put(inCopy);
    }

    public static ByteBuffer readFromBuffer(final ByteBuffer in) {
        final int size = in.getInt();
        final byte[] out = new byte[size];
        in.get(out);
        return ByteBuffer.wrap(out);
    }

    public static boolean secureCompareBuffers(final ByteBuffer a, final ByteBuffer b) {
        final ByteBuffer aCopy = copy(a);
        final ByteBuffer bCopy = copy(b);

        // always check at least as many bytes as provided.
        boolean equal = aCopy.capacity() == bCopy.capacity();
        while(aCopy.hasRemaining() && bCopy.hasRemaining()) {
            // Bitwise AND used here so that we always perform every operation
            equal &= aCopy.get() == bCopy.get();
        }
        return equal;
    }

    public static ByteBuffer randomBytes(final Random random, final int i) {
        final byte[] array = new byte[i];
        random.nextBytes(array);
        return ByteBuffer.wrap(array);
    }
}
