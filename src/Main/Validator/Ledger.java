package Main.Validator;

import java.util.concurrent.ConcurrentHashMap;

public final class Ledger {

    public static final class Account {
        public long balance;
        public long nonce;
        public Account(long balance, long nonce) {
            this.balance = balance;
            this.nonce = nonce;
        }
    }

    private final ConcurrentHashMap<String, Account> map = new ConcurrentHashMap<>();

    public Account getOrCreate(byte[] addr20) {
        String k = HexUtil.toHex(addr20);
        return map.computeIfAbsent(k, _k -> new Account(0, 0));
    }

    public long balance(byte[] addr20) { return getOrCreate(addr20).balance; }
    public long nonce(byte[] addr20) { return getOrCreate(addr20).nonce; }

    public void faucet(byte[] addr20, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        getOrCreate(addr20).balance += amount;
    }
}
