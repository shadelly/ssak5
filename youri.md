#1.SAGA
#2.CQRS
#3.CORERELATION
#4.REQ/RESP
#5.GATEWAY
#6.DEPLOY/PIPELINE
#7.CIRCUIT BREAKER
#8.AUTOSCALE(HPA)
#9.ZERO-DOWNTIME DEPLOY
- 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함 (위의 시나리오에서 제거되었음)
```console
kubectl delete horizontalpodautoscaler.autoscaling/paymethod -n ssak5
```
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
    Image:          ssak5acr.azurecr.io/reservation:1.0
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
