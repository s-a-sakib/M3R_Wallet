package Main.model;

public class Validator {
    private final String url;
    private final String address;
    private final String publicKey;
    private final long fee;

    public Validator(String url, String address, String publicKey, long fee) {
        this.url = url;
        this.address = address;
        this.publicKey = publicKey;
        this.fee = fee;
    }

    public String getUrl() {
        return url;
    }

    public String getAddress() {
        return address;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public long getFee() {
        return fee;
    }

    @Override
    public String toString() {
        return url + " (fee " + fee + " qwei)";
    }
}
