package Main.Test;

import Main.Util.KeyAddressGen.M3RAddressFactory;

import java.nio.file.Path;

public class Test {
    public static void main(String[] args) {
        String mnemoric = "";
        M3RAddressFactory.Wallet w = M3RAddressFactory.generate(mnemoric);

        System.out.println("Private Key: " + w.privateKeyHex());
        System.out.println("Public Key (Compressed): " + w.publicKeyCompressedHex());
        System.out.println("M3R Address: " + w.addressBase58);

        // Save JSON
        M3RAddressFactory.saveToJsonFile(w, Path.of("m3r_wallet.json"));
        System.out.println("Saved: m3r_wallet.json");
    }
}
