package Main.Util.KeyAddressGen;

import Main.Util.Base58.Base58Check;
import Main.Util.ByteToHex;
import Main.Util.Hash.Hash;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class M3RAddressFactory {

    public static final byte VERSION = (byte) 0x35;

    /** Generate a new wallet (privateKey, compressed publicKey, address).
     *  If mnemonic is null/empty -> random key.
     *  Else -> key derived from mnemonic.
     */
    public static Wallet generate(String mnemonic) {

        final byte[] privateKey;
        final String usedMnemonic;

        if (mnemonic == null || mnemonic.trim().isEmpty()) {
            privateKey = PrivateKeyGenerator.randomPrivateKey();
            usedMnemonic = null;
        } else {
            usedMnemonic = mnemonic.trim();
            privateKey = PrivateKeyGenerator.privateKeyFromMnemonic(usedMnemonic);
        }

        byte[] publicKeyCompressed = PublicKeyGenerator.generateCompressedPublicKey(privateKey);

        // keccak256(compressedPubKey) -> 32 bytes
        byte[] k = Hash.KECCAK_256(publicKeyCompressed);

        // last 20 bytes (160 bits)
        byte[] payload20 = Arrays.copyOfRange(k, 12, 32);

        // Base58Check(version || payload20 || checksum4)
        String address = Base58Check.encode(VERSION, payload20);

        return new Wallet(
                privateKey,
                publicKeyCompressed,
                address,
                k,
                payload20,
                VERSION,
                usedMnemonic
        );
    }

    /** Save wallet data to JSON file (plain JSON, no extra libraries) */
    public static void saveToJsonFile(Wallet wallet, Path filePath) {
        try {
            Files.writeString(filePath, wallet.toJson(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON file: " + filePath, e);
        }
    }

    // ----------------- Wallet DTO -----------------

    public static class Wallet {
        public final byte[] privateKey;
        public final byte[] publicKeyCompressed;
        public final String addressBase58;

        public final byte[] keccak256OfPub;
        public final byte[] payload20;
        public final byte version;

        // optional
        public final String mnemonic; // null if random key

        public Wallet(byte[] privateKey,
                      byte[] publicKeyCompressed,
                      String addressBase58,
                      byte[] keccak256OfPub,
                      byte[] payload20,
                      byte version,
                      String mnemonic) {
            this.privateKey = privateKey;
            this.publicKeyCompressed = publicKeyCompressed;
            this.addressBase58 = addressBase58;
            this.keccak256OfPub = keccak256OfPub;
            this.payload20 = payload20;
            this.version = version;
            this.mnemonic = mnemonic;
        }

        public String privateKeyHex() { return ByteToHex.BytesToHex(privateKey); }
        public String publicKeyCompressedHex() { return ByteToHex.BytesToHex(publicKeyCompressed); }
        public String keccakHex() { return ByteToHex.BytesToHex(keccak256OfPub); }
        public String payload20Hex() { return ByteToHex.BytesToHex(payload20); }
        public String versionHex() { return String.format("%02x", version); }

        public String toJson() {
            return "{\n" +
                    "  \"versionHex\": \"" + esc(versionHex()) + "\",\n" +
                    "  \"mnemonic\": " + (mnemonic == null ? "null" : "\"" + esc(mnemonic) + "\"") + ",\n" +
                    "  \"privateKeyHex\": \"" + esc(privateKeyHex()) + "\",\n" +
                    "  \"publicKeyCompressedHex\": \"" + esc(publicKeyCompressedHex()) + "\",\n" +
                    "  \"keccak256Hex\": \"" + esc(keccakHex()) + "\",\n" +
                    "  \"payload20Hex\": \"" + esc(payload20Hex()) + "\",\n" +
                    "  \"addressBase58\": \"" + esc(addressBase58) + "\"\n" +
                    "}\n";
        }
        private static String esc(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
