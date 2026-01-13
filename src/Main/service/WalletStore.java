package Main.service;

import Main.model.Contact;
import Main.model.WalletProfile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

public class WalletStore {
    private final Path storePath;

    public WalletStore() {
        this.storePath = Paths.get(System.getProperty("user.home"), ".m3r_wallet.properties");
    }

    public boolean exists() {
        return Files.exists(storePath);
    }

    public WalletProfile loadProfile() throws IOException {
        if (!exists()) {
            return null;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(storePath)) {
            properties.load(inputStream);
        }
        byte[] privateKey = Base64.getDecoder().decode(properties.getProperty("privateKey", ""));
        byte[] publicKey = Base64.getDecoder().decode(properties.getProperty("publicKey", ""));
        String address = properties.getProperty("address", "");
        String base58PublicKey = properties.getProperty("base58PublicKey", "");
        String salt = properties.getProperty("passwordSalt", "");
        String hash = properties.getProperty("passwordHash", "");
        Instant createdAt = Instant.parse(properties.getProperty("createdAt", Instant.now().toString()));

        return new WalletProfile(privateKey, publicKey, address, base58PublicKey, salt, hash, createdAt);
    }

    public List<Contact> loadContacts() throws IOException {
        List<Contact> contacts = new ArrayList<>();
        if (!exists()) {
            return contacts;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(storePath)) {
            properties.load(inputStream);
        }
        int index = 0;
        while (true) {
            String value = properties.getProperty("contact." + index);
            if (value == null) {
                break;
            }
            String[] parts = value.split("\\|", 2);
            String name = parts.length > 0 ? parts[0] : "";
            String address = parts.length > 1 ? parts[1] : "";
            contacts.add(new Contact(name, address));
            index++;
        }
        return contacts;
    }

    public String loadValidatorApiUrl() throws IOException {
        if (!exists()) {
            return "";
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(storePath)) {
            properties.load(inputStream);
        }
        return properties.getProperty("validatorApiUrl", "");
    }

    public void save(WalletProfile profile, List<Contact> contacts, String validatorApiUrl) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("privateKey", Base64.getEncoder().encodeToString(profile.getPrivateKey()));
        properties.setProperty("publicKey", Base64.getEncoder().encodeToString(profile.getPublicKey()));
        properties.setProperty("address", profile.getAddress());
        properties.setProperty("base58PublicKey", profile.getBase58PublicKey());
        properties.setProperty("passwordSalt", profile.getPasswordSalt());
        properties.setProperty("passwordHash", profile.getPasswordHash());
        properties.setProperty("createdAt", profile.getCreatedAt().toString());
        properties.setProperty("validatorApiUrl", validatorApiUrl == null ? "" : validatorApiUrl);

        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            properties.setProperty("contact." + i, contact.getName() + "|" + contact.getAddress());
        }

        try (OutputStream outputStream = Files.newOutputStream(storePath)) {
            properties.store(outputStream, "M3R Wallet profile");
        }
    }
}
