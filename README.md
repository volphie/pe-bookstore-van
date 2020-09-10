# pe-bookstore-van
# cna-bookstore

## User Scenario
```
* 고객 또는 고객 운영자는 고객정보를 생성한다.
* 북 관리자는 판매하는 책의 종류와 보유 수량을 생성하고 수정할 수 있다.
* 고객은 책의 주문과 취소가 가능하며 주문 정보의 수정은 없다.
* 고객이 주문을 생성할 때 고객정보와 Book 정보가 있어야 한다.
  - Order -> Customer 동기호출
  - Order -> BookInventory 동기호출
  - Customer 서비스가 중지되어 있더라도 주문은 생성하되 주문상태를 "Customer_Not_Verified"로 설정하여 타 서비스로의 전달은 진행하지 않는다.
* 주문 시에 재고가 없더라도 주문이 가능하다.
  - 주문 상태는 “Ordered”
* 주문 취소는 "Ordered" 상태일 경우만 가능하다.
* 배송준비는 주문 정보를 받아 이뤄지며 재고가 부족한 경우, 책 입고가 이뤄져서 재고 수량이 충분한 경우에 배송 준비가 완료되었음을 알린다.
* 배송은 주문 생성 정보를 받아서 배송을 준비하고 주문 상품 준비 정보를 받아서 배송을 시작하며 배송이 시작되었음을 주문에도 알린다.
  - 주문 생성 시 배송 생성
  - 상품 준비 시 배송 시작  
* 배송을 시행하는 외부 시스템(물류 회사 시스템) 또는 배송 담당자는 배송 단계별로 상태는 변경한다. 변경된 배송 상태는 주문에 알려 반영한다.
* 주문 취소되더라도 고객은 MyPage에서 주문 이력을 모두 조회할 수 있다.

== 신규 기능/서비스 ==
* 결재 서비스가 주문 내용을 받아서 Van사와 결제를 실행하고 그 결과를 메시지로 전달한다.
  - Van 사와의 호출은 동기호출
  - 주문받은 내용은 비동기로 전달된다.
* 결재 이력은 별도의 CQRS 서비스로 구현하고 결제 메시지가 전달되서 저장되도록 한다.  
```

## 아키텍처
```
* 모든 요청은 단일 접점을 통해 이뤄진다.

```

## Cloud Native Application Model
![Alt text](model.PNG?raw=true "Optional Title")

## 구현 점검

### 모든 서비스 정상 기동 
```
* Httpie Pod 접속
kubectl exec -it httpie -- bash

* API 
http http://gateway:8080/payments
http http://gateway:8080/paymentHistories
http http://gateway:8080/orders
http http://gateway:8080/requestPayment
```

### Kafka 기동 및 모니터링 용 Consumer 연결
```
kubectl -n kafka exec -ti my-kafka-0 -- /usr/bin/kafka-console-consumer --bootstrap-server my-kafka:9092 --topic cnabookstore --from-beginning
```

### 주문 생성
```
http POST http://gateway:8080/orders bookId=1 customerId=1 quantity=100 deliveryAddress="busan si" payAmount=1000.0 payType=ONCE
```

##### Message 전송 확인 결과
```
{"eventType":"Ordered","timestamp":"20200910045035","orderId":4,"bookId":1,"customerId":1,"quantity":100,"deliveryAddress":"busan si","orderStatus":"ORDERED","payAmount":1000.0,"payType":"ONCE","me":true}
{"eventType":"Payed","timestamp":"20200910045036","orderId":4,"paymentId":1,"payStatus":"PAYMENT_COMPLETED","me":true}
```

##### Payment 확인 결과
```
root@httpie:/# http http://gateway:8080/payments
HTTP/1.1 200 OK
Content-Type: application/hal+json;charset=UTF-8
Date: Thu, 10 Sep 2020 04:52:14 GMT
transfer-encoding: chunked

{
    "_embedded": {
        "payments": [
            {
                "_links": {
                    "payment": {
                        "href": "http://payment:8080/payments/1"
                    }, 
                    "self": {
                        "href": "http://payment:8080/payments/1"
                    }
                }, 
                "amount": 1000.0, 
                "orderId": 4, 
                "status": "PAYMENT_COMPLETED", 
                "type": "ONCE"
            }
        ]
    }, 
    "_links": {
        "profile": {
            "href": "http://payment:8080/profile/payments"
        }, 
        "self": {
            "href": "http://payment:8080/payments{?page,size,sort}", 
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

### Payment History 확인(CQRS)
```
root@httpie:/# http http://gateway:8080/paymentHistoriesHTTP/1.1 200 OK
Content-Type: application/hal+json;charset=UTF-8
Date: Thu, 10 Sep 2020 04:53:38 GMT
transfer-encoding: chunked

