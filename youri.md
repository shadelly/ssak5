#1.SAGA
- 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트
결제수단이 변경된 후에 알림 처리는 동기식이 아니라 비 동기식으로 처리하여 알림 시스템의 처리를 위하여 결제수단 등록이 블로킹 되지 않도록 처리

```java
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
                payKindChangeConfirmed.setStatus("Payment kind change Completed");
                payKindChangeConfirmed.publishAfterCommit();
            }
        }
    //...
}
```
- 알림 서비스에서는 결제승인, 결제취소 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다
```java
@Service
public class PolicyHandler{

	@Autowired
    private MessageRepository messageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayConfirmed_MessageAlert(@Payload PayConfirmed payConfirmed){

        if(payConfirmed.isMe()){
        	Message message = new Message();

        	message.setRequestId(payConfirmed.getRequestId());
        	message.setStatus(payConfirmed.getStatus());

        	messageRepository.save(message);

            System.out.println("##### listener MessageAlert : " + payConfirmed.toJson());
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCleaningConfirmed_MessageAlert(@Payload CleaningConfirmed cleaningConfirmed){

        if(cleaningConfirmed.isMe()){
        	Message message = new Message();

        	message.setRequestId(cleaningConfirmed.getRequestId());
        	message.setStatus(cleaningConfirmed.getStatus());

        	messageRepository.save(message);

            System.out.println("##### listener MessageAlert : " + cleaningConfirmed.toJson());
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayCancelConfirmed_MessageAlert(@Payload PayCancelConfirmed payCancelConfirmed){

        if(payCancelConfirmed.isMe()){
        	Message message = new Message();

        	message.setRequestId(payCancelConfirmed.getRequestId());
        	message.setStatus(payCancelConfirmed.getStatus());

        	messageRepository.save(message);

            System.out.println("##### listener MessageAlert : " + payCancelConfirmed.toJson());
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverKindChanged_MessageAlert(@Payload KindChanged kindChanged){

        if(kindChanged.isMe()){
            Message message = new Message();

            message.setRequestId(kindChanged.getRequestId());
            message.setPayKind(kindChanged.getKind());
            message.setKindRegStatus(kindChanged.getKindRegStatus());

            messageRepository.save(message);
            System.out.println("##### listener MessageAlert : " + kindChanged.toJson());
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayKindChangeConfirmed_MessageAlert(@Payload PayKindChangeConfirmed payKindChangeConfirmed){

        if(payKindChangeConfirmed.isMe()){
            Message message = new Message();

            message.setRequestId(payKindChangeConfirmed.getRequestId());
            message.setPayKind(payKindChangeConfirmed.getPayKind());
            message.setKindRegStatus(payKindChangeConfirmed.getKindRegStatus());

            messageRepository.save(message);
            System.out.println("##### listener MessageAlert : " + payKindChangeConfirmed.toJson());
        }
    }


}
```
- 실제 알림 처리
```
@Service
public class PolicyHandler{

    @Autowired
    private MessageRepository messageRepository;

    @StreamListener(KafkaProcessor.INPUT)
        public void wheneverPayKindChangeConfirmed_MessageAlert(@Payload PayKindChangeConfirmed payKindChangeConfirmed){
    
            if(payKindChangeConfirmed.isMe()){
                Message message = new Message();
    
                message.setRequestId(payKindChangeConfirmed.getRequestId());
                message.setPayKind(payKindChangeConfirmed.getPayKind());
                message.setKindRegStatus(payKindChangeConfirmed.getKindRegStatus());
    
                messageRepository.save(message);
                System.out.println("##### listener MessageAlert : " + payKindChangeConfirmed.toJson());
            }
        }
 ```
* 알림 시스템은 결제수단관리와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 알림 시스템이 유지보수로 인해 잠시 내려간 상태라도 결제수단을 등록하는데 문제가 없다

