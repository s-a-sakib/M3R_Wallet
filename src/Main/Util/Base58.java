package Main.Util;

import java.math.BigInteger;

public class Base58 {
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE = BigInteger.valueOf(58);

    public static String encode(byte[] input) {
        if (input == null || input.length == 0) {
            return "";
        }

        BigInteger value = new BigInteger(1, input);
        StringBuilder sb = new StringBuilder();
        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRem = value.divideAndRemainder(BASE);
            sb.append(ALPHABET.charAt(divRem[1].intValue()));
            value = divRem[0];
        }

        for (byte b : input) {
            if (b == 0) {
                sb.append(ALPHABET.charAt(0));
            } else {
                break;
            }
        }

        return sb.reverse().toString();
    }
}
