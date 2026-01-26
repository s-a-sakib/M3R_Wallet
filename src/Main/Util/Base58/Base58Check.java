package Main.Util.Base58;

import java.security.MessageDigest;
import java.util.Arrays;

public class Base58Check {

    public static String encode(byte version, byte[] payload20) {
        if (payload20 == null || payload20.length != 20) {
            throw new IllegalArgumentException("payload must be 20 bytes");
        }

        byte[] versioned = new byte[1 + payload20.length];
        versioned[0] = version;
        System.arraycopy(payload20, 0, versioned, 1, payload20.length);

        byte[] checksum = checksum4(versioned);

        byte[] full = new byte[versioned.length + 4];
        System.arraycopy(versioned, 0, full, 0, versioned.length);
        System.arraycopy(checksum, 0, full, versioned.length, 4);

        return Base58.encode(full);
    }

    private static byte[] checksum4(byte[] data) {
        byte[] first = sha256(data);
        byte[] second = sha256(first);
        return Arrays.copyOfRange(second, 0, 4);
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
