package Main.model;

public class EscrowDetails {
    private final String sellerAddress;
    private final String memoNumber;
    private final String escrowType;
    private final String releaseDate;
    private final String releaseAuthority;
    private final String disputePolicy;
    private final String refundWindowDays;
    private final String milestoneNotes;

    public EscrowDetails(String sellerAddress,
                         String memoNumber,
                         String escrowType,
                         String releaseDate,
                         String releaseAuthority,
                         String disputePolicy,
                         String refundWindowDays,
                         String milestoneNotes) {
        this.sellerAddress = sellerAddress;
        this.memoNumber = memoNumber;
        this.escrowType = escrowType;
        this.releaseDate = releaseDate;
        this.releaseAuthority = releaseAuthority;
        this.disputePolicy = disputePolicy;
        this.refundWindowDays = refundWindowDays;
        this.milestoneNotes = milestoneNotes;
    }

    public String getSellerAddress() {
        return sellerAddress;
    }

    public String getMemoNumber() {
        return memoNumber;
    }

    public String getEscrowType() {
        return escrowType;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getReleaseAuthority() {
        return releaseAuthority;
    }

    public String getDisputePolicy() {
        return disputePolicy;
    }

    public String getRefundWindowDays() {
        return refundWindowDays;
    }

    public String getMilestoneNotes() {
        return milestoneNotes;
    }
}
