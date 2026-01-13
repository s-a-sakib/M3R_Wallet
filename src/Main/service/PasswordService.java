package Main.service;

import Main.Util.Hash.Hash;

import java.security.SecureRandom;
import java.util.Base64;

public class PasswordService {
    public String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public String hashPassword(String password, String saltBase64) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        byte[] passwordBytes = password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] combined = new byte[salt.length + passwordBytes.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(passwordBytes, 0, combined, salt.length, passwordBytes.length);
        return Base64.getEncoder().encodeToString(Hash.SHA_256(combined));
    }

    public boolean verify(String password, String saltBase64, String expectedHashBase64) {
        String actual = hashPassword(password, saltBase64);
        return actual.equals(expectedHashBase64);
    }
}
