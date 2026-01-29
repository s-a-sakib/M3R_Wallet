package Main.WalletNet;

import Main.Transaction.TxBuilder;
import Main.Transaction.TxSchema;
import Main.Transaction.TxV1;
import Main.Util.KeyAddressGen.M3RAddressFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * WalletNetwork v2
 * - Wallet <-> Validator HTTP bridge (typed DTOs)
 * - Supports TxV1 broadcast for:
 *    TRANSFER, ESCROW_CREATE, ESCROW_RELEASE, ESCROW_REFUND
 * - Supports optional arbiter API endpoints (non-tx):
 *    /arbiter/request, /arbiter/register, /arbiter/list
 */
public final class WalletNetwork {

    // ==================== Config ====================

    public static final class ValidatorConfig {
        public final String baseUrl; // "http://127.0.0.1:8080" or "https://..."
        public final int connectTimeoutMs;
        public final int readTimeoutMs;

        public ValidatorConfig(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
            if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("baseUrl empty");
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            this.connectTimeoutMs = connectTimeoutMs;
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    // ==================== DTOs ====================

    public enum TxSubmitStatus { ACCEPTED, REJECTED }
    public enum TxState { PENDING, CONFIRMED, REJECTED, UNKNOWN }

    public static final class FeePolicy {
        public final long broadcastFee;
        public final int percentFeeBps; // 1% = 100 bps

        public FeePolicy(long broadcastFee, int percentFeeBps) {
            this.broadcastFee = broadcastFee;
            this.percentFeeBps = percentFeeBps;
        }

        /** fee = broadcastFee + (amount * bps / 10000) */
        public long feeForAmount(long amount) {
            if (amount < 0) throw new IllegalArgumentException("amount < 0");
            long pct = (amount * (long) percentFeeBps) / 10_000L;
            return broadcastFee + pct;
        }
    }

    public static final class AccountInfo {
        public final long balance;
        public final long nonce;
        public AccountInfo(long balance, long nonce) {
            this.balance = balance;
            this.nonce = nonce;
        }
    }

    public static final class SubmitResult {
        public final TxSubmitStatus status;
        public final String txHash;     // null if rejected
        public final String message;    // optional
        public SubmitResult(TxSubmitStatus status, String txHash, String message) {
            this.status = status;
            this.txHash = txHash;
            this.message = message;
        }
    }

    public static final class TxStatusResult {
        public final TxState status;
        public final String message;
        public TxStatusResult(TxState status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    // -------- Arbiter DTOs (off-chain endpoints) --------

    public static final class ArbiterInfo {
        public final String addressBase58;
        public final String endpoint; // http/https URL (optional)
        public final long stake;      // optional
        public ArbiterInfo(String addressBase58, String endpoint, long stake) {
            this.addressBase58 = addressBase58;
            this.endpoint = endpoint;
            this.stake = stake;
        }
    }

    public static final class ArbiterResult {
        public final boolean ok;
        public final String arbiterAddressBase58;
        public final String message;
        public ArbiterResult(boolean ok, String arbiterAddressBase58, String message) {
            this.ok = ok;
            this.arbiterAddressBase58 = arbiterAddressBase58;
            this.message = message;
        }
    }

    // ==================== Fields ====================

    private final ValidatorConfig cfg;
    private final TxSchema.ChainID chainId;

    public WalletNetwork(ValidatorConfig cfg, TxSchema.ChainID chainId) {
        this.cfg = cfg;
        this.chainId = chainId;
    }

    // ==================== Core API ====================

    /** GET /fee */
    public FeePolicy getFeePolicy() throws IOException {
        String json = httpGet(cfg.baseUrl + "/fee");
        Long broadcastFee = Json.getLong(json, "broadcastFee");
        Long bps = Json.getLong(json, "percentFeeBps");
        if (broadcastFee == null || bps == null) throw new IOException("Bad /fee response: " + json);
        return new FeePolicy(broadcastFee, bps.intValue());
    }

    /**
     * GET /account
     * Tries:
     *  1) /account?addr=<payload20hex>
     *  2) /account?addr=0x<payload20hex>
     *  3) /account?address=<base58>
     */
    public AccountInfo getAccount(byte[] addr20, String base58IfAny) throws IOException {
        if (addr20 == null || addr20.length != 20) throw new IllegalArgumentException("addr20 must be 20 bytes");

        IOException last = null;

        try { return parseAccount(httpGet(cfg.baseUrl + "/account?addr=" + Hex.hex(addr20))); }
        catch (IOException e) { last = e; }

        try { return parseAccount(httpGet(cfg.baseUrl + "/account?addr=0x" + Hex.hex(addr20))); }
        catch (IOException e) { last = e; }

        if (base58IfAny != null && !base58IfAny.isBlank()) {
            try {
                String enc = urlEncode(base58IfAny.trim());
                return parseAccount(httpGet(cfg.baseUrl + "/account?address=" + enc));
            } catch (IOException e) { last = e; }
        }

        throw new IOException("Cannot fetch account. Last error: " +
                (last != null ? last.getMessage() : "unknown"), last);
    }

    private AccountInfo parseAccount(String json) throws IOException {
        Long bal = Json.getLong(json, "balance");
        Long nonce = Json.getLong(json, "nonce");
        if (bal == null || nonce == null) throw new IOException("Bad /account response: " + json);
        return new AccountInfo(bal, nonce);
    }

    /** POST /tx/submit */
    public SubmitResult submitTx(TxV1 tx, byte[] pubKeyCompressed) throws IOException {
        if (tx == null) throw new IllegalArgumentException("tx null");
        if (pubKeyCompressed == null || pubKeyCompressed.length == 0) throw new IllegalArgumentException("pubKeyCompressed null");

        String rawTxHex = Hex.hex(tx.encodeFull());
        String pubKeyHex = Hex.hex(pubKeyCompressed);

        String req = "{"
                + "\"rawTxHex\":\"" + rawTxHex + "\","
                + "\"pubKeyCompressedHex\":\"" + pubKeyHex + "\""
                + "}";

        String resp = httpPost(cfg.baseUrl + "/tx/submit", req);

        String st = Json.getString(resp, "status");
        String hash = Json.getString(resp, "txHash");
        String msg = Json.getString(resp, "message");

        TxSubmitStatus status = "ACCEPTED".equalsIgnoreCase(st) ? TxSubmitStatus.ACCEPTED : TxSubmitStatus.REJECTED;
        return new SubmitResult(status, hash, msg);
    }

    /** GET /tx/status?hash=... */
    public TxStatusResult getTxStatus(String txHashHex) throws IOException {
        if (txHashHex == null || txHashHex.isBlank()) throw new IllegalArgumentException("txHashHex empty");

        String resp = httpGet(cfg.baseUrl + "/tx/status?hash=" + urlEncode(txHashHex.trim()));
        String st = Json.getString(resp, "status");
        String msg = Json.getString(resp, "message");

        TxState state;
        if (st == null) state = TxState.UNKNOWN;
        else {
            switch (st.toUpperCase(Locale.ROOT)) {
                case "PENDING" -> state = TxState.PENDING;
                case "CONFIRMED" -> state = TxState.CONFIRMED;
                case "REJECTED" -> state = TxState.REJECTED;
                default -> state = TxState.UNKNOWN;
            }
        }
        return new TxStatusResult(state, msg);
    }

    // ==================== Convenience: Transfer ====================

    public SubmitResult sendTransfer(M3RAddressFactory.Wallet sender, byte[] toAddr20, long amount, byte[] memo)
            throws IOException {

        requireWallet(sender);
        requireLen(toAddr20, 20, "toAddr20");
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");

        FeePolicy feePolicy = getFeePolicy();
        long fee = feePolicy.feeForAmount(amount);

        AccountInfo acc = getAccount(sender.payload20, sender.addressBase58);
        long nonce = acc.nonce + 1;

        long required = amount + fee;
        if (acc.balance < required) throw new IOException("Insufficient funds: need " + required + " have " + acc.balance);

        long nowSec = System.currentTimeMillis() / 1000L;

        TxV1 tx = TxBuilder.transfer(
                TxSchema.Version.ALPHA,
                chainId,
                nonce,
                fee,
                nowSec,
                sender.payload20,
                toAddr20,
                amount,
                memo
        );

        tx.signSecp256k1(sender.privateKey);
        return submitTx(tx, sender.publicKeyCompressed);
    }

    // ==================== Convenience: Escrow TxV1 ====================

    public SubmitResult sendEscrowCreate(
            M3RAddressFactory.Wallet sender,
            byte[] escrowId32,
            byte[] buyer20,
            byte[] seller20,
            byte[] arbiter20,
            long amount,
            long expiryTsSec,
            int releaseModeU8,
            int disputeModeU8,
            byte[] metaHash32,
            byte[] memo
    ) throws IOException {

        requireWallet(sender);
        requireLen(escrowId32, 32, "escrowId32");
        requireLen(buyer20, 20, "buyer20");
        requireLen(seller20, 20, "seller20");
        requireLen(arbiter20, 20, "arbiter20");
        requireLen(metaHash32, 32, "metaHash32");
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        if (expiryTsSec < 0) throw new IllegalArgumentException("expiryTsSec must be >= 0");
        if ((releaseModeU8 & ~0xFF) != 0) throw new IllegalArgumentException("releaseModeU8 must fit u8");
        if ((disputeModeU8 & ~0xFF) != 0) throw new IllegalArgumentException("disputeModeU8 must fit u8");

        FeePolicy feePolicy = getFeePolicy();
        long fee = feePolicy.feeForAmount(amount);

        AccountInfo acc = getAccount(sender.payload20, sender.addressBase58);
        long nonce = acc.nonce + 1;

        long required = amount + fee;
        if (acc.balance < required) throw new IOException("Insufficient funds: need " + required + " have " + acc.balance);

        long nowSec = System.currentTimeMillis() / 1000L;

        TxV1 tx = TxBuilder.escrowCreate(
                TxSchema.Version.ALPHA,
                chainId,
                nonce,
                fee,
                nowSec,
                sender.payload20,
                escrowId32,
                buyer20,
                seller20,
                arbiter20,
                amount,
                expiryTsSec,
                releaseModeU8,
                disputeModeU8,
                metaHash32,
                memo
        );

        tx.signSecp256k1(sender.privateKey);
        return submitTx(tx, sender.publicKeyCompressed);
    }

    public SubmitResult sendEscrowRelease(
            M3RAddressFactory.Wallet sender,
            byte[] escrowId32,
            byte[] toAddr20,
            long amount,
            byte[] memo
    ) throws IOException {

        requireWallet(sender);
        requireLen(escrowId32, 32, "escrowId32");
        requireLen(toAddr20, 20, "toAddr20");
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");

        FeePolicy feePolicy = getFeePolicy();
        long fee = feePolicy.feeForAmount(amount);

        AccountInfo acc = getAccount(sender.payload20, sender.addressBase58);
        long nonce = acc.nonce + 1;

        // Release typically does NOT require sender to cover "amount" from their balance (it comes from escrow),
        // but fee must still be paid. If your chain charges fee from sender, check fee only:
        if (acc.balance < fee) throw new IOException("Insufficient funds for fee: need " + fee + " have " + acc.balance);

        long nowSec = System.currentTimeMillis() / 1000L;

        TxV1 tx = TxBuilder.escrowRelease(
                TxSchema.Version.ALPHA,
                chainId,
                nonce,
                fee,
                nowSec,
                sender.payload20,
                escrowId32,
                toAddr20,
                amount,
                memo
        );

        tx.signSecp256k1(sender.privateKey);
        return submitTx(tx, sender.publicKeyCompressed);
    }

    public SubmitResult sendEscrowRefund(
            M3RAddressFactory.Wallet sender,
            byte[] escrowId32,
            byte[] toAddr20,
            long amount,
            byte[] memo
    ) throws IOException {

        requireWallet(sender);
        requireLen(escrowId32, 32, "escrowId32");
        requireLen(toAddr20, 20, "toAddr20");
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");

        FeePolicy feePolicy = getFeePolicy();
        long fee = feePolicy.feeForAmount(amount);

        AccountInfo acc = getAccount(sender.payload20, sender.addressBase58);
        long nonce = acc.nonce + 1;

        if (acc.balance < fee) throw new IOException("Insufficient funds for fee: need " + fee + " have " + acc.balance);

        long nowSec = System.currentTimeMillis() / 1000L;

        TxV1 tx = TxBuilder.escrowRefund(
                TxSchema.Version.ALPHA,
                chainId,
                nonce,
                fee,
                nowSec,
                sender.payload20,
                escrowId32,
                toAddr20,
                amount,
                memo
        );

        tx.signSecp256k1(sender.privateKey);
        return submitTx(tx, sender.publicKeyCompressed);
    }

    // ==================== Arbiter APIs (non-Tx HTTP) ====================

    /**
     * POST /arbiter/request
     * Request format example:
     * {"buyer":"<base58>","seller":"<base58>","type":"<string>","memo":"..."}
     *
     * Response example:
     * {"status":"OK","arbiterAddress":"<base58>","message":"..."}
     */
    public ArbiterResult requestArbiter(String buyerBase58, String sellerBase58, String type, String memo) throws IOException {
        if (buyerBase58 == null || buyerBase58.isBlank()) throw new IllegalArgumentException("buyer empty");
        if (sellerBase58 == null || sellerBase58.isBlank()) throw new IllegalArgumentException("seller empty");
        if (type == null || type.isBlank()) throw new IllegalArgumentException("type empty");

        String req = "{"
                + "\"buyer\":\"" + Json.escape(buyerBase58.trim()) + "\","
                + "\"seller\":\"" + Json.escape(sellerBase58.trim()) + "\","
                + "\"type\":\"" + Json.escape(type.trim()) + "\","
                + "\"memo\":\"" + Json.escape(memo == null ? "" : memo) + "\""
                + "}";

        String resp = httpPost(cfg.baseUrl + "/arbiter/request", req);

        String st = Json.getString(resp, "status");
        String arb = Json.getString(resp, "arbiterAddress");
        String msg = Json.getString(resp, "message");
        boolean ok = "OK".equalsIgnoreCase(st) || "ACCEPTED".equalsIgnoreCase(st);

        return new ArbiterResult(ok, arb, msg);
    }

    /**
     * POST /arbiter/register
     * {"address":"<base58>","endpoint":"https://...","stake":12345,"signature":"...optional..."}
     *
     * Response:
     * {"status":"OK","message":"..."}
     */
    public ArbiterResult registerArbiter(String addressBase58, String endpointUrl, long stake) throws IOException {
        if (addressBase58 == null || addressBase58.isBlank()) throw new IllegalArgumentException("address empty");
        if (endpointUrl == null) endpointUrl = "";

        String req = "{"
                + "\"address\":\"" + Json.escape(addressBase58.trim()) + "\","
                + "\"endpoint\":\"" + Json.escape(endpointUrl.trim()) + "\","
                + "\"stake\":" + stake
                + "}";

        String resp = httpPost(cfg.baseUrl + "/arbiter/register", req);

        String st = Json.getString(resp, "status");
        String msg = Json.getString(resp, "message");
        boolean ok = "OK".equalsIgnoreCase(st) || "ACCEPTED".equalsIgnoreCase(st);

        return new ArbiterResult(ok, null, msg);
    }

    /**
     * GET /arbiter/list
     * Response example:
     * {"status":"OK","arbiters":[{"address":"...","endpoint":"...","stake":123}, ...]}
     *
     * If your server doesn't support this yet, implement later.
     */
    public List<ArbiterInfo> listArbiters() throws IOException {
        String resp = httpGet(cfg.baseUrl + "/arbiter/list");

        // Minimal parsing: we only extract repeated "address","endpoint","stake" triples.
        // Later, you can switch to a real JSON parser library if you want.
        List<ArbiterInfo> out = new ArrayList<>();
        String status = Json.getString(resp, "status");
        if (status == null || (!status.equalsIgnoreCase("OK") && !status.equalsIgnoreCase("ACCEPTED"))) {
            return out;
        }

        // naive scan for objects: "address":"..","endpoint":"..","stake":..
        int idx = 0;
        while (true) {
            int a = resp.indexOf("\"address\"", idx);
            if (a < 0) break;
            String addr = Json.getStringFromIndex(resp, "address", a);
            String ep = Json.getStringFromIndex(resp, "endpoint", a);
            Long stake = Json.getLongFromIndex(resp, "stake", a);
            if (addr != null) out.add(new ArbiterInfo(addr, ep == null ? "" : ep, stake == null ? 0 : stake));
            idx = a + 9;
        }
        return out;
    }

    // ==================== HTTP ====================

    private String httpGet(String urlStr) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
        con.setConnectTimeout(cfg.connectTimeoutMs);
        con.setReadTimeout(cfg.readTimeoutMs);
        con.setRequestMethod("GET");

        int code = con.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
        byte[] resp = (is == null) ? new byte[0] : is.readAllBytes();
        String body = new String(resp, StandardCharsets.UTF_8);

        if (code < 200 || code >= 300) throw new IOException("HTTP " + code + " " + body);
        return body;
    }

    private String httpPost(String urlStr, String json) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
        con.setConnectTimeout(cfg.connectTimeoutMs);
        con.setReadTimeout(cfg.readTimeoutMs);

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        con.setDoOutput(true);

        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = con.getOutputStream()) {
            os.write(payload);
        }

        int code = con.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
        byte[] resp = (is == null) ? new byte[0] : is.readAllBytes();
        String body = new String(resp, StandardCharsets.UTF_8);

        if (code < 200 || code >= 300) throw new IOException("HTTP " + code + " " + body);
        return body;
    }

