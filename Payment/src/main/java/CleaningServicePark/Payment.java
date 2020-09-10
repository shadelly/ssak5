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
    private String payKindRegStatus;

    @PostPersist
    public void onPostPersist(){

        System.out.println("##### Payment onPostPersist : " + getStatus());

        if("PaymentApproved".equals(getStatus())) {

            PayConfirmed payConfirmed = new PayConfirmed();
            BeanUtils.copyProperties(this, payConfirmed);
            payConfirmed.setRequestId(getRequestId());
            payConfirmed.setStatus("PaymentCompleted");
            payConfirmed.publishAfterCommit();
        }

        else if("PaymentCancel".equals(getStatus())) {
            PayCancelConfirmed payCancelConfirmed = new PayCancelConfirmed();
            BeanUtils.copyProperties(this, payCancelConfirmed);
            payCancelConfirmed.setRequestId(getRequestId());
            payCancelConfirmed.setStatus("PaymentCancelCompleted");
            payCancelConfirmed.publishAfterCommit();
        }

        else if("PaymentKindRegistered".equals(getPayKindRegStatus())){
            PayKindChangeConfirmed payKindChangeConfirmed = new PayKindChangeConfirmed();
            BeanUtils.copyProperties(this, payKindChangeConfirmed);
            payKindChangeConfirmed.setRequestId(getRequestId());
            payKindChangeConfirmed.setPayKindRegStatus("Payment kind change Completed");
            payKindChangeConfirmed.publishAfterCommit();
        }
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


    public String getPayKindRegStatus() {
        return payKindRegStatus;
    }

    public void setPayKindRegStatus(String payKindRegStatus) {
        this.payKindRegStatus = payKindRegStatus;
    }


}
