package Main.Transaction;

public final class TxSchema {

    // Never use enum ordinal() for anything consensus-related.
    public enum Version {
        ALPHA(1); // start at 1 for clarity (you can change)

        public final int code;
        Version(int code) { this.code = code; }
    }

    public enum ChainID {
        MAINNET(1),
        TESTNET(2);

        public final int id;
        ChainID(int id) { this.id = id; }
    }

    public enum TxType {
        TRANSFER(0),
        ESCROW_CREATE(1),
        ESCROW_RELEASE(2),
        ESCROW_REFUND(3);

        public final int code;
        TxType(int code) { this.code = code; }
    }

    private TxSchema() {}
}
