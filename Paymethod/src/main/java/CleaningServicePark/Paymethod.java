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

    @PostPersist
    public void onPostPersist(){
        KindRegistered kindRegistered = new KindRegistered();
        BeanUtils.copyProperties(this, kindRegistered);
        kindRegistered.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        CleaningServicePark.external.Payment payment = new CleaningServicePark.external.Payment();
        // mappings goes here
        PaymethodApplication.applicationContext.getBean(CleaningServicePark.external.PaymentService.class)
            .payKindChange(payment);


    }

    @PrePersist
    public void onPrePersist(){
        KindChanged kindChanged = new KindChanged();
        BeanUtils.copyProperties(this, kindChanged);
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




}
