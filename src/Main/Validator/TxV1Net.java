package Main.Validator;

import Main.Util.Hash.Hash;
import Main.Transaction.EcdsaSecp256k1;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class TxV1Net {

    public final int versionU16;
    public final long chainIdU32;
    public final int typeU8;

    public final long nonceU64;
    public final long feeU64;
    public final long timestampU64;

    public final byte[] fromAddr20;

    public final byte[] payload;
    public final byte[] memo;

    public final int sigSchemeU8;
    public final byte[] signatureDer;

    private final byte[] signingBytes; // exact preimage bytes, deterministic
    public final byte[] txHash32;      // keccak256(signingBytes)

    private TxV1Net(int versionU16, long chainIdU32, int typeU8,
                    long nonceU64, long feeU64, long timestampU64,
                    byte[] fromAddr20,
                    byte[] payload, byte[] memo,
                    int sigSchemeU8, byte[] signatureDer,
                    byte[] signingBytes) {
        this.versionU16 = versionU16;
        this.chainIdU32 = chainIdU32;
        this.typeU8 = typeU8;
        this.nonceU64 = nonceU64;
        this.feeU64 = feeU64;
        this.timestampU64 = timestampU64;
        this.fromAddr20 = fromAddr20;
        this.payload = payload;
        this.memo = memo;
        this.sigSchemeU8 = sigSchemeU8;
        this.signatureDer = signatureDer;
        this.signingBytes = signingBytes;
        this.txHash32 = Hash.KECCAK_256(signingBytes);
    }

    public static TxV1Net decodeFull(byte[] rawTx) {
        ByteBuffer b = ByteBuffer.wrap(rawTx).order(ByteOrder.BIG_ENDIAN);

        int version = b.getShort() & 0xFFFF;
        long chainId = b.getInt() & 0xFFFFFFFFL;
        int type = b.get() & 0xFF;

        long nonce = b.getLong();
        long fee = b.getLong();
        long ts = b.getLong();

        byte[] from20 = new byte[20];
        b.get(from20);

        byte[] payload = readVarBytes(b);
        byte[] memo = readVarBytes(b);

        int sigScheme = b.get() & 0xFF;
        byte[] sig = readVarBytes(b);

        // Reconstruct signing bytes = everything except the last signature varbytes
        // signing bytes length = raw - (4 + sigLen)
        int sigLen = sig.length;
        int signingLen = rawTx.length - (4 + sigLen);
        byte[] signingBytes = Arrays.copyOfRange(rawTx, 0, signingLen);

        return new TxV1Net(version, chainId, type, nonce, fee, ts, from20, payload, memo, sigScheme, sig, signingBytes);
    }

    private static byte[] readVarBytes(ByteBuffer b) {
        int len = b.getInt();
        if (len < 0 || len > 10_000_000) throw new IllegalArgumentException("bad varbytes len=" + len);
        byte[] out = new byte[len];
        b.get(out);
        return out;
    }

    /** Address binding: fromAddr20 == last20(keccak(pubKeyCompressed33)). */
    public static boolean addressMatchesPubKey(byte[] fromAddr20, byte[] pubKeyCompressed33) {
        byte[] k = Hash.KECCAK_256(pubKeyCompressed33);
        byte[] expected = Arrays.copyOfRange(k, 12, 32);
        return Arrays.equals(expected, fromAddr20);
    }

    public boolean verifySignature(byte[] pubKeyCompressed33) {
        return EcdsaSecp256k1.verifyDer(txHash32, signatureDer, pubKeyCompressed33);
    }

    /** TRANSFER payload layout: to20(20) + amountU64(8) */
    public long transferAmountOrThrow() {
        if (payload == null || payload.length != 28) throw new IllegalArgumentException("transfer payload must be 28 bytes");
        return readU64BE(payload, 20);
    }

    public byte[] transferTo20OrThrow() {
        if (payload == null || payload.length != 28) throw new IllegalArgumentException("transfer payload must be 28 bytes");
        return Arrays.copyOfRange(payload, 0, 20);
    }

    private static long readU64BE(byte[] b, int off) {
        long v = 0;
        for (int i = 0; i < 8; i++) v = (v << 8) | (b[off + i] & 0xFFL);
        return v;
    }
}
