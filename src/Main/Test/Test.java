package Main.Test;

import Main.Transaction.TxSchema;
import Main.Util.KeyAddressGen.M3RAddressFactory;
import Main.WalletNet.WalletNetwork;

import java.nio.charset.StandardCharsets;

public class  Test {
    // ==================== Demo (console test) ====================

    public static void main(String[] args) throws Exception {

        // Use your funded mnemonic so account has balance on validator
        String mnemonic = "16TH APRIL 2000 MY NAME IS SHAHREAR AL SAKIB FATHER OF M3RCOIN";

        M3RAddressFactory.Wallet sender = M3RAddressFactory.generate(mnemonic);
        M3RAddressFactory.Wallet receiver = M3RAddressFactory.generate("");

        WalletNetwork.ValidatorConfig cfg = new WalletNetwork.ValidatorConfig("http://127.0.0.1:8080", 5000, 8000);
        WalletNetwork net = new WalletNetwork(cfg, TxSchema.ChainID.TESTNET);

        System.out.println("Sender:   " + sender.addressBase58);
        System.out.println("Receiver: " + receiver.addressBase58);

        WalletNetwork.FeePolicy fp = net.getFeePolicy();
        System.out.println("FeePolicy: broadcastFee=" + fp.broadcastFee + " bps=" + fp.percentFeeBps);

        WalletNetwork.AccountInfo ai = net.getAccount(sender.payload20, sender.addressBase58);
        System.out.println("Account: balance=" + ai.balance + " nonce=" + ai.nonce);

        long amount = 50_0000;
        WalletNetwork.SubmitResult r = net.sendTransfer(sender, receiver.payload20, amount, "hello".getBytes(StandardCharsets.UTF_8));
        System.out.println("Submit: status=" + r.status + " txHash=" + r.txHash + " msg=" + r.message);

        if (r.txHash != null) {
            WalletNetwork.TxStatusResult st = net.getTxStatus(r.txHash);
            System.out.println("Status: " + st.status + " msg=" + st.message);
        }
    }
}
