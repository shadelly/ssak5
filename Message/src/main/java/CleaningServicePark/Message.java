package CleaningServicePark;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Message_table")
public class Message {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long msgId;
    private Long requestId;
    private String status;
    private String payKind;
    private String kindRegStatus;

    @PostPersist
    public void onPostPersist(){
        MessageAlerted messageAlerted = new MessageAlerted();
        BeanUtils.copyProperties(this, messageAlerted);
        messageAlerted.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getMsgId() {
        return msgId;
    }

    public void setMsgId(Long msgId) {
        this.msgId = msgId;
    }
    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPayKind() {
        return payKind;
    }

    public void setPayKind(String payKind) {
        this.payKind = payKind;
    }
    public String getKindRegStatus() {
        return kindRegStatus;
    }

    public void setKindRegStatus(String kindRegStatus) {
        this.kindRegStatus = kindRegStatus;
    }




}
