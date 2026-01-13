package Main.model;

import java.time.Instant;

public class WalletProfile {
    private final byte[] privateKey;
    private final byte[] publicKey;
    private final String address;
    private final String base58PublicKey;
    private final String passwordSalt;
    private final String passwordHash;
    private final Instant createdAt;

    public WalletProfile(byte[] privateKey,
                         byte[] publicKey,
                         String address,
                         String base58PublicKey,
                         String passwordSalt,
                         String passwordHash,
                         Instant createdAt) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.address = address;
        this.base58PublicKey = base58PublicKey;
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public String getAddress() {
        return address;
    }

    public String getBase58PublicKey() {
        return base58PublicKey;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
