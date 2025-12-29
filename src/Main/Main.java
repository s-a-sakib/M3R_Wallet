package Main;

import Main.Util.Signature.*;
import Main.Util.ByteToHex;
public class Main {

    public static  void  main(String[] args){
        String words = "";
        byte[] privateKey = PrivateKeyGenerator.randomPrivateKey();
        byte[] ComPressedPublicKey  = PublicKeyGenerator.generateCompressedPublicKey(privateKey);
        System.out.println(ByteToHex.BytesToHex(privateKey));
        System.out.println(ByteToHex.BytesToHex(ComPressedPublicKey));
    }
}
