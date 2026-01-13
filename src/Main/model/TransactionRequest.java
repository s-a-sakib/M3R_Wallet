package Main.model;

import java.time.Instant;
import java.util.StringJoiner;

public class TransactionRequest {
    private final TransactionType type;
    private final String fromAddress;
    private final String toAddress;
    private final String amount;
    private final String memo;
    private final EscrowDetails escrowDetails;
    private final Instant createdAt;

    public TransactionRequest(TransactionType type,
                              String fromAddress,
                              String toAddress,
                              String amount,
                              String memo,
                              EscrowDetails escrowDetails,
                              Instant createdAt) {
        this.type = type;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.amount = amount;
        this.memo = memo;
        this.escrowDetails = escrowDetails;
        this.createdAt = createdAt;
    }

    public TransactionType getType() {
        return type;
    }

    public String toJson() {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        joiner.add("\"type\":\"" + type.name() + "\"");
        joiner.add("\"fromAddress\":\"" + escape(fromAddress) + "\"");
        joiner.add("\"toAddress\":\"" + escape(toAddress) + "\"");
        joiner.add("\"amount\":\"" + escape(amount) + "\"");
        joiner.add("\"memo\":\"" + escape(memo) + "\"");
        joiner.add("\"createdAt\":\"" + createdAt.toString() + "\"");
        if (escrowDetails != null) {
            joiner.add("\"escrowDetails\":" + escrowJson());
        }
        return joiner.toString();
    }

    private String escrowJson() {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        joiner.add("\"sellerAddress\":\"" + escape(escrowDetails.getSellerAddress()) + "\"");
        joiner.add("\"memoNumber\":\"" + escape(escrowDetails.getMemoNumber()) + "\"");
        joiner.add("\"escrowType\":\"" + escape(escrowDetails.getEscrowType()) + "\"");
        joiner.add("\"releaseDate\":\"" + escape(escrowDetails.getReleaseDate()) + "\"");
        joiner.add("\"releaseAuthority\":\"" + escape(escrowDetails.getReleaseAuthority()) + "\"");
        joiner.add("\"disputePolicy\":\"" + escape(escrowDetails.getDisputePolicy()) + "\"");
        joiner.add("\"refundWindowDays\":\"" + escape(escrowDetails.getRefundWindowDays()) + "\"");
        joiner.add("\"milestoneNotes\":\"" + escape(escrowDetails.getMilestoneNotes()) + "\"");
        return joiner.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