{
    "_embedded": {
        "paymentHistories": [
            {
                "_links": {
                    "paymentHistory": {
                        "href": "http://paymenthistory:8080/paymentHistories/1"
                    }, 
                    "self": {
                        "href": "http://paymenthistory:8080/paymentHistories/1"
                    }
                }, 
                "orderId": 1, 
                "payStatus": "PAYMENT_COMPLETED", 
                "paymentId": 1
            }, 
            {
                "_links": {
                    "paymentHistory": {
                        "href": "http://paymenthistory:8080/paymentHistories/2"
                    }, 
                    "self": {
                        "href": "http://paymenthistory:8080/paymentHistories/2"
                    }
                }, 
                "orderId": 2, 
                "payStatus": "PAYMENT_FAILED", 
                "paymentId": 2
            }, 
            {
                "_links": {
                    "paymentHistory": {
                        "href": "http://paymenthistory:8080/paymentHistories/3"
                    }, 
                    "self": {
                        "href": "http://paymenthistory:8080/paymentHistories/3"
                    }
                }, 
                "orderId": 3, 
                "payStatus": "PAYMENT_FAILED", 
                "paymentId": 3
            }, 
            {
                "_links": {
                    "paymentHistory": {
                        "href": "http://paymenthistory:8080/paymentHistories/4"
                    }, 
                    "self": {
                        "href": "http://paymenthistory:8080/paymentHistories/4"
                    }
                }, 
                "orderId": 4, 
                "payStatus": "PAYMENT_COMPLETED", 
                "paymentId": 1
            }
        ]
    }, 
    "_links": {
        "profile": {
            "href": "http://paymenthistory:8080/profile/paymentHistories"
        }, 
        "self": {
            "href": "http://paymenthistory:8080/paymentHistories"
        }
    }
```

## CI/CD 점검

## Circuit Breaker 점검

```
Hystrix Command
	5000ms 이상 Timeout 발생 시 CircuitBearker 발동

CircuitBeaker 발생
	http http://van:8080/requestPayment?payment=0
		- 잘못된 쿼리 수행 시 CircuitBeaker
		- 10000ms(10sec) Sleep 수행
		- 5000ms Timeout으로 CircuitBeaker 발동
		- 10000ms(10sec) 
```

```
실행 결과

root@httpie:/# http http://van:8080/requestPayment?payment=100
HTTP/1.1 200 
Content-Length: 17
Content-Type: text/plain;charset=UTF-8
Date: Thu, 10 Sep 2020 05:11:56 GMT

PAYMENT_COMPLETED

root@httpie:/# http http://van:8080/requestPayment?payment=0  
HTTP/1.1 200 
Content-Length: 14
Content-Type: text/plain;charset=UTF-8
Date: Thu, 10 Sep 2020 05:12:12 GMT

PAYMENT_FAILED

```
소스 코드
```
    @GetMapping("/requestPayment")
    @HystrixCommand(fallbackMethod = "fallBackPayment", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000"),
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000")
    })
    public @ResponseBody String requestPayment(@RequestParam long payment) throws InterruptedException {
        System.out.println("@@@ requestPayment!!!");
        if (payment == 0) {
            System.out.println("@@@ CircuitBreaker!!!");
            Thread.sleep(10000);
            return "PAYMENT_FAILED";
        } else {
            System.out.println("@@@ Success!!!");
            return "PAYMENT_COMPLETED";
        }

    }

    public String fallBackPayment(long payment ){
        System.out.println("### fallback!!!");
        return "PAYMENT_FAILED";
    }

