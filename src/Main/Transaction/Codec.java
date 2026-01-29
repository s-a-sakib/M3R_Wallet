package Main.Transaction;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class Codec {

    private Codec() {}

    public static void putU16(ByteArrayOutputStream out, int v) {
        ByteBuffer b = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN);
        b.putShort((short) (v & 0xFFFF));
        write(out, b.array());
    }

    public static void putU32(ByteArrayOutputStream out, long v) {
        ByteBuffer b = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        b.putInt((int) (v & 0xFFFFFFFFL));
        write(out, b.array());
    }

    public static void putU64(ByteArrayOutputStream out, long v) {
        ByteBuffer b = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        b.putLong(v);
        write(out, b.array());
    }

    public static void putI8(ByteArrayOutputStream out, int v) {
        out.write((byte) (v & 0xFF));
    }

    /** VarBytes = u32 length + bytes */
    public static void putVarBytes(ByteArrayOutputStream out, byte[] data) {
        if (data == null) data = new byte[0];
        putU32(out, data.length);
        write(out, data);
    }

    public static void write(ByteArrayOutputStream out, byte[] bytes) {
        if (bytes == null || bytes.length == 0) return;
        out.write(bytes, 0, bytes.length);
    }
}
