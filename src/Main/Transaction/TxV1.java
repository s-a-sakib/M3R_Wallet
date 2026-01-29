package Main.Transaction;

import Main.Util.Hash.Hash; // uses your project hasher
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class TxV1 {

    public static final int ADDRESS_LEN = 20;
    public static final int HASH_LEN = 32;

    private final TxSchema.Version version;
    private final TxSchema.ChainID chainId;
    private final TxSchema.TxType type;

    private final long nonce;       // uint64
    private final long fee;         // uint64
    private final long timestamp;   // unix seconds uint64

    private final byte[] fromAddr20; // 20 bytes (payload20)

    private final byte[] payload;   // type-specific bytes
    private final byte[] memo;      // optional bytes

    private final byte sigScheme;   // 1 = ECDSA_secp256k1
    private byte[] signature;       // DER signature bytes

    public TxV1(
            TxSchema.Version version,
            TxSchema.ChainID chainId,
            TxSchema.TxType type,
            long nonce,
            long fee,
            long timestamp,
            byte[] fromAddr20,
            byte[] payload,
            byte[] memo,
            byte sigScheme
    ) {
        if (version == null) throw new IllegalArgumentException("version is null");
        if (chainId == null) throw new IllegalArgumentException("chainId is null");
        if (type == null) throw new IllegalArgumentException("type is null");
        if (nonce < 0) throw new IllegalArgumentException("nonce must be >= 0");
        if (fee < 0) throw new IllegalArgumentException("fee must be >= 0");
        if (timestamp < 0) throw new IllegalArgumentException("timestamp must be >= 0");

        this.version = version;
        this.chainId = chainId;
        this.type = type;

        this.nonce = nonce;
        this.fee = fee;
        this.timestamp = timestamp;

        this.fromAddr20 = requireLen(fromAddr20, ADDRESS_LEN, "fromAddr20");
        this.payload = payload == null ? new byte[0] : payload.clone();
        this.memo = memo == null ? new byte[0] : memo.clone();

        this.sigScheme = sigScheme;
        this.signature = null;
    }

    /** Deterministic bytes for hashing/signing (signature excluded). */
    public byte[] encodeForSigning() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Codec.putU16(out, version.code);
        Codec.putU32(out, chainId.id);
        Codec.putI8(out, type.code);

        Codec.putU64(out, nonce);
        Codec.putU64(out, fee);
        Codec.putU64(out, timestamp);

        Codec.write(out, fromAddr20);

        Codec.putVarBytes(out, payload);
        Codec.putVarBytes(out, memo);

        Codec.putI8(out, sigScheme & 0xFF);

        return out.toByteArray();
    }

    /** Full tx bytes includes signature as VarBytes at the end. */
    public byte[] encodeFull() {
        if (signature == null) throw new IllegalStateException("signature not set");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Codec.write(out, encodeForSigning());
        Codec.putVarBytes(out, signature);
        return out.toByteArray();
    }

    /** Keccak256(encodeForSigning()) using your Hash.KECCAK_256. */
    public byte[] txHashKeccak256() {
        byte[] h = Hash.KECCAK_256(encodeForSigning());
        if (h == null || h.length != HASH_LEN) {
            throw new IllegalStateException("KECCAK_256 must return 32 bytes");
        }
        return h;
    }

    /** Sign using secp256k1 private key (32 bytes). */
    public void signSecp256k1(byte[] privateKey32) {
        byte[] hash32 = txHashKeccak256();
        this.signature = EcdsaSecp256k1.signDer(hash32, privateKey32);
    }

    /** Verify signature against a compressed public key (33 bytes). */
    public boolean verifySecp256k1(byte[] compressedPubKey33) {
        if (signature == null) return false;
        return EcdsaSecp256k1.verifyDer(txHashKeccak256(), signature, compressedPubKey33);
    }

    private static byte[] requireLen(byte[] x, int len, String name) {
        if (x == null) throw new IllegalArgumentException(name + " is null");
        if (x.length != len) throw new IllegalArgumentException(name + " must be " + len + " bytes");
        return x.clone();
    }

    // Getters
    public TxSchema.Version getVersion() { return version; }
    public TxSchema.ChainID getChainId() { return chainId; }
    public TxSchema.TxType getType() { return type; }
    public long getNonce() { return nonce; }
    public long getFee() { return fee; }
    public long getTimestamp() { return timestamp; }
    public byte[] getFromAddr20() { return fromAddr20.clone(); }
    public byte[] getPayload() { return payload.clone(); }
    public byte[] getMemo() { return memo.clone(); }
    public byte getSigScheme() { return sigScheme; }
    public byte[] getSignature() { return signature == null ? null : signature.clone(); }

    @Override
    public String toString() {
        return "TxV1{" +
                "v=" + version +
                ", chain=" + chainId +
                ", type=" + type +
                ", nonce=" + nonce +
                ", fee=" + fee +
                ", ts=" + timestamp +
                ", from20=" + Arrays.toString(fromAddr20) +
                ", payloadLen=" + payload.length +
                ", memoLen=" + memo.length +
                ", sigScheme=" + (sigScheme & 0xFF) +
                ", sigLen=" + (signature == null ? 0 : signature.length) +
                '}';
    }
    public static TxV1 decodeFull(byte[] raw) {
        if (raw == null) throw new IllegalArgumentException("raw is null");

        Reader r = new Reader(raw);

        int vCode = r.u16();
        int chainId = r.u32();
        int tCode = r.u8(); // i8 stored, but we treat it as unsigned 0..255

        long nonce = r.u64();
        long fee = r.u64();
        long ts = r.u64();

        byte[] from20 = r.bytesExact(ADDRESS_LEN);

        byte[] payload = r.varBytesU32();
        byte[] memo = r.varBytesU32();

        int sigSchemeU8 = r.u8();
        byte sigScheme = (byte) sigSchemeU8;

        byte[] sig = r.varBytesU32();

        if (r.remaining() != 0) {
            throw new IllegalArgumentException("extra bytes at end: " + r.remaining());
        }

        TxSchema.Version version = versionFromCode(vCode);
        TxSchema.ChainID cid = chainFromId(chainId);
        TxSchema.TxType type = typeFromCode(tCode);

        TxV1 tx = new TxV1(
                version,
                cid,
                type,
                nonce,
                fee,
                ts,
                from20,
                payload,
                memo,
                sigScheme
        );
        tx.signature = sig;
        return tx;
    }

    private static TxSchema.Version versionFromCode(int code) {
        for (TxSchema.Version v : TxSchema.Version.values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("unknown version code: " + code);
    }

    private static TxSchema.ChainID chainFromId(int id) {
        for (TxSchema.ChainID c : TxSchema.ChainID.values()) {
            if (c.id == id) return c;
        }
        throw new IllegalArgumentException("unknown chainId: " + id);
    }

    private static TxSchema.TxType typeFromCode(int code) {
        for (TxSchema.TxType t : TxSchema.TxType.values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("unknown txType code: " + code);
    }

    /**
     * Mirror of Codec.putU16/putU32/putU64 + putVarBytes (assumed U32 length prefix).
     * If your Codec uses VARINT instead of U32, tell me and I’ll swap this.
     */
    static final class Reader {
        private final byte[] b;
        private int p;

        Reader(byte[] b) { this.b = b; this.p = 0; }

        int remaining() { return b.length - p; }

        private void need(int n) {
            if (p + n > b.length) throw new IllegalArgumentException("truncated: need " + n + " remaining " + remaining());
        }

        int u8() {
            need(1);
            return b[p++] & 0xFF;
        }

        int u16() {
            need(2);
            int v = ((b[p] & 0xFF) << 8) | (b[p + 1] & 0xFF);
            p += 2;
            return v;
        }

        int u32() {
            need(4);
            int v = ((b[p] & 0xFF) << 24)
                    | ((b[p + 1] & 0xFF) << 16)
                    | ((b[p + 2] & 0xFF) << 8)
                    |  (b[p + 3] & 0xFF);
            p += 4;
            return v;
        }

        long u64() {
            need(8);
            long v = 0;
            for (int i = 0; i < 8; i++) {
                v = (v << 8) | (b[p + i] & 0xFFL);
            }
            p += 8;
            return v;
        }

        byte[] bytesExact(int n) {
            need(n);
            byte[] out = java.util.Arrays.copyOfRange(b, p, p + n);
            p += n;
            return out;
        }

        byte[] varBytesU32() {
            int n = u32();
            if (n < 0) throw new IllegalArgumentException("negative length");
            need(n);
            byte[] out = java.util.Arrays.copyOfRange(b, p, p + n);
            p += n;
            return out;
        }
    }

}
