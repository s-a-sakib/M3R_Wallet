package Main.Transaction;

import Main.Util.KeyAddressGen.M3RAddressFactory;

public final class WalletTxFactory {

    private WalletTxFactory() {}

    /** fromAddr20 comes from wallet.payload20 (NOT Base58 string). */
    public static byte[] fromWallet(M3RAddressFactory.Wallet wallet) {
        if (wallet == null) throw new IllegalArgumentException("wallet is null");
        if (wallet.payload20 == null || wallet.payload20.length != TxV1.ADDRESS_LEN)
            throw new IllegalArgumentException("wallet.payload20 must be 20 bytes");
        return wallet.payload20.clone();
    }

    /** Create + sign a transfer tx in one call. */
    public static TxV1 createAndSignTransfer(
            M3RAddressFactory.Wallet wallet,
            TxSchema.ChainID chainId,
            long nonce,
            long fee,
            byte[] toAddr20,
            long amount,
            byte[] memo
    ) {
        long now = System.currentTimeMillis() / 1000L;

        TxV1 tx = TxBuilder.transfer(
                TxSchema.Version.ALPHA,
                chainId,
                nonce,
                fee,
                now,
                fromWallet(wallet),
                toAddr20,
                amount,
                memo
        );

        tx.signSecp256k1(wallet.privateKey);
        return tx;
    }
}
