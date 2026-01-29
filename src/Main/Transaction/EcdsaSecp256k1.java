package Main.Transaction;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.Arrays;

public final class EcdsaSecp256k1 {

    private static final ECParameterSpec SPEC = ECNamedCurveTable.getParameterSpec("secp256k1");
    private static final ECDomainParameters DOMAIN = new ECDomainParameters(
            SPEC.getCurve(), SPEC.getG(), SPEC.getN(), SPEC.getH()
    );

    private EcdsaSecp256k1() {}

    /** Deterministic RFC6979 signature, returned as DER (r,s). */
    public static byte[] signDer(byte[] hash32, byte[] privKey32) {
        if (hash32 == null || hash32.length != 32) throw new IllegalArgumentException("hash32 must be 32 bytes");
        if (privKey32 == null || privKey32.length != 32) throw new IllegalArgumentException("privKey32 must be 32 bytes");

        BigInteger d = new BigInteger(1, privKey32);
        ECPrivateKeyParameters priv = new ECPrivateKeyParameters(d, DOMAIN);

        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        signer.init(true, priv);

        BigInteger[] sig = signer.generateSignature(hash32);

        BigInteger r = sig[0];
        BigInteger s = sig[1];

        // Low-S normalization (prevents malleability)
        BigInteger n = SPEC.getN();
        BigInteger halfN = n.shiftRight(1);
        if (s.compareTo(halfN) > 0) {
            s = n.subtract(s);
        }

        return derEncode(r, s);
    }

    public static boolean verifyDer(byte[] hash32, byte[] derSig, byte[] compressedPubKey33) {
        if (hash32 == null || hash32.length != 32) throw new IllegalArgumentException("hash32 must be 32 bytes");
        if (derSig == null || derSig.length == 0) throw new IllegalArgumentException("derSig empty");
        if (compressedPubKey33 == null || compressedPubKey33.length != 33)
            throw new IllegalArgumentException("compressedPubKey33 must be 33 bytes");

        BigInteger[] rs = derDecode(derSig);
        BigInteger r = rs[0];
        BigInteger s = rs[1];

        ECPoint Q = SPEC.getCurve().decodePoint(compressedPubKey33).normalize();
        ECPublicKeyParameters pub = new ECPublicKeyParameters(Q, DOMAIN);

        ECDSASigner verifier = new ECDSASigner();
        verifier.init(false, pub);
        return verifier.verifySignature(hash32, r, s);
    }

    private static byte[] derEncode(BigInteger r, BigInteger s) {
        try {
            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(new ASN1Integer(r));
            v.add(new ASN1Integer(s));
            return new DERSequence(v).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("DER encode failed", e);
        }
    }

    private static BigInteger[] derDecode(byte[] der) {
        try (ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(der))) {
            Object obj = in.readObject();
            if (!(obj instanceof DLSequence)) throw new IllegalArgumentException("Not a DER sequence");
            DLSequence seq = (DLSequence) obj;
            BigInteger r = ((ASN1Integer) seq.getObjectAt(0)).getValue();
            BigInteger s = ((ASN1Integer) seq.getObjectAt(1)).getValue();
            return new BigInteger[]{r, s};
        } catch (Exception e) {
            throw new RuntimeException("DER decode failed", e);
        }
    }
}
