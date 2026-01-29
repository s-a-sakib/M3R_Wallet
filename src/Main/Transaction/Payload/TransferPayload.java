package Main.Transaction.Payload;

import Main.Transaction.Codec;
import Main.Transaction.TxV1;

import java.io.ByteArrayOutputStream;

public final class TransferPayload {

    private TransferPayload() {}

    public static byte[] encode(byte[] toAddr20, long amount) {
        if (toAddr20 == null || toAddr20.length != TxV1.ADDRESS_LEN)
            throw new IllegalArgumentException("toAddr20 must be 20 bytes");
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Codec.write(out, toAddr20);
        Codec.putU64(out, amount);
        return out.toByteArray();
    }
}
