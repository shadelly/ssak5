
package CleaningServicePark;

public class PayKindChangeConfirmed extends AbstractEvent {

    private Long id;
    private Long payID;
    private String status;
    private Long requestID;
    private String payKind;
    private String payKindRegStatus;

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

    public String getPayKindRegStatus() {
        return payKindRegStatus;
    }

    public void setPayKindRegStatus(String payKindRegStatus) {
        this.payKindRegStatus = payKindRegStatus;
    }
}
