package Main.Util.Signature;

import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.List;

import Main.Util.Hash.Hash;
public class PrivateKeyGenerator {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // secp256k1 curve order
    private static final BigInteger CURVE_N =
            new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

    /**
     * Generate private key from 12 or 24 mnemonic words
     */
    public static byte[] privateKeyFromMnemonic(String mnemonic) {

        List<String> words = Arrays.asList(mnemonic.trim().split("\\s+"));

        if (words.size() != 12 && words.size() != 24) {
            throw new IllegalArgumentException("Mnemonic must be 12 or 24 words");
        }

        // BIP-39 seed derivation
        PKCS5S2ParametersGenerator gen =
                new PKCS5S2ParametersGenerator(new SHA512Digest());

        byte[] salt = "mnemonic".getBytes(StandardCharsets.UTF_8);

        gen.init(
                mnemonic.getBytes(StandardCharsets.UTF_8),
                salt,
                2048
        );

        byte[] seed = ((KeyParameter) gen.generateDerivedParameters(512)).getKey();

        // Hash seed → 256-bit private key
        byte[] privateKey = Hash.SHA_256(seed);

        return normalizePrivateKey(privateKey);
    }

    /**
     * Generate random 256-bit private key using OS entropy
     */
    public static byte[] randomPrivateKey() {
        SecureRandom secureRandom = new SecureRandom();

        byte[] key = new byte[32];
        do {
            secureRandom.nextBytes(key);
        } while (!isValidPrivateKey(key));

        return key;
    }

    private static boolean isValidPrivateKey(byte[] key) {
        BigInteger k = new BigInteger(1, key);
        return k.compareTo(BigInteger.ONE) >= 0 && k.compareTo(CURVE_N) < 0;
    }

    private static byte[] normalizePrivateKey(byte[] key) {
        BigInteger k = new BigInteger(1, key).mod(CURVE_N.subtract(BigInteger.ONE)).add(BigInteger.ONE);
        byte[] normalized = k.toByteArray();

        // Ensure exactly 32 bytes
        if (normalized.length > 32) {
            normalized = Arrays.copyOfRange(normalized, normalized.length - 32, normalized.length);
        } else if (normalized.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(normalized, 0, padded, 32 - normalized.length, normalized.length);
            normalized = padded;
        }
        return normalized;
    }
}
