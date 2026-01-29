package Main.Validator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ValidatorServer {

    private final NodeConfig cfg;
    private final Ledger ledger = new Ledger();

    // txHashHex -> status
    private final Map<String, String> status = new ConcurrentHashMap<>();
    // txHashHex -> rawTx (stored until mined)
    private final Map<String, byte[]> mempool = new ConcurrentHashMap<>();

    public ValidatorServer(NodeConfig cfg) {
        this.cfg = cfg;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(cfg.port), 0);

        server.createContext("/fee", this::handleFee);
        server.createContext("/account", this::handleAccount);
        server.createContext("/tx/submit", this::handleSubmit);
        server.createContext("/tx/status", this::handleStatus);
        server.createContext("/mine", this::handleMine);

        server.start();
        System.out.println("Validator running: http://127.0.0.1:" + cfg.port);

        // For quick testing: faucet some addresses by calling ledger.faucet(...) manually
        // or add a dev-only endpoint.
    }

    // ---------- Handlers ----------

    private void handleFee(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, jsonErr("GET required"));
            return;
        }
        String j = "{"
                + "\"broadcastFee\":" + cfg.broadcastFee + ","
                + "\"percentFeeBps\":" + cfg.percentFeeBps
                + "}";
        send(ex, 200, j);
    }

    private void handleAccount(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, jsonErr("GET required"));
            return;
        }

        // /account?addr=<payload20hex>
        String addrHex = queryParam(ex.getRequestURI().getQuery(), "addr");
        if (addrHex == null) {
            send(ex, 400, jsonErr("missing addr (payload20 hex)"));
            return;
        }

        byte[] addr20;
        try {
            addr20 = HexUtil.fromHex(addrHex);
        } catch (Exception e) {
            send(ex, 400, jsonErr("bad addr hex"));
            return;
        }
        if (addr20.length != 20) {
            send(ex, 400, jsonErr("addr must be 20 bytes hex"));
            return;
        }

        Ledger.Account a = ledger.getOrCreate(addr20);
        String j = "{"
                + "\"addr\":\"" + addrHex + "\","
                + "\"balance\":" + a.balance + ","
                + "\"nonce\":" + a.nonce
                + "}";
        send(ex, 200, j);
    }

    private void handleSubmit(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, jsonErr("POST required"));
            return;
        }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String rawTxHex = JsonMini.getString(body, "rawTxHex");
        String pubKeyHex = JsonMini.getString(body, "pubKeyCompressedHex");

        if (rawTxHex == null || pubKeyHex == null) {
            send(ex, 400, jsonErr("missing rawTxHex or pubKeyCompressedHex"));
            return;
        }

        byte[] rawTx;
        byte[] pubKey33;
        try {
            rawTx = HexUtil.fromHex(rawTxHex);
            pubKey33 = HexUtil.fromHex(pubKeyHex);
        } catch (Exception e) {
            send(ex, 400, jsonErr("bad hex"));
            return;
        }
        if (pubKey33.length != 33) {
            send(ex, 400, jsonErr("pubKeyCompressedHex must be 33 bytes"));
            return;
        }

        TxV1Net tx;
        try {
            tx = TxV1Net.decodeFull(rawTx);
        } catch (Exception e) {
            send(ex, 200, reject("DECODE_FAIL", null, "cannot decode tx"));
            return;
        }

        String txHashHex = HexUtil.toHex(tx.txHash32);

        // 1) address binding check (from == keccak(pub)[12..32])
        if (!TxV1Net.addressMatchesPubKey(tx.fromAddr20, pubKey33)) {
            send(ex, 200, reject("BAD_FROM_ADDR", txHashHex, "fromAddr20 does not match pubKey"));
            return;
        }

        // 2) signature verify
        if (!tx.verifySignature(pubKey33)) {
            send(ex, 200, reject("BAD_SIGNATURE", txHashHex, "signature verify failed"));
            return;
        }

        // 3) mempool size
        if (mempool.size() >= cfg.maxMempool) {
            send(ex, 200, reject("MEMPOOL_FULL", txHashHex, "try later"));
            return;
        }

        // 4) ledger checks (nonce, balance, fee policy) for TRANSFER only (type=0)
        // You can expand later for escrow types.
        if (tx.typeU8 == 0) {
            long amount;
            byte[] to20;
            try {
                amount = tx.transferAmountOrThrow();
                to20 = tx.transferTo20OrThrow();
            } catch (Exception e) {
                send(ex, 200, reject("BAD_PAYLOAD", txHashHex, "invalid transfer payload"));
                return;
            }

            long requiredFee = cfg.requiredFee(amount);
            if (tx.feeU64 != requiredFee) {
                send(ex, 200, reject("BAD_FEE", txHashHex, "fee must be broadcastFee + 1% amount; required=" + requiredFee));
                return;
            }

            Ledger.Account from = ledger.getOrCreate(tx.fromAddr20);
            long expectedNonce = from.nonce + 1;
            if (tx.nonceU64 != expectedNonce) {
                send(ex, 200, reject("BAD_NONCE", txHashHex, "expected " + expectedNonce + " got " + tx.nonceU64));
                return;
            }

            long required = amount + tx.feeU64;
            if (from.balance < required) {
                send(ex, 200, reject("INSUFFICIENT_FUNDS", txHashHex, "need " + required + " have " + from.balance));
                return;
            }
        }

        // Accept into mempool (PENDING)
        mempool.put(txHashHex, rawTx);
        status.put(txHashHex, "PENDING");

        String ok = "{"
                + "\"status\":\"ACCEPTED\","
                + "\"txHash\":\"" + txHashHex + "\","
                + "\"mempool\":true"
                + "}";
        send(ex, 200, ok);
    }

    private void handleStatus(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, jsonErr("GET required"));
            return;
        }
        String hash = queryParam(ex.getRequestURI().getQuery(), "hash");
        if (hash == null) {
            send(ex, 400, jsonErr("missing hash"));
            return;
        }
        String st = status.getOrDefault(hash, "UNKNOWN");
        String j = "{"
                + "\"txHash\":\"" + hash + "\","
                + "\"status\":\"" + st + "\""
                + "}";
        send(ex, 200, j);
    }

    private void handleMine(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, jsonErr("POST required"));
            return;
        }

        // Confirm all pending txs (simple “single node mining”)
        int applied = 0;

        for (Map.Entry<String, byte[]> ent : mempool.entrySet()) {
            String txHash = ent.getKey();
            byte[] rawTx = ent.getValue();

            TxV1Net tx;
            try {
                tx = TxV1Net.decodeFull(rawTx);
            } catch (Exception e) {
                status.put(txHash, "REJECTED");
                continue;
            }

            // apply only TRANSFER for now
            if (tx.typeU8 != 0) {
                status.put(txHash, "REJECTED");
                continue;
            }

            long amount = tx.transferAmountOrThrow();
            byte[] to20 = tx.transferTo20OrThrow();

            Ledger.Account from = ledger.getOrCreate(tx.fromAddr20);
            Ledger.Account to = ledger.getOrCreate(to20);

            long required = amount + tx.feeU64;
            if (from.balance < required) {
                status.put(txHash, "REJECTED");
                continue;
            }

            // apply
            from.balance -= required;
            to.balance += amount;
            from.nonce = tx.nonceU64;

            status.put(txHash, "CONFIRMED");
            applied++;
        }

        mempool.clear();

        String j = "{"
                + "\"status\":\"OK\","
                + "\"applied\":" + applied
                + "}";
        send(ex, 200, j);
    }

    // ---------- Utilities ----------

    private static String queryParam(String query, String key) {
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }

    private static String jsonErr(String msg) {
        return "{\"status\":\"ERROR\",\"message\":\"" + esc(msg) + "\"}";
    }

    private static String reject(String reason, String txHash, String detail) {
        String h = (txHash == null) ? "" : ",\"txHash\":\"" + txHash + "\"";
        return "{"
                + "\"status\":\"REJECTED\""
                + h + ","
                + "\"reason\":\"" + esc(reason) + "\","
                + "\"detail\":\"" + esc(detail) + "\""
                + "}";
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void send(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    // ---------- main ----------
    public static void main(String[] args) throws Exception {
        NodeConfig cfg = new NodeConfig(
                8080,
                1000L,   // broadcast fee
                100,     // 1% = 100 bps
                10_000   // max mempool
        );
        ValidatorServer node = new ValidatorServer(cfg);

        // DEV faucet: give some money to an address20 hex via code or add endpoint later.
        // Example: node.ledger.faucet(HexUtil.fromHex("...20 bytes..."), 1_000_000);

        node.start();
    }
}
