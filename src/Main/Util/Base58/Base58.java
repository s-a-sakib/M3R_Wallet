package Main.Util.Base58;
import java.math.BigInteger;

public class Base58 {

    private static final char[] ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
                    .toCharArray();

    private static final BigInteger BASE = BigInteger.valueOf(58);

    public static String encode(byte[] input) {
        if (input.length == 0) return "";

        // Count leading zeros
        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) {
            zeros++;
        }

        // Convert to BigInteger
        BigInteger value = new BigInteger(1, input);

        // Encode
        StringBuilder sb = new StringBuilder();
        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divmod = value.divideAndRemainder(BASE);
            value = divmod[0];
            sb.append(ALPHABET[divmod[1].intValue()]);
        }

        // Add leading zeros
        for (int i = 0; i < zeros; i++) {
            sb.append(ALPHABET[0]); // '1'
        }

        return sb.reverse().toString();
    }
}
