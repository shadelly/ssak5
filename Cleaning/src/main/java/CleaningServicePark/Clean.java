package CleaningServicePark;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Clean_table")
public class Clean {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long cleanId;
    private String status;
    private Long requestId;

    @PostPersist
    public void onPostPersist(){
        CleaningConfirmed cleaningConfirmed = new CleaningConfirmed();
        BeanUtils.copyProperties(this, cleaningConfirmed);
        cleaningConfirmed.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getCleanId() {
        return cleanId;
    }

    public void setCleanId(Long cleanId) {
        this.cleanId = cleanId;
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




}
