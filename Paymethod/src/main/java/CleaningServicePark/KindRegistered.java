package CleaningServicePark;

public class KindRegistered extends AbstractEvent {

    private Long id;
    private String kind;
    private Long number;
    private Long requestId;

    public KindRegistered(){
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
}
