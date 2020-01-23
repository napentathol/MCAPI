package us.sodiumlabs.mcapi.common;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class BaseUtils {
    private BaseUtils() {}

    public static String toHex(final ByteBuffer bb) {
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

    public static ByteBuffer fromBase64(final String s) {
        return ByteBuffer.wrap(Base64.getDecoder().decode(s));
    }

    public static String toBase64(final ByteBuffer bb) {
        final ByteBuffer newBb = bb.asReadOnlyBuffer();
        newBb.rewind();

        final ByteBuffer encoded = Base64.getEncoder().withoutPadding().encode(newBb);
        return new String(encoded.array(), StandardCharsets.UTF_8);
    }
}
