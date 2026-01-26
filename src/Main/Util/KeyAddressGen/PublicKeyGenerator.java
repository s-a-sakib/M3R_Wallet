package Main.Util.KeyAddressGen;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.Security;

public class PublicKeyGenerator {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Generate uncompressed public key (65 bytes, 0x04 || X || Y)
     */
    public static byte[] generateUncompressedPublicKey(byte[] privateKey) {

        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");

        BigInteger d = new BigInteger(1, privateKey);

        ECPoint Q = ecSpec.getG().multiply(d).normalize();

        byte[] x = Q.getAffineXCoord().getEncoded();
        byte[] y = Q.getAffineYCoord().getEncoded();

        byte[] publicKey = new byte[65];
        publicKey[0] = 0x04;

        System.arraycopy(x, 0, publicKey, 1, 32);
        System.arraycopy(y, 0, publicKey, 33, 32);

        return publicKey;
    }

    /**
     * Generate compressed public key (33 bytes)
     * 0x02 if Y is even, 0x03 if Y is odd
     */
    public static byte[] generateCompressedPublicKey(byte[] privateKey) {

        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");

        BigInteger d = new BigInteger(1, privateKey);

        ECPoint Q = ecSpec.getG().multiply(d).normalize();

        byte[] x = Q.getAffineXCoord().getEncoded();
        byte[] y = Q.getAffineYCoord().getEncoded();

        byte prefix = (y[y.length - 1] & 1) == 0 ? (byte) 0x02 : (byte) 0x03;

        byte[] publicKey = new byte[33];
        publicKey[0] = prefix;
        System.arraycopy(x, 0, publicKey, 1, 32);

        return publicKey;
    }
}
