package CleaningServicePark;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Payment_table")
public class Payment {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long payId;
    private String status;
    private Long requestId;
    private String payKind;

    @PostPersist
    public void onPostPersist(){
        PayConfirmed payConfirmed = new PayConfirmed();
        BeanUtils.copyProperties(this, payConfirmed);
        payConfirmed.publishAfterCommit();


        PayCancelConfirmed payCancelConfirmed = new PayCancelConfirmed();
        BeanUtils.copyProperties(this, payCancelConfirmed);
        payCancelConfirmed.publishAfterCommit();


        PayKindChangeConfirmed payKindChangeConfirmed = new PayKindChangeConfirmed();
        BeanUtils.copyProperties(this, payKindChangeConfirmed);
        payKindChangeConfirmed.publishAfterCommit();


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
