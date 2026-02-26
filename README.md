#  Banking System (MSA 기반 뱅킹 시스템)

> Spring Boot와 MSA(Microservices Architecture)를 기반으로 구현한 뱅킹(계좌 이체 등) 시스템입니다. 
> 트랜잭션의 정합성 보장부터 Terraform을 활용한 인프라 구축, CI/CD 파이프라인 자동화, 그리고 모니터링까지 전체 시스템의 라이프사이클을 설계하고 구축했습니다.

<br/>

##  아키텍처 및 시스템 구성
```
[사용자 브라우저]
   │
   ├──> (:5173) [ Frontend (React) ]
   │                 │
   ├──> (:8000) [ API Gateway (JWT 검증/라우팅) ]
   │                 ├──> Auth Service (:8081) ──> [ MySQL 8.4 ]
   │                 ├──> Core Banking (:8080) ──> [ MySQL 8.4 ]
   │                 ├──> Other APIs     
   │                 └──> . . .         
   │                     
   │
   └──> (:3000) [ Grafana ] ◄── [ Prometheus ]
```
CI
```
PR 작성
│
├── API 1 테스트 + JaCoCo 커버리지
├── API 2 테스트 + JaCoCo 커버리지
├── . . .
├── frontend 빌드
│
└── SonarCloud 분석
    → PR에 코드 품질 리포트 자동 코멘트
```
CD
 ```text
  main 브랜치에 머지
    │
    ▼
[테스트]  전체 테스트 재실행
    │
    ▼
[빌드]   Docker 이미지 빌드
    │
    ▼
[푸시]   ECR에 이미지 업로드
    │
    ▼
[배포]   ECS에 롤링 배포 (새 컨테이너 기동 → 헬스체크 → 이전 컨테이너 종료)
    │
    ▼
[프론트] npm build → S3 업로드 → CloudFront 캐시 무효화
 ```
AWS
```
Internet → CloudFront → ALB
                          │
                          ├─────────────────
                          ├──> api-gateway   │
                          ├──> auth-service  │ ──> RDS MySQL
                          ├──> core-banking  │ ──> RDS MySQL
                          └──> Other APIs    │
                          └──────────────────
                                    └───>ECS Fargate
```
<br/>

##  기술 스택

### Backend
- **Language/Framework:** Java 21, Spring Boot 3.4, Spring Cloud Gateway
- **ORM:** JPA (Hibernate)

### Frontend
- **Framework/Build:** React 18, Vite

### Database
- **RDBMS:** MySQL 8.4

### Infrastructure & DevOps
- **Cloud:** AWS (ECS Fargate, RDS, ALB, VPC)
- **IaC:** Terraform
- **Container:** Docker

### CI/CD & Code Quality
- **Pipeline:** GitHub Actions
- **Quality & Testing:** SonarCloud, JaCoCo, JUnit 5

### Monitoring
- **Metrics & Dashboard:** Prometheus, Grafana
<br/>

##  핵심 기술 및 구현 포인트 

### 1. 금융 트랜잭션의 정합성 및 동시성 제어 (Core Banking)
- **동시성 제어:** `PESSIMISTIC_WRITE` (비관적 락)를 활용해, 동시 출금 요청 시 발생할 수 있는 갱신 손실(Lost Update)을 방지하고 순차적 처리를 보장했습니다.
- **데드락(Deadlock) 방지:** 다중 계좌 간 교차 이체 시 발생하는 교착 상태를 막기 위해, 계좌 ID를 오름차순으로 정렬하여 항상 일관된 순서로 락(Lock)을 획득하도록 구현했습니다.
- **멱등성(Idempotency) 보장:** 네트워크 지연이나 사용자의 연속 클릭으로 결제 요청이 중복해서 들어오더라도, 클라이언트가 생성한 고유 `transactionId`를 확인하여 똑같은 결제가 두 번 일어나지 않도록 원천 차단했습니다.

### 2. API Gateway 인증 중앙화
- **JWT 검증 일원화:** 시스템의 관문인 Spring Cloud Gateway에서 JWT 토큰 검증을 전담하고, 하위 마이크로서비스(Auth, Core 등)에는 `X-User-UUID` 헤더로 인가 정보를 전달하도록 설계했습니다. 이를 통해 불필요한 중복 코드를 없애고 보안 관리를 단순화했습니다.

### 3. 인프라 구축 자동화 (Terraform)
- **IaC 기반 클라우드 구축:** AWS 서버와 네트워크 설정(VPC, DB 등)을 수동으로 작업하지 않고 코드(Terraform)로 관리합니다. 인프라 설정 오류를 줄이고, 언제든 동일한 서버 환경을 빠르고 정확하게 재현해 낼 수 있습니다.

### 4. CI/CD 및 코드 품질 관리 자동화
- **CI (지속적 통합):** GitHub Actions를 통해 PR 생성 시 자동 빌드, 테스트, JaCoCo 커버리지 측정을 수행합니다. SonarCloud와 연동하여 취약점 및 코드 스멜(Code Smell)을 PR에 자동 리뷰 코멘트로 남기도록 파이프라인을 구축했습니다.


        

- **CD (지속적 배포):** `main` 브랜치 병합 시 ECR에 Docker 이미지를 푸시하고, ECS 롤링 배포(Rolling Update)를 트리거하여 서비스 중단 없이 안전하게 신규 버전을 릴리즈합니다.



### 5. 실시간 모니터링 (Monitoring)
- **대시보드 구축:** Prometheus와 Grafana를 연동하여 에러 감지, API 응답 지연 시간, JVM 메모리 사용량 등 시스템의 핵심 메트릭을 실시간으로 추적할 수 있는 환경을 구축했습니다.
