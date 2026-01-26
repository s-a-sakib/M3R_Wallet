package Main.Util.Hash;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import org.bouncycastle.jcajce.provider.digest.Keccak;

public class Hash {
    public static byte[] SHA_256(byte[] text){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(text);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public static byte[] SHA_256(String text){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public static byte[] SHA_512(byte[] text){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            return md.digest(text);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public static byte[] SHA_512(String text){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            return md.digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public static byte[] KECCAK_256(String text) {
        try{
            Keccak.Digest256 digest = new Keccak.Digest256();
            return digest.digest(text.getBytes(StandardCharsets.UTF_8));
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static byte[] KECCAK_256(byte[] text) {
        try{
            Keccak.Digest256 digest = new Keccak.Digest256();
            return digest.digest(text);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
