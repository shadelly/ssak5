package CleaningServicePark;

public class KindChanged extends AbstractEvent {

    private Long id;
    private Long requestID;
    private String kindRegStatus;
    private String kind;
    private Long number;

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
    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }
}