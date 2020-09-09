package CleaningServicePark;

public class PayKindChangeConfirmed extends AbstractEvent {

    private Long id;
    private Long payId;
    private String status;
    private Long requestId;
    private String payKind;

    public PayKindChangeConfirmed(){
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getPayId() {
        return payId;
    }

    public void setPayId(Long payId) {
        this.payId = payId;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
    public String getPayKind() {
        return payKind;
    }

    public void setPayKind(String payKind) {
        this.payKind = payKind;
    }
}