```
# 알림 서비스를 잠시 내려놓음
kubectl delete -f message.yaml

# 결제수단등록 (siege 에서)
http POST http://paymethod:8080/paymethods kind=credit number=40095003 requestId=1 payKindRegStatus=PaymentKindRegistered #Fail

# 알림이력 확인 (siege 에서)
http http://message:8080/messages # 알림이력조회 불가

http: error: ConnectionError: HTTPConnectionPool(host='message', port=8080): Max retries exceeded with url: /messages (Caused by NewConnectionError('<urllib3.connection.HTTPConnection object at 0x7fae6595deb8>: Failed to establish a new connection: [Errno -2] Name or service not known')) while doing GET request to URL: http://message:8080/messages

# 알림 서비스 기동
kubectl apply -f message.yaml

# 알림이력 확인 (siege 에서)
http http://message:8080/messages # 알림이력조회

HTTP/1.1 200 OK
content-type: application/hal+json;charset=UTF-8
date: Wed, 09 Sep 2020 16:22:52 GMT
server: envoy
transfer-encoding: chunked
x-envoy-upstream-service-time: 439

{
    "_embedded": {
        "messages": [
            {
                "_links": {
                    "message": {
                        "href": "http://message:8080/messages/1"
                    },
                    "self": {
                        "href": "http://message:8080/messages/1"
                    }
                },
                "requestId": 6,
                "status": "PaymentKindRegistered"
            }
        ]
    },
    "_links": {
        "profile": {
            "href": "http://message:8080/profile/messages"
        },
        "self": {
            "href": "http://message:8080/messages{?page,size,sort}",
            "templated": true
        }
    },
    "page": {
        "number": 0,
        "size": 20,
        "totalElements": 1,
        "totalPages": 1
    }
}
```
#2.CQRS
```java
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
//...중략

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
//...중략
}
```
#3.CORERELATION
```
gateway 사용
```
#4.REQ/RESP
분석단계에서의 조건 중 하나로 예약->결제 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 결제 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 
```java
@FeignClient(name="Payment", url="${api.url.payment}")
public interface PaymentService {

    @RequestMapping(method= RequestMethod.POST, path="/payments")
    public void payRequest(@RequestBody Payment payment);

}
```
- 결제수단 등록을 한 직후(@PostPersist) 결제가 완료되도록 처리
```java
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
        // 예약시 결제까지 트랜잭션을 통합을 위해 결제 서비스 직접 호출
    	CleaningServicePark.external.Payment payment = new CleaningServicePark.external.Payment();
        payment.setRequestId(getId());
        payment.setPrice(getPrice());
        payment.setStatus("PaymentApproved");

        try {
        	Paymethod.applicationContext.getBean(CleaningServicePark.external.PaymentService.class)
            	.payRequest(payment);
        } catch(Exception e) {
        	throw new RuntimeException("PaymentApprove failed. Check your payment Service.");
        }

    }
}
```

