package CleaningServicePark;

import CleaningServicePark.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class DashBoardViewViewHandler {


    @Autowired
    private DashBoardViewRepository dashBoardViewRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCleaningRequested_then_CREATE_1 (@Payload CleaningRequested cleaningRequested) {
        try {
            if (cleaningRequested.isMe()) {
                // view 객체 생성
                DashBoardView dashBoardView = new DashBoardView();
                // view 객체에 이벤트의 Value 를 set 함
                dashBoardView.setRequestId(cleaningRequested.getRequestId());
                dashBoardView.setRequestDate(cleaningRequested.getRequestDate());
                dashBoardView.setPlace(cleaningRequested.getPlace());
                dashBoardView.setStatus(cleaningRequested.getStatus());
                dashBoardView.setPrice(cleaningRequested.getPrice());
                // view 레파지 토리에 save
                dashBoardViewRepository.save(dashBoardView);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenPayConfirmed_then_UPDATE_1(@Payload PayConfirmed payConfirmed) {
        try {
            if (payConfirmed.isMe()) {
                // view 객체 조회
                List<DashBoardView> dashBoardViewList = dashBoardViewRepository.findByRequestId(payConfirmed.getRequestId());
                for(DashBoardView dashBoardView : dashBoardViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashBoardView.setStatus(payConfirmed.getStatus());
                    // view 레파지 토리에 save
                    dashBoardViewRepository.save(dashBoardView);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenCleaningRequestCanceled_then_UPDATE_2(@Payload CleaningRequestCanceled cleaningRequestCanceled) {
        try {
            if (cleaningRequestCanceled.isMe()) {
                // view 객체 조회
                List<DashBoardView> dashBoardViewList = dashBoardViewRepository.findByRequestId(cleaningRequestCanceled.getRequestId());
                for(DashBoardView dashBoardView : dashBoardViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashBoardView.setStatus(cleaningRequestCanceled.getStatus());
                    // view 레파지 토리에 save
                    dashBoardViewRepository.save(dashBoardView);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenCleaningConfirmed_then_UPDATE_3(@Payload CleaningConfirmed cleaningConfirmed) {
        try {
            if (cleaningConfirmed.isMe()) {
                // view 객체 조회
                List<DashBoardView> dashBoardViewList = dashBoardViewRepository.findByRequestId(cleaningConfirmed.getRequestId());
                for(DashBoardView dashBoardView : dashBoardViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashBoardView.setStatus(cleaningConfirmed.getStatus());
                    // view 레파지 토리에 save
                    dashBoardViewRepository.save(dashBoardView);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCleanCanceled_then_DELETE_(@Payload CleanCanceled cleanCanceled) {
        try {
            if (cleanCanceled.isMe()) {
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}