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
    public void whenPayConfirmed_then_CREATE_1 (@Payload PayConfirmed payConfirmed) {
        try {
            if (payConfirmed.isMe()) {
                // view 객체 생성
                DashBoardView dashBoardView = new DashBoardView();
                // view 객체에 이벤트의 Value 를 set 함
                dashBoardView.setRequestId(payConfirmed.getRequestId());
                dashBoardView.setPayKind(payConfirmed.getKind());
                dashBoardView.setStatus(payConfirmed.getStatus());
                // view 레파지 토리에 save
                dashBoardViewRepository.save(dashBoardView);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenPayCancelConfirmed_then_UPDATE_1(@Payload PayCancelConfirmed payCancelConfirmed) {
        try {
            if (payCancelConfirmed.isMe()) {
                // view 객체 조회
                List<DashBoardView> dashBoardViewList = dashBoardViewRepository.findByRequestId(payCancelConfirmed.getRequestId());
                for(DashBoardView dashBoardView : dashBoardViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashBoardView.setStatus(payCancelConfirmed.getStatus());
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
    public void whenKindRegistered_then_UPDATE_4(@Payload KindRegistered kindRegistered) {
        try {
            if (kindRegistered.isMe()) {
                // view 객체 조회
                List<DashBoardView> dashBoardViewList = dashBoardViewRepository.findByRequestId(kindRegistered.getRequestId());
                for(DashBoardView dashBoardView : dashBoardViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashBoardView.setPayKind(kindRegistered.getKind());
                    // view 레파지 토리에 save
                    dashBoardViewRepository.save(dashBoardView);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenKindChanged_then_UPDATE_5(@Payload KindChanged kindChanged) {
        try {
            if (kindChanged.isMe()) {
                // view 객체 조회
                List<DashBoardView> dashBoardViewList = dashBoardViewRepository.findByRequestId(kindChanged.getRequestId());
                for(DashBoardView dashBoardView : dashBoardViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashBoardView.setPayKind(kindChanged.getKind());
                    // view 레파지 토리에 save
                    dashBoardViewRepository.save(dashBoardView);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCleaningRequestCanceled_then_DELETE_1(@Payload CleaningRequestCanceled cleaningRequestCanceled) {
        try {
            if (cleaningRequestCanceled.isMe()) {
                // view 레파지 토리에 삭제 쿼리
                dashBoardViewRepository.deleteByRequestId(cleaningRequestCanceled.getRequestId());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}