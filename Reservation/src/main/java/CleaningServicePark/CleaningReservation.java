package CleaningServicePark;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="CleaningReservation_table")
public class CleaningReservation {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long requestId;
    private String requestDate;
    private String place;
    private String status;
    private Integer price;

    @PostPersist
    public void onPostPersist(){
        CleaningRequested cleaningRequested = new CleaningRequested();
        BeanUtils.copyProperties(this, cleaningRequested);
        cleaningRequested.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        CleaningServicePark.external.Payment payment = new CleaningServicePark.external.Payment();
        // mappings goes here
        ReservationApplication.applicationContext.getBean(CleaningServicePark.external.PaymentService.class)
            .payRequest(payment);


        CleaningRequestCanceled cleaningRequestCanceled = new CleaningRequestCanceled();
        BeanUtils.copyProperties(this, cleaningRequestCanceled);
        cleaningRequestCanceled.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }
    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }




}