    // ==================== Helpers ====================

    private static String urlEncode(String s) {
        try { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }

    private static void requireWallet(M3RAddressFactory.Wallet w) {
        if (w == null) throw new IllegalArgumentException("wallet null");
        if (w.privateKey == null || w.privateKey.length != 32) throw new IllegalArgumentException("wallet privateKey invalid");
        if (w.publicKeyCompressed == null || w.publicKeyCompressed.length == 0) throw new IllegalArgumentException("wallet pubKey invalid");
        if (w.payload20 == null || w.payload20.length != 20) throw new IllegalArgumentException("wallet payload20 invalid");
    }

    private static void requireLen(byte[] b, int len, String name) {
        if (b == null) throw new IllegalArgumentException(name + " null");
        if (b.length != len) throw new IllegalArgumentException(name + " must be " + len + " bytes");
    }

    // ==================== Tiny JSON + Hex ====================

    static final class Json {
        static Long getLong(String json, String key) {
            if (json == null) return null;
            String needle = "\"" + key + "\"";
            int i = json.indexOf(needle);
            if (i < 0) return null;
            int colon = json.indexOf(':', i + needle.length());
            if (colon < 0) return null;

            int start = colon + 1;
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;

            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;

            if (end <= start) return null;
            try { return Long.parseLong(json.substring(start, end)); }
            catch (Exception e) { return null; }
        }

        static String getString(String json, String key) {
            if (json == null) return null;
            String needle = "\"" + key + "\"";
            int i = json.indexOf(needle);
            if (i < 0) return null;
            int colon = json.indexOf(':', i + needle.length());
            if (colon < 0) return null;

            int q1 = json.indexOf('"', colon + 1);
            if (q1 < 0) return null;
            int q2 = json.indexOf('"', q1 + 1);
            if (q2 < 0) return null;
            return json.substring(q1 + 1, q2);
        }

        // Helpers for listArbiters scanning
        static String getStringFromIndex(String json, String key, int startIndex) {
            int i = json.indexOf("\"" + key + "\"", startIndex);
            if (i < 0) return null;
            return getString(json.substring(i), key);
        }
        static Long getLongFromIndex(String json, String key, int startIndex) {
            int i = json.indexOf("\"" + key + "\"", startIndex);
            if (i < 0) return null;
            return getLong(json.substring(i), key);
        }

        static String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    static final class Hex {
        static String hex(byte[] b) {
            StringBuilder sb = new StringBuilder(b.length * 2);
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        }
    }
}
