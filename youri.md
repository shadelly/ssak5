#1.SAGA
- 결제수단을 등록하면 결제가 완료되었다는 메시지를 알림 서비스에 알림이력으로 등록하고 Dashboard에서 결제수단을 보여준다.
* 결제수단 등록
``` console
http POST http://paymethod:8080/paymethods kind=credit number=40095003 requestId=1 payKindRegStatus=PaymentKindRegistered

```
* Message 확인
```
root@siege:/# http GET http://message:8080/messages
HTTP/1.1 200 OK
content-type: application/hal+json;charset=UTF-8
date: Thu, 10 Sep 2020 02:58:15 GMT
server: envoy
transfer-encoding: chunked
x-envoy-upstream-service-time: 26

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
                }
            }
        } 
    }
}
```
* Dashboard에서 확인
```console
root@siege:/# http GET http://dashboard:8080/dashBoardViews
   HTTP/1.1 200 OK
   content-type: application/hal+json;charset=UTF-8
   date: Thu, 10 Sep 2020 02:52:50 GMT
   server: envoy
   transfer-encoding: chunked
   x-envoy-upstream-service-time: 131
   
   {
       "_embedded": {
           "dashBoardViews": [
               {
                   "_links": {
                       "dashBoardView": {
                           "href": "http://dashboard:8080/dashBoardViews/1"
                       },
                       "self": {
                           "href": "http://dashboard:8080/dashBoardViews/1"
                       }
                   },
                   "payKind": "credit",
                   "place": null,
                   "price": null,
                   "requestDate": null,
                   "requestId": null,
                   "status": "ReservationApply"
               }
            }
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
-gateway 사용
```yaml

