
package CleaningServicePark;

public class KindChanged extends AbstractEvent {

    private Long id;
    private Long requestID;
    private String kindRegStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getRequestId() {
        return requestID;
    }

    public void setRequestId(Long requestID) {
        this.requestID = requestID;
    }
    public String getKindRegStatus() {
        return kindRegStatus;
    }

    public void setKindRegStatus(String kindRegStatus) {
        this.kindRegStatus = kindRegStatus;
    }
}
