package CleaningServicePark;

import CleaningServicePark.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayConfirmed_MessageAlert(@Payload PayConfirmed payConfirmed){

        if(payConfirmed.isMe()){
            System.out.println("##### listener MessageAlert : " + payConfirmed.toJson());
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCleaningConfirmed_MessageAlert(@Payload CleaningConfirmed cleaningConfirmed){

        if(cleaningConfirmed.isMe()){
            System.out.println("##### listener MessageAlert : " + cleaningConfirmed.toJson());
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayCancelConfirmed_MessageAlert(@Payload PayCancelConfirmed payCancelConfirmed){

        if(payCancelConfirmed.isMe()){
            System.out.println("##### listener MessageAlert : " + payCancelConfirmed.toJson());
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverKindChanged_MessageAlert(@Payload KindChanged kindChanged){

        if(kindChanged.isMe()){
            System.out.println("##### listener MessageAlert : " + kindChanged.toJson());
        }
    }

}
