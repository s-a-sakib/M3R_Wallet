package Main.Transaction;

import Main.Transaction.Payload.*;

public final class TxBuilder {

    private TxBuilder() {}

    public static final byte SIG_SCHEME_ECDSA_SECP256K1 = 1;

    public static TxV1 transfer(
            TxSchema.Version version,
            TxSchema.ChainID chainId,
            long nonce,
            long fee,
            long timestampSec,
            byte[] fromAddr20,
            byte[] toAddr20,
            long amount,
            byte[] memo
    ) {
        byte[] payload = TransferPayload.encode(toAddr20, amount);
        return new TxV1(version, chainId, TxSchema.TxType.TRANSFER, nonce, fee, timestampSec, fromAddr20, payload, memo, SIG_SCHEME_ECDSA_SECP256K1);
    }

    public static TxV1 escrowCreate(
            TxSchema.Version version,
            TxSchema.ChainID chainId,
            long nonce,
            long fee,
            long timestampSec,
            byte[] fromAddr20,
            byte[] escrowId32,
            byte[] buyer20,
            byte[] seller20,
            byte[] arbiter20,
            long amount,
            long expiryTsSec,
            int releaseModeU8,
            int disputeModeU8,
            byte[] metaHash32,
            byte[] memo
    ) {
        byte[] payload = EscrowCreatePayload.encode(
                escrowId32, buyer20, seller20, arbiter20, amount, expiryTsSec, releaseModeU8, disputeModeU8, metaHash32
        );
        return new TxV1(version, chainId, TxSchema.TxType.ESCROW_CREATE, nonce, fee, timestampSec, fromAddr20, payload, memo, SIG_SCHEME_ECDSA_SECP256K1);
    }

    public static TxV1 escrowRelease(
            TxSchema.Version version,
            TxSchema.ChainID chainId,
            long nonce,
            long fee,
            long timestampSec,
            byte[] fromAddr20,
            byte[] escrowId32,
            byte[] toAddr20,
            long amount,
            byte[] memo
    ) {
        byte[] payload = EscrowReleasePayload.encode(escrowId32, toAddr20, amount);
        return new TxV1(version, chainId, TxSchema.TxType.ESCROW_RELEASE, nonce, fee, timestampSec, fromAddr20, payload, memo, SIG_SCHEME_ECDSA_SECP256K1);
    }

    public static TxV1 escrowRefund(
            TxSchema.Version version,
            TxSchema.ChainID chainId,
            long nonce,
            long fee,
            long timestampSec,
            byte[] fromAddr20,
            byte[] escrowId32,
            byte[] toAddr20,
            long amount,
            byte[] memo
    ) {
        byte[] payload = EscrowRefundPayload.encode(escrowId32, toAddr20, amount);
        return new TxV1(version, chainId, TxSchema.TxType.ESCROW_REFUND, nonce, fee, timestampSec, fromAddr20, payload, memo, SIG_SCHEME_ECDSA_SECP256K1);
    }
}