```

## Autoscale 점검
### 설정 확인
```
application.yaml 파일 설정 변경
(https://k8s.io/examples/application/php-apache.yaml 파일 참고)
 resources:
  limits:
    cpu: 500m
  requests:
    cpu: 200m
```
### 점검 순서
```
1. HPA 생성 및 설정
	kubectl autoscale deploy van --min=1 --max=10 --cpu-percent=30
	kubectl get hpa van -o yaml
2. 모니터링 걸어놓고 확인
	kubectl get hpa van -w
	watch kubectl get deploy,po
3. Siege 실행
  siege -c10 -t60S -v 'http://gateway:8080/requestPayment?payment=1000'
```
### 점검 결과
root@labs--1357583724:~# kubectl get hpa van -w
NAME   REFERENCE        TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
van    Deployment/van   1%/30%    1         10        1          19s
van    Deployment/van   92%/30%   1         10        1          77s
van    Deployment/van   92%/30%   1         10        4          92s
van    Deployment/van   29%/30%   1         10        4          2m17s
van    Deployment/van   3%/30%    1         10        4          3m18s
van    Deployment/van   2%/30%    1         10        4          4m19s
van    Deployment/van   2%/30%    1         10        4          8m7s
van    Deployment/van   1%/30%    1         10        1          8m23s
van    Deployment/van   1%/30%    1         10        1          9m23s
van    Deployment/van   1%/30%    1         10        1          11m
van    Deployment/van   1%/30%    1         10        1          12m


## Readiness Probe 점검
### 설정 확인
```
readinessProbe:
  httpGet:
    path: '/isReady'
    port: 8080
  initialDelaySeconds: 12
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 10
```
### 점검 순서
#### 1. Siege 실행
```
siege -c2 -t120S  -v 'http://van:8080/isReady
```
#### 2. Readiness 설정 제거 후 배포
#### 3. Siege 결과 Availability 확인(100% 미만)
```
Lifting the server siege...      done.

Transactions:                    330 hits
Availability:                  70.82 %
Elapsed time:                 119.92 secs
Data transferred:               0.13 MB
Response time:                  0.02 secs
Transaction rate:               2.75 trans/sec
Throughput:                     0.00 MB/sec
Concurrency:                    0.07
Successful transactions:         330
Failed transactions:             136
Longest transaction:            1.75
Shortest transaction:           0.00
```
#### 4. Readiness 설정 추가 후 배포

#### 6. Siege 결과 Availability 확인(100%)
```
Lifting the server siege...      done.

Transactions:                    443 hits
Availability:                 100.00 %
Elapsed time:                 119.60 secs
Data transferred:               0.51 MB
Response time:                  0.01 secs
Transaction rate:               3.70 trans/sec
Throughput:                     0.00 MB/sec
Concurrency:                    0.04
Successful transactions:         443
Failed transactions:               0
Longest transaction:            0.18
Shortest transaction:           0.00
 
FILE: /var/log/siege.log
```

## Liveness Probe 점검
### 설정 확인
```
livenessProbe:
  httpGet:
    path: '/isHealthy'
    port: 8080
  initialDelaySeconds: 120
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 5
```
### 점검 순서 및 결과
#### 1. 기동 확인
```
http http://van:8080/requestPayment?payment=1000
```
#### 2. 상태 확인
```
root@httpie:/# http http://van:8080/isHealthy
HTTP/1.1 200 
Content-Length: 0
Date: Thu, 10 Sep 2020 05:00:19 GMT
```

#### 3. 상태 변경
```
root@httpie:/# http http://van:8080/makeZombie
HTTP/1.1 200 
Content-Length: 0
Date: Thu, 10 Sep 2020 05:07:39 GMT
```
#### 4. 상태 확인
```
root@httpie:/# http http://van:8080/isHealthy
HTTP/1.1 500 
Connection: close
Content-Type: application/json;charset=UTF-8
Date: Thu, 10 Sep 2020 05:07:44 GMT
Transfer-Encoding: chunked

{
    "error": "Internal Server Error", 
    "message": "Zombie...", 
    "path": "/isHealthy", 
    "status": 500, 
    "timestamp": "2020-09-10T05:07:44.652+0000"
}
```
#### 5. Pod 재기동 확인
```

NAME                              READY   STATUS    RESTARTS   AGE
gateway-db9bdbdb7-nknzs           1/1     Running   0          3h42m
httpie                            1/1     Running   0          18h
order-56fb5bb9d6-57g2m            1/1     Running   0          4h49m
payment-57ddfb8576-wlldx          1/1     Running   0          44m
paymenthistory-5654c5447f-tphnn   1/1     Running   0          3h43m
van-c5fd5c97d-4qxdd               0/1     Running   0          6s
van-c5fd5c97d-9fx6f               0/1     Running   0          6s


root@httpie:/# http http://van:8080/isHealthy

http: error: ConnectionError: HTTPConnectionPool(host='van', port=8080): Max retries exceeded with url: /isHealthy (Caused by NewConnectionError('<requests.packages.urllib3.connection.HTTPConnection object at 0x7fe0e7101810>: Failed to establish a new connection: [Errno 111] Connection refused',))
root@httpie:/# 
```
