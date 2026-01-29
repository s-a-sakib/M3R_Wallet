package Main.Transaction.Payload;

import Main.Transaction.Codec;
import Main.Transaction.TxV1;

import java.io.ByteArrayOutputStream;

public final class EscrowCreatePayload {

    private EscrowCreatePayload() {}

    public static byte[] encode(
            byte[] escrowId32,
            byte[] buyer20,
            byte[] seller20,
            byte[] arbiter20,
            long amount,
            long expiryTs,
            int releaseModeU8,
            int disputeModeU8,
            byte[] metaHash32
    ) {
        requireLen(escrowId32, 32, "escrowId32");
        requireLen(buyer20, TxV1.ADDRESS_LEN, "buyer20");
        requireLen(seller20, TxV1.ADDRESS_LEN, "seller20");
        requireLen(arbiter20, TxV1.ADDRESS_LEN, "arbiter20");
        requireLen(metaHash32, 32, "metaHash32");

        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        if (expiryTs <= 0) throw new IllegalArgumentException("expiryTs must be > 0");
        if (releaseModeU8 < 0 || releaseModeU8 > 255) throw new IllegalArgumentException("releaseModeU8 invalid");
        if (disputeModeU8 < 0 || disputeModeU8 > 255) throw new IllegalArgumentException("disputeModeU8 invalid");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Codec.write(out, escrowId32);
        Codec.write(out, buyer20);
        Codec.write(out, seller20);
        Codec.write(out, arbiter20);
        Codec.putU64(out, amount);
        Codec.putU64(out, expiryTs);
        Codec.putI8(out, releaseModeU8);
        Codec.putI8(out, disputeModeU8);
        Codec.write(out, metaHash32);

        return out.toByteArray();
    }

    private static void requireLen(byte[] x, int len, String name) {
        if (x == null || x.length != len) throw new IllegalArgumentException(name + " must be " + len + " bytes");
    }
}
