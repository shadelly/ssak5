package CleaningServicePark;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Paymethod_table")
public class Paymethod {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String kind;
    private Long number;
    private Long requestId;
    private String payKindRegStatus;

    @PostPersist
    public void onPostPersist(){
        CleaningServicePark.external.Payment payment = new CleaningServicePark.external.Payment();
        payment.setRequestId(getId());
        payment.setPayKind(getKind());
        payment.setPayKindRegStatus("PaymentKindRegistered");

        try {
            PaymethodApplication.applicationContext.getBean(CleaningServicePark.external.PaymentService.class)
                    .payKindChange(payment);
        } catch(Exception e) {
            throw new RuntimeException("PaymentKindRegister failed. Check your payment Service.");
        }

    }

    @PreUpdate
    public void onPrePersist(){
        KindChanged kindChanged = new KindChanged();
        BeanUtils.copyProperties(this, kindChanged);
        kindChanged.setRequestId(getId());
        kindChanged.setKind(getKind());
        kindChanged.setNumber(getNumber());
        kindChanged.setKindRegStatus("PaymentKindRegistered");
        kindChanged.publishAfterCommit();
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

    public String getPayKindRegStatus() {
        return payKindRegStatus;
    }

    public void setPayKindRegStatus(String payKindRegStatus) {
        this.payKindRegStatus = payKindRegStatus;
    }


}