server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: Reservation
          uri: http://localhost:8081
          predicates:
            - Path=/cleaningReservations/** 
        - id: Cleaning
          uri: http://localhost:8082
          predicates:
            - Path=/cleans/** 
        - id: Payment
          uri: http://localhost:8083
          predicates:
            - Path=/payments/** 
        - id: DashBoard
          uri: http://localhost:8084
          predicates:
            - Path= /dashBoardViews/**
        - id: Message
          uri: http://localhost:8085
          predicates:
            - Path=/messages/** 
        - id: Paymethod
          uri: http://localhost:8086
          predicates:
            - Path=/paymethods/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: Reservation
          uri: http://Reservation:8080
          predicates:
            - Path=/cleaningReservations/** 
        - id: Cleaning
          uri: http://Cleaning:8080
          predicates:
            - Path=/cleans/** 
        - id: Payment
          uri: http://Payment:8080
          predicates:
            - Path=/payments/** 
        - id: DashBoard
          uri: http://DashBoard:8080
          predicates:
            - Path= /dashBoardViews/**
        - id: Message
          uri: http://Message:8080
          predicates:
            - Path=/messages/** 
        - id: Paymethod
          uri: http://Paymethod:8080
          predicates:
            - Path=/paymethods/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080

```
#4.REQ/RESP

- 결제수단 관리에서 결제 서비스를 호출하기 위하여 FeignClient를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 
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
- 60초 동안 실시
```console
siege -v -c100 -t60S -r10 --content-type "application/json" 'http://paymethod:8080/paymethods POST {"kind": "credit","number": 40095003,"requestId": 1,"payKindRegStatus": "PaymentKindRegistered"}'

HTTP/1.1 201     0.91 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.82 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.92 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.12 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.87 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     3.67 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.06 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.87 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.09 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.80 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     1.48 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.89 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.12 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.12 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.11 secs:     291 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 201     0.03 secs:     291 bytes ==> POST http://paymethod:8080/paymethods

Lifting the server siege...
Transactions:                   6551 hits
Availability:                 100.00 %
Elapsed time:                  59.21 secs
Data transferred:               1.82 MB
Response time:                  0.89 secs
Transaction rate:             110.64 trans/sec
Throughput:                     0.03 MB/sec
Concurrency:                   98.65
Successful transactions:        6551
Failed transactions:               0
Longest transaction:            4.20
Shortest transaction:           0.02

```
* kiali에서 확인
![image](https://user-images.githubusercontent.com/68408649/92673004-08956680-f355-11ea-8b12-3dd837f5949a.png)

* 서킷 브레이킹을 위한 DestinationRule 적용
```
cd ssak5/yaml
kubectl apply -f payment_dr.yaml

# destinationrule.networking.istio.io/dr-payment created
# 다시 부하테스트 수행

HTTP/1.1 500     0.49 secs:     252 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 500     0.90 secs:     252 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 500     0.78 secs:     252 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 500     0.91 secs:     252 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 500     0.82 secs:     252 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 500     0.78 secs:     252 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 500     0.78 secs:     252 bytes ==> POST http://paymethod:8080/paymethods
HTTP/1.1 500     0.59 secs:     252 bytes ==> POST http://paymethod:8080/paymethods
siege aborted due to excessive socket failure; you
can change the failure threshold in $HOME/.siegerc

Transactions:                    376 hits
Availability:                  25.08 %
Elapsed time:                  12.14 secs
Data transferred:               0.37 MB
Response time:                  3.17 secs
Transaction rate:              30.97 trans/sec
Throughput:                     0.03 MB/sec
Concurrency:                   98.24
Successful transactions:         376
Failed transactions:            1123
Longest transaction:            4.38
Shortest transaction:           0.02
```
- DestinationRule 적용되어 서킷 브레이킹 동작 확인 (kiali 화면)
![image](https://user-images.githubusercontent.com/68408649/92673438-0f70a900-f356-11ea-9a75-66989605e292.png)

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

admin5@ssak5-vm:~/ssak5$ kubectl get all -n ssak5
NAME                               READY   STATUS    RESTARTS   AGE
pod/cleaning-745f4b7566-lrn6w      2/2     Running   0          14h
pod/dashboard-5c68d447f5-kjdpk     2/2     Running   0          14h
pod/gateway-5489b49b67-zgw98       2/2     Running   0          39m
pod/message-5975967f78-dccfq       2/2     Running   0          39m
pod/payment-6dbbfc7cf5-r9jb4       2/2     Running   0          39m
pod/paymethod-646dcb9ffb-llrzk     2/2     Running   0          10h
pod/reservation-79596c74b8-c2djj   2/2     Running   0          38m
pod/siege                          2/2     Running   0          13h

NAME                  TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)    AGE
service/cleaning      ClusterIP   10.0.55.227    <none>        8080/TCP   14h
service/dashboard     ClusterIP   10.0.108.26    <none>        8080/TCP   14h
service/gateway       ClusterIP   10.0.248.84    <none>        8080/TCP   39m
service/message       ClusterIP   10.0.114.150   <none>        8080/TCP   39m
service/payment       ClusterIP   10.0.219.35    <none>        8080/TCP   39m
service/paymethod     ClusterIP   10.0.234.6     <none>        8080/TCP   14h
service/reservation   ClusterIP   10.0.233.100   <none>        8080/TCP   38m

NAME                          READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/cleaning      1/1     1            1           14h
deployment.apps/dashboard     1/1     1            1           14h
deployment.apps/gateway       1/1     1            1           39m
deployment.apps/message       1/1     1            1           39m
deployment.apps/payment       1/1     1            1           39m
deployment.apps/paymethod     1/1     1            1           14h
deployment.apps/reservation   1/1     1            1           38m

NAME                                     DESIRED   CURRENT   READY   AGE
replicaset.apps/cleaning-745f4b7566      1         1         1       14h
replicaset.apps/cleaning-8884cb4f4       0         0         0       14h
replicaset.apps/dashboard-5c68d447f5     1         1         1       14h
replicaset.apps/dashboard-768c6c58bc     0         0         0       14h
replicaset.apps/gateway-5489b49b67       1         1         1       39m
replicaset.apps/message-5975967f78       1         1         1       39m
replicaset.apps/payment-6dbbfc7cf5       1         1         1       39m
replicaset.apps/paymethod-646dcb9ffb     1         1         1       10h
replicaset.apps/paymethod-6db58f6bb7     0         0         0       14h
replicaset.apps/paymethod-797b9b6f66     0         0         0       14h
replicaset.apps/reservation-79596c74b8   1         1         1       38m

NAME                                            REFERENCE              TARGETS         MINPODS   MAXPODS   REPLICAS   AGE
horizontalpodautoscaler.autoscaling/paymethod   Deployment/paymethod   <unknown>/15%   1         3         1          27s
```

- CB 에서 했던 방식대로 워크로드를 1분 동안 걸어준다.
```console
siege -v -c100 -t180S -r10 --content-type "application/json" 'http://paymethod:8080/paymethods POST {"kind": "credit","number": 40095003,"requestId": 1,"payKindRegStatus": "PaymentKindRegistered"}'
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다
```console
kubectl get deploy paymethod -n ssak5 -w 

NAME      READY   UP-TO-DATE   AVAILABLE     AGE
paymethod   1/1     1            1           14h

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
- paymethod.yaml 설정 참고
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