- 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 결제수단 등록도 되지않음을 확인
```
# 결제 서비스를 잠시 내려놓음
$ kubectl delete -f payment.yaml

NAME                           READY   STATUS    RESTARTS   AGE
cleaning-bf474f568-vxl8r       2/2     Running   0          137m
dashboard-7f7768bb5-7l8wr      2/2     Running   0          136m
gateway-6dfcbbc84f-rwnsh       2/2     Running   0          37m
message-69597f6864-mhwx7       2/2     Running   0          137m
paymethod-646dcb9ffb-llrzk     2/2     Running   0          12m
reservation-775fc6574d-kddgd   2/2     Running   0          144m
siege                          2/2     Running   0          3h39m

# 결제수단등록 (siege 에서)
http POST http://paymethod:8080/paymethods kind=credit number=40095003 requestId=1 payKindRegStatus=PaymentKindRegistered #Fail

# 결제수단등록 시 에러 내용
HTTP/1.1 500 Internal Server Error
content-type: application/json;charset=UTF-8
date: Wed, 09 Sep 2020 15:52:52 GMT
server: envoy
transfer-encoding: chunked
x-envoy-upstream-service-time: 82

{
    "error": "Internal Server Error",
    "message": "Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction",
    "path": "/paymethods",
    "status": 500,
    "timestamp": "2020-09-09T15:52:53.135+0000"
}

# 결제서비스 재기동전에 아래의 비동기식 호출 기능 점검 테스트 수행 (siege 에서)
http DELETE http://paymethod:8080/paymethod/1 #Success

# 결과
root@siege:/# http DELETE http://paymethod:8080/paymethod/1
HTTP/1.1 404 Not Found
content-type: application/hal+json;charset=UTF-8
date: Wed, 09 Sep 2020 15:56:17 GMT
server: envoy
transfer-encoding: chunked
x-envoy-upstream-service-time: 15

{
    "error": "Not Found",
    "message": "No message available",
    "path": "/paymethod/1",
    "status": 404,
    "timestamp": "2020-09-09T15:56:17.705+0000"
}

# 결제서비스 재기동
$ kubectl apply -f payment.yaml

NAME                           READY   STATUS    RESTARTS   AGE
cleaning-bf474f568-vxl8r       2/2     Running   0          137m
dashboard-7f7768bb5-7l8wr      2/2     Running   0          136m
gateway-6dfcbbc84f-rwnsh       2/2     Running   0          37m
message-69597f6864-mhwx7       2/2     Running   0          137m
payment-7749f7dc7c-kfjxb       2/2     Running   0          88s
paymethod-646dcb9ffb-llrzk     2/2     Running   0          12m
reservation-775fc6574d-kddgd   2/2     Running   0          144m
siege                          2/2     Running   0          3h39m

# 결제수단등록 (siege 에서)
http POST http://paymethod:8080/paymethods kind=credit number=40095003 requestId=1 payKindRegStatus=PaymentKindRegistered

# 처리결과
HTTP/1.1 201 Created
content-type: application/json;charset=UTF-8
date: Wed, 09 Sep 2020 16:10:58 GMT
location: http://Paymethod:8080/paymethods/1
server: envoy
transfer-encoding: chunked
x-envoy-upstream-service-time: 651

{
    "_links": {
        "paymethod": {
            "href": "http://Paymethod:8080/paymethods/1"
        },
        "self": {
            "href": "http://Paymethod:8080/paymethods/1"
        }
    },
    "kind": "credit",
    "number": 40095003,
    "payKindRegStatus": "PaymentKindRegistered",
    "requestId": 1
}
```
#5.GATEWAY
- gateway service type 변경 (ClusterIP -> LoadBalancer)
```console
$ kubectl edit service/gateway -n ssak5

root@ssak5-vm:~/ssak5# kubectl get service -n ssak5
NAME          TYPE           CLUSTER-IP     EXTERNAL-IP    PORT(S)          AGE
cleaning      ClusterIP      10.0.55.227    <none>         8080/TCP         30m
dashboard     ClusterIP      10.0.108.26    <none>         8080/TCP         30m
gateway       LoadBalancer   10.0.55.192    20.39.188.50   8080:32750/TCP   31m
message       ClusterIP      10.0.23.249    <none>         8080/TCP         30m
payment       ClusterIP      10.0.213.242   <none>         8080/TCP         30m
paymethod     ClusterIP      10.0.234.6     <none>         8080/TCP         30m
reservation   ClusterIP      10.0.126.188   <none>         8080/TCP         30m
```
#6.DEPLOY/PIPELINE
```
kubectl apply -f configmap.yaml
kubectl apply -f gateway.yaml
kubectl apply -f cleaning.yaml
kubectl apply -f reservation.yaml
kubectl apply -f payment.yaml
kubectl apply -f dashboard.yaml
kubectl apply -f message.yaml
kubectl apply -f paymethod.yaml

```
#7.CIRCUIT BREAKER
* istio-injection 적용 (기 적용완료)
```
kubectl label namespace ssak5 istio-injection=enabled

```
* 예약, 결제 서비스 모두 아무런 변경 없음

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 100명
- 30초 동안 실시
```console
siege -v -c100 -t30S -r10 --content-type "application/json" 'http://paymethod:8080/paymethods POST {"kind": "credit","number": 40095003,"requestId": 1,"payKindRegStatus": "PaymentKindRegistered"}'

HTTP/1.1 201     1.20 secs:     341 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     1.12 secs:     341 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.14 secs:     341 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     1.11 secs:     341 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     1.21 secs:     341 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     1.20 secs:     341 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     1.20 secs:     341 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     1.11 secs:     341 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     1.21 secs:     341 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.12 secs:     341 bytes ==> POST http://paymethod:8080/paymethods

Lifting the server siege...
Transactions:                   2817 hits
Availability:                 100.00 %
Elapsed time:                  29.11 secs
Data transferred:               1.53 MB
Response time:                  1.23 secs
Transaction rate:              79.79 trans/sec
Throughput:                     0.03 MB/sec
Concurrency:                   97.95
Successful transactions:        2817
Failed transactions:               0
Longest transaction:            7.29
Shortest transaction:           0.05
```
* 서킷 브레이킹을 위한 DestinationRule 적용
```
cd ssak5/yaml
kubectl apply -f payment_dr.yaml

# destinationrule.networking.istio.io/dr-payment created

HTTP/1.1 500     0.68 secs:     262 bytes ==> POST http://paymethod:8080/paymethod
HTTP/1.1 500     0.70 secs:     262 bytes ==> POST http://paymethod:8080/paymethod
HTTP/1.1 500     0.71 secs:     262 bytes ==> POST http://paymethod:8080/paymethod
HTTP/1.1 500     0.72 secs:     262 bytes ==> POST http://paymethod:8080/paymethod
HTTP/1.1 500     0.92 secs:     262 bytes ==> POST http://paymethod:8080/paymethod
HTTP/1.1 500     0.68 secs:     262 bytes ==> POST http://paymethod:8080/paymethod
HTTP/1.1 500     0.82 secs:     262 bytes ==> POST http://paymethod:8080/paymethod
HTTP/1.1 500     0.71 secs:     262 bytes ==> POST http://paymethod:8080/paymethod
siege aborted due to excessive socket failure; you
can change the failure threshold in $HOME/.siegerc

Transactions:                     20 hits
Availability:                   1.75 %
Elapsed time:                   9.92 secs
Data transferred:               0.29 MB
Response time:                 48.04 secs
Transaction rate:               2.02 trans/sec
Throughput:                     0.03 MB/sec
Concurrency:                   96.85
Successful transactions:          20
Failed transactions:            1123
Longest transaction:            2.53
Shortest transaction:           0.04
```
- DestinationRule 적용되어 서킷 브레이킹 동작 확인 (kiali 화면)
![image](https://user-images.githubusercontent.com/68408649/92671670-d2a2b300-f351-11ea-91d4-26fde368fa02.png)

#8.AUTOSCALE(HPA)
* (istio injection 적용한 경우) istio injection 적용 해제
```
kubectl label namespace ssak5 istio-injection=disabled --overwrite

# namespace/ssak5 labeled
kubectl apply -f paymethod.yaml
```
- 결제수단관리 서비스 배포시 resource 설정 적용되어 있음
```
    spec:
      containers:
          ...
          resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
```

- 결제수단관리 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 3개까지 늘려준다
```console
kubectl autoscale deploy paymethod -n ssak5 --min=1 --max=3 --cpu-percent=15

# horizontalpodautoscaler.autoscaling/paymethod autoscaled

root@ssak5-vm:~/ssak5/yaml# kubectl get all -n ssak5
NAME                               READY   STATUS    RESTARTS   AGE
pod/cleaning-bf474f568-vxl8r       2/2     Running   0          3h5m
pod/dashboard-7f7768bb5-7l8wr      2/2     Running   0          3h3m
pod/gateway-6dfcbbc84f-rwnsh       2/2     Running   0          85m
pod/message-69597f6864-fjs69       2/2     Running   0          34m
pod/payment-7749f7dc7c-kfjxb       2/2     Running   0          39m
pod/paymethod-646dcb9ffb-llrzk     2/2     Running   0          60m
pod/reservation-775fc6574d-kddgd   2/2     Running   0          3h12m
pod/siege                          2/2     Running   0          4h27m

NAME                  TYPE           CLUSTER-IP     EXTERNAL-IP    PORT(S)          AGE
service/cleaning      ClusterIP      10.0.150.114   <none>         8080/TCP         3h5m
service/dashboard     ClusterIP      10.0.69.44     <none>         8080/TCP         3h3m
service/gateway       LoadBalancer   10.0.56.218    20.39.188.50   8080:32750/TCP   31m
service/message       ClusterIP      10.0.255.90    <none>         8080/TCP         34m
service/payment       ClusterIP      10.0.64.167    <none>         8080/TCP         39m
service/paymethod     ClusterIP      10.0.234.6     <none>         8080/TCP         30m
service/reservation   ClusterIP      10.0.23.111    <none>         8080/TCP         3h12m


NAME                          READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/cleaning      1/1     1            1           3h5m
deployment.apps/dashboard     1/1     1            1           3h3m
deployment.apps/gateway       1/1     1            1           85m
deployment.apps/message       1/1     1            1           34m
deployment.apps/payment       1/1     1            1           39m
deployment.apps/paymethod     1/1     1            1           4h44m
deployment.apps/reservation   1/1     1            1           3h12m

NAME                                     DESIRED   CURRENT   READY   AGE
replicaset.apps/cleaning-bf474f568       1         1         1       3h5m
replicaset.apps/dashboard-7f7768bb5      1         1         1       3h3m
replicaset.apps/gateway-6dfcbbc84f       1         1         1       85m
replicaset.apps/message-69597f6864       1         1         1       34m
replicaset.apps/payment-7749f7dc7c       1         1         1       39m
replicaset.apps/paymethod-797b9b6f66     1         1         1       4h44m
replicaset.apps/reservation-775fc6574d   1         1         1       3h12m

NAME                                          REFERENCE            TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
horizontalpodautoscaler.autoscaling/payment   Deployment/paymethod   3%/15%    1         3         1          55s
```

- CB 에서 했던 방식대로 워크로드를 1분 동안 걸어준다.
```console
siege -v -c100 -t60S -r10 --content-type "application/json" 'http://paymethod:8080/paymethods POST {"kind": "credit","number": 40095003,"requestId": 1,"payKindRegStatus": "PaymentKindRegistered"}'
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다
```console
kubectl get deploy paymethod -n ssak5 -w 

NAME      READY   UP-TO-DATE   AVAILABLE   AGE
paymethod   1/1     1            1           43m

# siege 부하 적용 후
root@ssak5-vm:/# kubectl get deploy paymethod -n ssak5 -w
NAME      READY   UP-TO-DATE   AVAILABLE   AGE
paymethod   1/1     1            1           37m
paymethod   1/3     1            1           38m
paymethod   1/3     3            1           38m
paymethod   2/3     3            2           40m
paymethod   3/3     3            3           40m
```
- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다.
```console
Lifting the server siege...
Transactions:                  15109 hits
Availability:                 100.00 %
Elapsed time:                 179.75 secs
Data transferred:               6.31 MB
Response time:                  0.92 secs
Transaction rate:             107.42 trans/sec
Throughput:                     0.04 MB/sec
Concurrency:                   99.29
Successful transactions:       15109
Failed transactions:               0
Longest transaction:            7.33
Shortest transaction:           0.01
```
#9.ZERO-DOWNTIME DEPLOY
- 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함 (위의 시나리오에서 제거되었음)
```console
kubectl delete horizontalpodautoscaler.autoscaling/paymethod -n ssak5
```
- yaml 설정 참고
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: paymethod
  namespace: ssak5
  labels:
    app: paymethod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: paymethod
  template:
    metadata:
      labels:
        app: paymethod
    spec:
      containers:
        - name: paymethod
          image: ssak5acr.azurecr.io/paymethod:1.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          env:
            - name: api.url.payment
              valueFrom:
                configMapKeyRef:
                  name: ssak5-config
                  key: api.url.payment
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5

---

apiVersion: v1
kind: Service
metadata:
  name: paymethod
  namespace: ssak5
  labels:
    app: paymethod
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: paymethod
```

- siege 로 배포작업 직전에 워크로드를 모니터링 함.
```console
siege -v -c1 -t120S -r10 --content-type "application/json" 'http://paymethod:8080/paymethods POST {"kind": "credit","number": 40095003,"requestId": 1,"payKindRegStatus": "PaymentKindRegistered"}'
```

- 새버전으로의 배포 시작
```
# 컨테이너 이미지 Update (readness, liveness 미설정 상태)
kubectl apply -f paymethod_na.yaml
```

- siege 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인
```console
Lifting the server siege...
Transactions:                  23574 hits
Availability:                  97.68 %
Elapsed time:                 299.64 secs
Data transferred:               7.52 MB
Response time:                  0.01 secs
Transaction rate:              76.71 trans/sec
Throughput:                     0.03 MB/sec
Concurrency:                    0.97
Successful transactions:       23574
Failed transactions:             308
Longest transaction:            0.97
Shortest transaction:           0.00

```

- 배포기간중 Availability 가 평소 100%에서 97% 대로 떨어지는 것을 확인 후 Readiness Probe 를 설정함:
```console
# deployment.yaml 의 readiness probe 의 설정:
kubectl apply -f paymethod.yaml

NAME                               READY   STATUS    RESTARTS   AGE
pod/cleaning-bf474f568-vxl8r       2/2     Running   0          4h3m
pod/dashboard-7f7768bb5-7l8wr      2/2     Running   0          4h1m
pod/gateway-6dfcbbc84f-rwnsh       2/2     Running   0          143m
pod/message-69597f6864-fjs69       2/2     Running   0          92m
pod/payment-7749f7dc7c-kfjxb       2/2     Running   0          97m
pod/paymethod-646dcb9ffb-llrzk     2/2     Running   0          60m
pod/reservation-775fc6574d-nfnxx   1/1     Running   0          3m54s
pod/siege                          2/2     Running   0          5h24m

```
- 동일한 시나리오로 재배포 한 후 Availability 확인
```console
Lifting the server siege...
Transactions:                   6423 hits
Availability:                 100.00 %
Elapsed time:                 119.51 secs
Data transferred:               2.17 MB
Response time:                  0.02 secs
Transaction rate:              55.75 trans/sec
Throughput:                     0.02 MB/sec
Concurrency:                    0.98
Successful transactions:        6423
Failed transactions:               0
Longest transaction:            0.86
Shortest transaction:           0.00
```

- 배포기간 동안 Availability 100%이므로 무정지 재배포가 성공한 것으로 확인

#10.CONFIGMAP / PERSISTENCE VOLUME
- 시스템별로 또는 운영중에 동적으로 변경 가능성이 있는 설정들을 ConfigMap을 사용하여 관리
- configmap.yaml
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ssak5-config
  namespace: ssak5
data:
  api.url.payment: http://payment:8080
```

- paymethod.yaml (configmap 사용)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: paymethod
  namespace: ssak5
  labels:
    app: paymethod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: paymethod
  template:
    metadata:
      labels:
        app: paymethod
    spec:
      containers:
        - name: paymethod
          image: ssak5acr.azurecr.io/paymethod:1.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          env:
            - name: api.url.payment
              valueFrom:
                configMapKeyRef:
                  name: ssak5-config
                  key: api.url.payment
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5

---

apiVersion: v1
kind: Service
metadata:
  name: paymethod
  namespace: ssak5
  labels:
    app: paymethod
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: paymethod
```
- configmap 설정 정보 확인
```console
kubectl describe pod/paymethod-775fc6574d-kddgd -n ssak5

...중략
Containers:
  paymethod:
    Container ID:   docker://af733ea1c805029ad0baf5c448981b3b84def8e4c99656638f2560b48b14816e
    Image:          ssak5acr.azurecr.io/paymethod:1.0
    Image ID:       docker-pullable://ssak5acr.azurecr.io/paymethod@sha256:5a9eb3e1b40911025672798628d75de0670f927fccefea29688f9627742e3f6d
    Port:           8080/TCP
    Host Port:      0/TCP
    State:          Running
      Started:      Tue, 08 Sep 2020 13:24:05 +0000
    Ready:          True
    Restart Count:  0
    Liveness:       http-get http://:8080/actuator/health delay=120s timeout=2s period=5s #success=1 #failure=5
    Readiness:      http-get http://:8080/actuator/health delay=10s timeout=2s period=5s #success=1 #failure=10
    Environment:
      api.url.payment:  <set to the key 'api.url.payment' of config map 'ssak5-config'>  Optional: false
    Mounts:
      /var/run/secrets/kubernetes.io/serviceaccount from default-token-w4fh5 (ro)
...중략
```
#11.POLYGLOT
#12.SELF-HEALING(LIVENESS PROBE)
* Paymethod\kubernetes\deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: Paymethod
  labels:
    app: Paymethod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: Paymethod
  template:
    metadata:
      labels:
        app: Paymethod
    spec:
      containers:
        - name: Paymethod
          image: username/Paymethod:latest
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```
