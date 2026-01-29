package Main.Test;

import Main.Transaction.TxSchema;
import Main.Util.Hash.Hash;
import Main.Util.KeyAddressGen.M3RAddressFactory;
import Main.WalletNet.WalletNetwork;

import java.nio.charset.StandardCharsets;

public class WalletNetFullDemo {

    // Your genesis mnemonic (funded in genesis_state.json)
    private static final String GENESIS_MNEMONIC =
            "16TH APRIL 2000 MY NAME IS SHAHREAR AL SAKIB FATHER OF M3RCOIN";

    public static void main(String[] args) throws Exception {

        // ------------------ Setup ------------------
        M3RAddressFactory.Wallet sender = M3RAddressFactory.generate(GENESIS_MNEMONIC);
        M3RAddressFactory.Wallet receiver = M3RAddressFactory.generate("");

        WalletNetwork.ValidatorConfig cfg = new WalletNetwork.ValidatorConfig(
                "http://127.0.0.1:8080",
                5000,
                8000
        );

        WalletNetwork net = new WalletNetwork(cfg, TxSchema.ChainID.TESTNET);

        System.out.println("=== WALLETS ===");
        System.out.println("[Sender]   " + sender.addressBase58);
        System.out.println("[Receiver] " + receiver.addressBase58);

        // ------------------ Check Fee Policy ------------------
        WalletNetwork.FeePolicy fp = net.getFeePolicy();
        System.out.println("\n=== FEE POLICY ===");
        System.out.println("broadcastFee=" + fp.broadcastFee + " percentFeeBps=" + fp.percentFeeBps);

        // ------------------ Check Balances ------------------
        printBalances(net, sender, receiver);

        // ======================================================
        // 1) TRANSFER TEST
        // ======================================================
        long transferAmount = 50_000;
        System.out.println("\n=== TEST 1: TRANSFER " + transferAmount + " CORE ===");

        WalletNetwork.SubmitResult transferRes = net.sendTransfer(
                sender,
                receiver.payload20,
                transferAmount,
                "hello-transfer".getBytes(StandardCharsets.UTF_8)
        );

        System.out.println("Submit: status=" + transferRes.status + " txHash=" + transferRes.txHash + " msg=" + transferRes.message);

        if (transferRes.txHash != null) {
            WalletNetwork.TxStatusResult st = net.getTxStatus(transferRes.txHash);
            System.out.println("Status: " + st.status + " msg=" + st.message);
        }

        printBalances(net, sender, receiver);

        // ======================================================
        // 2) ARBITER REQUEST (OFF-CHAIN HTTP)
        // ======================================================
        System.out.println("\n=== TEST 2: REQUEST ARBITER (off-chain) ===");

        try {
            WalletNetwork.ArbiterResult arb = net.requestArbiter(
                    sender.addressBase58,
                    receiver.addressBase58,
                    "TIME_LOCK",
                    "need an arbiter for escrow demo"
            );

            System.out.println("Arbiter: ok=" + arb.ok + " arbiter=" + arb.arbiterAddressBase58 + " msg=" + arb.message);
        } catch (Exception e) {
            System.out.println("Arbiter request failed (maybe endpoint not implemented yet): " + e.getMessage());
        }

        // ======================================================
        // 3) ESCROW CREATE (ON-CHAIN TxV1)
        // ======================================================
        System.out.println("\n=== TEST 3: ESCROW CREATE (on-chain) ===");

        // Create a deterministic escrowId32 (for demo)
        // escrowId32 = keccak256("sender|receiver|timestamp")
        byte[] escrowId32 = Hash.KECCAK_256(
                (sender.addressBase58 + "|" + receiver.addressBase58 + "|" + (System.currentTimeMillis()/1000L))
                        .getBytes(StandardCharsets.UTF_8)
        );

        // Arbiter: for demo, we use receiver as arbiter or create a new arbiter wallet.
        // Better: use the returned arbiter from /arbiter/request when your server supports it.
        M3RAddressFactory.Wallet arbiter = M3RAddressFactory.generate("");
        System.out.println("Arbiter (demo): " + arbiter.addressBase58);

        long escrowAmount = 20_000;
        long expiryTsSec = (System.currentTimeMillis() / 1000L) + 3600; // 1 hour from now

        int releaseModeU8 = 1; // your enum mapping
        int disputeModeU8 = 1; // your enum mapping

        // metaHash32 could be keccak256 of invoice / order info / memo number etc
        byte[] metaHash32 = Hash.KECCAK_256("order#12345".getBytes(StandardCharsets.UTF_8));

        WalletNetwork.SubmitResult escCreate = net.sendEscrowCreate(
                sender,
                escrowId32,
                sender.payload20,          // buyer20 (demo: sender as buyer)
                receiver.payload20,        // seller20
                arbiter.payload20,         // arbiter20
                escrowAmount,
                expiryTsSec,
                releaseModeU8,
                disputeModeU8,
                metaHash32,
                "escrow-create".getBytes(StandardCharsets.UTF_8)
        );

        System.out.println("EscrowCreate Submit: status=" + escCreate.status + " txHash=" + escCreate.txHash + " msg=" + escCreate.message);

        if (escCreate.txHash != null) {
            WalletNetwork.TxStatusResult st = net.getTxStatus(escCreate.txHash);
            System.out.println("EscrowCreate Status: " + st.status + " msg=" + st.message);
        }

        // balances may change only if validator supports escrow execution
        printBalances(net, sender, receiver);

        System.out.println("\n=== DEMO COMPLETE ===");
        System.out.println("If ESCROW was REJECTED, implement escrow execution on validator next.");
    }

    private static void printBalances(WalletNetwork net, M3RAddressFactory.Wallet sender, M3RAddressFactory.Wallet receiver) throws Exception {
        WalletNetwork.AccountInfo s = net.getAccount(sender.payload20, sender.addressBase58);
        WalletNetwork.AccountInfo r = net.getAccount(receiver.payload20, receiver.addressBase58);

        System.out.println("\n--- BALANCES ---");
        System.out.println("Sender:   balance=" + s.balance + " nonce=" + s.nonce);
        System.out.println("Receiver: balance=" + r.balance + " nonce=" + r.nonce);
    }
}
