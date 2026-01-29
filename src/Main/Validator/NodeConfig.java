package Main.Validator;

public final class NodeConfig {

    public final int port;
    public final long broadcastFee;      // fixed part
    public final int percentFeeBps;      // 1% = 100 bps
    public final int maxMempool;

    public NodeConfig(int port, long broadcastFee, int percentFeeBps, int maxMempool) {
        if (port <= 0) throw new IllegalArgumentException("port invalid");
        if (broadcastFee < 0) throw new IllegalArgumentException("broadcastFee < 0");
        if (percentFeeBps < 0) throw new IllegalArgumentException("percentFeeBps < 0");
        if (maxMempool <= 0) throw new IllegalArgumentException("maxMempool <= 0");
        this.port = port;
        this.broadcastFee = broadcastFee;
        this.percentFeeBps = percentFeeBps;
        this.maxMempool = maxMempool;
    }

    /** fee = broadcastFee + amount * bps / 10000 */
    public long requiredFee(long amount) {
        long percentPart = (amount * percentFeeBps) / 10_000L;
        return broadcastFee + percentPart;
    }
}
