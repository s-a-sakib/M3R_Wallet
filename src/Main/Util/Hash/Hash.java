package Main.Util.Hash;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

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
}
