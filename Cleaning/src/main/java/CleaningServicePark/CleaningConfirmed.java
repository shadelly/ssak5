package CleaningServicePark;

public class CleaningConfirmed extends AbstractEvent {

    private Long id;
    private Long cleanID;
    private String status;
    private Long requestID;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getCleanId() {
        return cleanID;
    }

    public void setCleanId(Long cleanID) {
        this.cleanID = cleanID;
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
}