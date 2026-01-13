package Main.service;

import Main.Util.Base58;
import Main.Util.ByteToHex;
import Main.Util.Hash.Hash;
import Main.Util.Signature.PrivateKeyGenerator;
import Main.Util.Signature.PublicKeyGenerator;
import Main.model.WalletProfile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

public class WalletService {
    private final PasswordService passwordService;

    public WalletService(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    public WalletProfile createNewWallet(String password) {
        byte[] privateKey = PrivateKeyGenerator.randomPrivateKey();
        return buildProfile(privateKey, password);
    }

    public WalletProfile importWallet(String keyMaterial, String password) {
        byte[] privateKey;
        if (keyMaterial.trim().contains(" ")) {
            privateKey = PrivateKeyGenerator.privateKeyFromMnemonic(keyMaterial.trim());
        } else {
            privateKey = hexToBytes(keyMaterial.trim());
        }
        return buildProfile(privateKey, password);
    }

    public boolean verifyPassword(WalletProfile profile, String password) {
        return passwordService.verify(password, profile.getPasswordSalt(), profile.getPasswordHash());
    }

    private WalletProfile buildProfile(byte[] privateKey, String password) {
        byte[] publicKey = PublicKeyGenerator.generateCompressedPublicKey(privateKey);
        String base58PublicKey = Base58.encode(publicKey);
        String address = deriveAddress(publicKey);
        String salt = passwordService.generateSalt();
        String passwordHash = passwordService.hashPassword(password, salt);
        return new WalletProfile(
                privateKey,
                publicKey,
                address,
                base58PublicKey,
                salt,
                passwordHash,
                Instant.now()
        );
    }

    public String privateKeyHex(WalletProfile profile) {
        return ByteToHex.BytesToHex(profile.getPrivateKey());
    }

    public String publicKeyHex(WalletProfile profile) {
        return ByteToHex.BytesToHex(profile.getPublicKey());
    }

    public String deriveAddress(byte[] publicKey) {
        byte[] sha256 = Hash.SHA_256(publicKey);
        byte[] sha3 = Hash.SHA3_256(sha256);
        String base58 = Base58.encode(sha3);
        byte[] base58Bytes = base58.getBytes(StandardCharsets.UTF_8);
        byte[] addressBytes = ensureLength(base58Bytes, 20);
        return "M3R" + ByteToHex.BytesToHex(addressBytes).toUpperCase();
    }

    public String exportPrivateKeyBase64(WalletProfile profile) {
        return Base64.getEncoder().encodeToString(profile.getPrivateKey());
    }

    private byte[] ensureLength(byte[] input, int length) {
        byte[] output = new byte[length];
        int copyLength = Math.min(input.length, length);
        System.arraycopy(input, 0, output, 0, copyLength);
        if (copyLength < length) {
            Arrays.fill(output, copyLength, length, (byte) 0);
        }
        return output;
    }

    private byte[] hexToBytes(String hex) {
        String clean = hex.replace("0x", "").replace("0X", "");
        if (clean.length() % 2 != 0) {
            throw new IllegalArgumentException("Private key hex length must be even");
        }
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < clean.length(); i += 2) {
            out[i / 2] = (byte) Integer.parseInt(clean.substring(i, i + 2), 16);
        }
        return out;
    }
}
