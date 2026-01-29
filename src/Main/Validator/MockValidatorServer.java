package Main.Validator;

import Main.Transaction.TxSchema;
import Main.Transaction.TxV1;
import Main.Util.Hash.Hash;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MockValidatorServer {

    // ===== Fee policy =====
    private static final long BROADCAST_FEE = 1000L;
    private static final int PERCENT_FEE_BPS = 100; // 1%

    // Ledger: key = payload20hex lowercase
    private final Map<String, Account> ledger = new ConcurrentHashMap<>();
    private final Map<String, String> txStatus = new ConcurrentHashMap<>();
    private final Map<String, String> txMessage = new ConcurrentHashMap<>();

    public static class Account {
        public long balance;
        public long nonce; // stored "current nonce used"
        public String addressBase58;

        public Account(long balance, long nonce, String addressBase58) {
            this.balance = balance;
            this.nonce = nonce;
            this.addressBase58 = addressBase58;
        }
    }

    public static void main(String[] args) throws Exception {
        MockValidatorServer s = new MockValidatorServer();
        s.loadGenesis(Path.of("genesis_state.json"));
        s.start(8080);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        server.createContext("/fee", this::handleFee);
        server.createContext("/account", this::handleAccount);
        server.createContext("/tx/submit", this::handleSubmit);
        server.createContext("/tx/status", this::handleStatus);

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("✅ MockValidatorServer listening on http://127.0.0.1:" + port);
        System.out.println("Loaded accounts: " + ledger.size());
    }

    // ----------------- Handlers -----------------

    private void handleFee(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            reply(ex, 405, jsonErr("METHOD_NOT_ALLOWED", "Use GET"));
            return;
        }
        reply(ex, 200, "{"
                + "\"broadcastFee\":" + BROADCAST_FEE + ","
                + "\"percentFeeBps\":" + PERCENT_FEE_BPS
                + "}");
    }

    private void handleAccount(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            reply(ex, 405, jsonErr("METHOD_NOT_ALLOWED", "Use GET"));
            return;
        }

        Map<String, String> q = query(ex.getRequestURI());
        String addrHex = q.get("addr");
        String addrBase58 = q.get("address");

        String key = null;

        if (addrHex != null && !addrHex.isBlank()) {
            key = normalizeHex20(addrHex);
            if (key == null) {
                reply(ex, 400, jsonErr("ERROR", "bad addr hex"));
                return;
            }
        } else if (addrBase58 != null && !addrBase58.isBlank()) {
            key = findByBase58(addrBase58.trim());
            if (key == null) {
                reply(ex, 404, jsonErr("NOT_FOUND", "unknown address"));
                return;
            }
        } else {
            reply(ex, 400, jsonErr("ERROR", "missing addr or address"));
            return;
        }

        Account a = ledger.get(key);
        if (a == null) {
            reply(ex, 200, "{\"balance\":0,\"nonce\":0}");
            return;
        }

        reply(ex, 200, "{"
                + "\"balance\":" + a.balance + ","
                + "\"nonce\":" + a.nonce
                + "}");
    }

    /**
     * REAL execution for TxType.TRANSFER:
     * - decode raw tx
     * - verify signature
     * - ensure pubKey maps to fromAddr20
     * - parse payload => (toAddr20, amount)
     * - apply ledger + confirm
     */
    private void handleSubmit(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            reply(ex, 405, jsonErr("METHOD_NOT_ALLOWED", "Use POST"));
            return;
        }

        String req = readBody(ex);
        String rawTxHex = jsonString(req, "rawTxHex");
        String pubKeyHex = jsonString(req, "pubKeyCompressedHex");

        if (rawTxHex == null || rawTxHex.isBlank()) {
            reply(ex, 400, jsonErr("ERROR", "missing rawTxHex"));
            return;
        }
        if (pubKeyHex == null || pubKeyHex.isBlank()) {
            reply(ex, 400, jsonErr("ERROR", "missing pubKeyCompressedHex"));
            return;
        }

        byte[] rawTx;
        byte[] pubKeyCompressed;
        try {
            rawTx = hexToBytes(rawTxHex);
            pubKeyCompressed = hexToBytes(pubKeyHex);
        } catch (Exception e) {
            reply(ex, 400, jsonErr("ERROR", "bad hex in request"));
            return;
        }

        String txHash = sha256Hex(rawTx);

        TxV1 tx;
        try {
            tx = TxV1.decodeFull(rawTx);
        } catch (Exception e) {
            reject(txHash, "bad tx encoding: " + e.getMessage());
            reply(ex, 400, submitRejected(txHash));
            return;
        }

        // pubkey -> payload20 must match fromAddr20
        byte[] derived20 = payload20FromCompressedPubKey(pubKeyCompressed);
        if (!Arrays.equals(derived20, tx.getFromAddr20())) {
            reject(txHash, "pubkey does not match tx.fromAddr20");
            reply(ex, 400, submitRejected(txHash));
            return;
        }

        // signature verify (your TxV1 already does Keccak256(signingBytes))
        if (!tx.verifySecp256k1(pubKeyCompressed)) {
            reject(txHash, "bad signature");
            reply(ex, 400, submitRejected(txHash));
            return;
        }

        // Only implement TRANSFER for now
        if (tx.getType() != TxSchema.TxType.TRANSFER) {
            reject(txHash, "tx type not supported yet: " + tx.getType());
            reply(ex, 400, submitRejected(txHash));
            return;
        }

        // Parse transfer payload: to20 + amountU64
        TransferPayload p;
        try {
            p = decodeTransferPayload(tx.getPayload());
        } catch (Exception e) {
            reject(txHash, "bad transfer payload: " + e.getMessage());
            reply(ex, 400, submitRejected(txHash));
            return;
        }

        // Fee policy check
        long minFee = BROADCAST_FEE + ((p.amount * (long) PERCENT_FEE_BPS) / 10_000L);
        if (tx.getFee() < minFee) {
            reject(txHash, "fee too low: need >= " + minFee + " got " + tx.getFee());
            reply(ex, 400, submitRejected(txHash));
            return;
        }

        String senderKey = normalizeHex20(bytesToHex(tx.getFromAddr20()));
        String receiverKey = normalizeHex20(bytesToHex(p.toAddr20));
        if (senderKey == null || receiverKey == null) {
            reject(txHash, "bad sender/receiver address");
            reply(ex, 400, submitRejected(txHash));
            return;
        }

        Account sender = ledger.getOrDefault(senderKey, new Account(0, 0, ""));
        Account receiver = ledger.getOrDefault(receiverKey, new Account(0, 0, ""));

        // Nonce rule must match wallet: wallet sends acc.nonce + 1
        long expectedNonce = sender.nonce + 1;
        if (tx.getNonce() != expectedNonce) {
            reject(txHash, "bad nonce: expected " + expectedNonce + " got " + tx.getNonce());
            reply(ex, 400, submitRejected(txHash));
            return;
        }

        long required = p.amount + tx.getFee();
        if (sender.balance < required) {
            reject(txHash, "insufficient funds: need " + required + " have " + sender.balance);
            reply(ex, 400, submitRejected(txHash));
            return;
        }

        // Apply
        sender.balance -= required;
        sender.nonce += 1;
        receiver.balance += p.amount;

        ledger.put(senderKey, sender);
        ledger.put(receiverKey, receiver);

        txStatus.put(txHash, "CONFIRMED");
        txMessage.put(txHash, "applied");

        reply(ex, 200, "{"
                + "\"status\":\"ACCEPTED\","
                + "\"txHash\":\"" + txHash + "\","
                + "\"message\":\"CONFIRMED\""
                + "}");
    }

    private void handleStatus(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            reply(ex, 405, jsonErr("METHOD_NOT_ALLOWED", "Use GET"));
            return;
        }

        Map<String, String> q = query(ex.getRequestURI());
        String hash = q.get("hash");
        if (hash == null || hash.isBlank()) {
            reply(ex, 400, jsonErr("ERROR", "missing hash"));
            return;
        }

        String h = hash.trim();
        String st = txStatus.getOrDefault(h, "UNKNOWN");
        String msg = txMessage.getOrDefault(h, "");

        reply(ex, 200, "{"
                + "\"status\":\"" + esc(st) + "\","
                + "\"message\":\"" + esc(msg) + "\""
                + "}");
    }

    private String submitRejected(String txHash) {
        return "{"
                + "\"status\":\"REJECTED\","
                + "\"txHash\":\"" + esc(txHash) + "\","
                + "\"message\":\"" + esc(txMessage.getOrDefault(txHash, "rejected")) + "\""
                + "}";
    }

    // ----------------- Transfer Payload -----------------

    private record TransferPayload(byte[] toAddr20, long amount) {}

    /**
     * Assumed TRANSFER payload layout:
     *  - toAddr20: 20 bytes
     *  - amount:   U64 big-endian
     */
    private static TransferPayload decodeTransferPayload(byte[] payload) {
        if (payload == null) throw new IllegalArgumentException("payload null");
        if (payload.length != 28) throw new IllegalArgumentException("TransferPayload must be 28 bytes (20 + 8)");

        byte[] to = Arrays.copyOfRange(payload, 0, 20);

        long amount = 0;
        for (int i = 0; i < 8; i++) {
            amount = (amount << 8) | (payload[20 + i] & 0xFFL);
        }

        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        return new TransferPayload(to, amount);
    }

    // ----------------- Genesis loading -----------------

    public void loadGenesis(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("genesis file not found: " + path.toAbsolutePath());
        }
        String json = Files.readString(path, StandardCharsets.UTF_8);

        List<Map<String, String>> accounts = extractObjectsFromAccountsArray(json);
        for (Map<String, String> a : accounts) {
            String payloadHex = a.get("payload20hex");
            String base58 = a.getOrDefault("addressBase58", "");
            String balStr = a.getOrDefault("balance", "0");
            String nonceStr = a.getOrDefault("nonce", "0");

            String key = normalizeHex20(payloadHex);
            if (key == null) continue;

            long bal = parseLongSafe(balStr);
            long nonce = parseLongSafe(nonceStr);

            ledger.put(key, new Account(bal, nonce, base58));
        }
    }

    // ----------------- Address derivation -----------------

    /** payload20 = last 20 bytes of keccak256(compressedPubKey) */
    private static byte[] payload20FromCompressedPubKey(byte[] compressedPubKey) {
        byte[] k = Hash.KECCAK_256(compressedPubKey);
        byte[] out = new byte[20];
        System.arraycopy(k, k.length - 20, out, 0, 20);
        return out;
    }

    private void reject(String txHash, String message) {
        txStatus.put(txHash, "REJECTED");
        txMessage.put(txHash, message);
    }

    // ----------------- Utilities -----------------

    private static void reply(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> query(URI uri) {
        Map<String, String> m = new HashMap<>();
        String q = uri.getRawQuery();
        if (q == null || q.isBlank()) return m;
        for (String part : q.split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String k = urlDecode(part.substring(0, eq));
            String v = urlDecode(part.substring(eq + 1));
            m.put(k, v);
        }
        return m;
    }

    private static String urlDecode(String s) {
        try { return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }

    private static String normalizeHex20(String s) {
        if (s == null) return null;
        String x = s.trim().toLowerCase(Locale.ROOT);
        if (x.startsWith("0x")) x = x.substring(2);
        if (x.length() != 40) return null;
        for (int i = 0; i < x.length(); i++) {
            char c = x.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!ok) return null;
        }
        return x;
    }

    private String findByBase58(String base58) {
        for (Map.Entry<String, Account> e : ledger.entrySet()) {
            if (base58.equals(e.getValue().addressBase58)) return e.getKey();
        }
        return null;
    }

    private static String jsonErr(String status, String msg) {
        return "{"
                + "\"status\":\"" + esc(status) + "\","
                + "\"message\":\"" + esc(msg) + "\""
                + "}";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static long parseLongSafe(String s) {
        try { return Long.parseLong(s.trim()); }
        catch (Exception e) { return 0; }
    }

    private static String jsonString(String json, String key) {
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

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(data);
            StringBuilder sb = new StringBuilder(h.length * 2);
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "00";
        }
    }

    private static byte[] hexToBytes(String s) {
        String x = s.trim().toLowerCase(Locale.ROOT);
        if (x.startsWith("0x")) x = x.substring(2);
        if (x.length() % 2 != 0) throw new IllegalArgumentException("odd hex length");
        byte[] out = new byte[x.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(x.charAt(i * 2), 16);
            int lo = Character.digit(x.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("bad hex");
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static Long jsonLong(String json, String key) {
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

    private static List<Map<String, String>> extractObjectsFromAccountsArray(String json) {
        List<Map<String, String>> out = new ArrayList<>();
        int a = json.indexOf("\"accounts\"");
        if (a < 0) return out;
        int lb = json.indexOf('[', a);
        int rb = json.indexOf(']', lb);
        if (lb < 0 || rb < 0) return out;

        String arr = json.substring(lb + 1, rb);

        String[] objs = arr.split("\\},\\s*\\{");
        for (String o : objs) {
            String obj = o.trim();
            if (!obj.startsWith("{")) obj = "{" + obj;
            if (!obj.endsWith("}")) obj = obj + "}";

            Map<String, String> m = new HashMap<>();
            putIfPresent(m, obj, "payload20hex");
            putIfPresent(m, obj, "addressBase58");
            putNumIfPresent(m, obj, "balance");
            putNumIfPresent(m, obj, "nonce");
            out.add(m);
        }
        return out;
    }

    private static void putIfPresent(Map<String, String> m, String json, String key) {
        String v = jsonString(json, key);
        if (v != null) m.put(key, v);
    }

    private static void putNumIfPresent(Map<String, String> m, String json, String key) {
        Long v = jsonLong(json, key);
        if (v != null) m.put(key, String.valueOf(v));
    }
}
