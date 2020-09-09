package CleaningServicePark;

public class PayConfirmed extends AbstractEvent {

    private Long id;
    private Long payID;
    private String status;
    private Long requestID;
    private String payKind;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getPayId() {
        return payID;
    }

    public void setPayId(Long payID) {
        this.payID = payID;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public Long getRequestId() {
        return requestID;
    }

    public void setRequestId(Long requestID) {
        this.requestID = requestID;
    }
    public String getPayKind() {
        return payKind;
    }

    public void setPayKind(String payKind) {
        this.payKind = payKind;
    }
}