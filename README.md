#  MSA-Based Banking System

> Spring Bootと MSA（Microservices Architecture）を基盤に実装したBanking（口座振込など）システムです。
> Transactionの整合性の保証から Terraformを活用したインフラ構築、CI/CD pipelineの自動化、そしてmonitoringに至るまで、システム全体のLife cycleを設計・構築しました。

<br/>

##  Architecture & System Configuration
```
[User Browsser]
   │
   ├──> (:5173) [ Frontend (React) ]
   │                 │
   ├──> (:8000) [ API Gateway (JWT Authentication / Routing) ]
   │                 ├──> Auth Service (:8081) ──> [ MySQL ]
   │                 ├──> Core Banking (:8080) ──> [ MySQL ]
   │                 ├──> Other APIs     
   │                 └──> . . .         
   │                     
   │
   └──> (:3000) [ Grafana ] ◄── [ Prometheus ]
```
CI
```
PR Creation
│
├── API 1 Test + JaCoCo Coverage
├── API 2 Test + JaCoCo Coverage
├── . . .
├── frontend Build
│
└── SonarCloudAnalysis
    → Automated Code Quailty Reporting on PR
```
CD
 ```text
  main / Merge 
    │
    ▼
[Test]  Execute full test
    │
    ▼
[Build]   Build Docker image
    │
    ▼
[Push]   Push image to AWS ECR
    │
    ▼
[Deploy]   Rolling deployment to AWS ECS (Start new container → Health check → Terminate old container)
    │
    ▼
[Frontend] npm build → Upload to AWS S3 → Invalidate CloudFront cache
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

##  Tech Stack

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

##  Key Technologies & Implementation

### 1. 金融 Transactionの整合性および同時実行制御（Core Banking）
- **同時実行制御（PESSIMISTIC_WRITE）:** `PESSIMISTIC_WRITE`（悲観的ロック）を活用し、同時出金リクエスト時に発生する更新喪失（Lost Update）を防止し、順次処理を保証しました。
- **デッドロック（Deadlock）の防止:** 複数口座間の交差振込時に発生するデッドロックを防ぐため、口座IDを昇順に整列し、常に一貫した順序でロック（Lock）を取得するように実装しました。
- **冪等性（Idempotency）の保証:** ネットワーク遅延やユーザーの連続クリックにより決済リクエストが重複した場合でも、クライアントが生成した固有の `transactionId`を検証し、二重決済が発生しないよう遮断しました。

### 2. API Gatewayにおける認証の集約化
- **JWT検証の一元化:** システムの関門であるSpring Cloud GatewayでJWT Tokenの検証を専担し、配下のマイクロサービス（Auth、Coreなど）には `X-User-UUID` ヘッダーで認可情報を伝達するよう設計しました。これにより、不要な重複コードを排除し、セキュリティ管理を簡素化しました。

### 3. インフラ構築の自動化 (Terraform)
- **IaC基盤のクラウド構築:** AWSサーバーやネットワーク設定（VPC、DBなど）を手動で作業せず、コード（Terraform）で管理しています。インフラ設定のヒューマンエラーを減らし、いつでも同一のサーバー環境を迅速かつ正確に再現できるようにしました。

### 4. CI/CDおよびコード品質管理の自動化
- **CI（継続的インテグレーション）:** GitHub Actionsを利用し、PR作成時に自動ビルド、テスト、JaCoCoによるカバレッジ測定を実行します。さらにSonarCloudと連携し、脆弱性やCode SmellをPRへ自動でレビューコメントとして残すパイプラインを構築しました。
- **CD（継続的デプロイ）:** `main` BranchへのMerge時にECRへDockerイメージをpushし、ECSのRolling Updateをトリガーして、無停止（Zero down time）で安全に新バージョンをリリースします。

### 5. リアルタイムモニタリング（Monitoring）
- **ダッシュボードの構築:** PrometheusとGrafanaを連携し、エラーの検知、APIの応答遅延、JVMのメモリ使用量など、システムの重要なMetricsをリアルタイムで追跡・可視化できる環境を構築しました。
