# ============================================================
# ECS Fargate - 컨테이너 오케스트레이션
# ============================================================
# docker-compose를 AWS로 옮긴 것이라고 이해하면 됩니다.
#
# 대응 관계:
#   docker-compose.yaml    →  Terraform ECS 설정
#   services:              →  aws_ecs_service
#   image:                 →  aws_ecs_task_definition (container image)
#   ports:                 →  aws_ecs_task_definition (portMappings)
#   environment:           →  aws_ecs_task_definition (environment)
#   docker-compose up      →  terraform apply
#
# Fargate란?
#   EC2 없이 컨테이너를 실행하는 서버리스 서비스.
#   서버 관리(OS 패치, 스케일링)가 불필요합니다.
#   → "컨테이너 정의만 하면 AWS가 알아서 서버를 할당"
# ============================================================

# ── ECS Cluster ──
# 여러 서비스를 묶는 논리적 그룹 (docker-compose의 project name과 유사)
resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-${var.environment}-cluster"

  # Container Insights: CPU/메모리/네트워크 메트릭을 CloudWatch에 자동 수집
  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = { Name = "${var.project_name}-cluster" }
}

# ============================================================
# IAM Role (권한 설정)
# ============================================================
# ECS가 AWS 서비스를 사용하기 위한 권한입니다.
#
# 2종류:
#   Task Execution Role → ECS가 ECR에서 이미지 pull, CloudWatch에 로그 전송
#   Task Role           → 컨테이너 내부에서 AWS 서비스 호출 시 사용
# ============================================================

# Task Execution Role: ECS 인프라 수준의 권한
resource "aws_iam_role" "ecs_task_execution" {
  name = "${var.project_name}-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

# AWS 기본 정책 연결 (ECR pull + CloudWatch 로그)
resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Task Role: 컨테이너 애플리케이션 수준의 권한
resource "aws_iam_role" "ecs_task" {
  name = "${var.project_name}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

# ============================================================
# CloudWatch Log Groups (로그 수집)
# ============================================================
# docker logs 대신 CloudWatch에 로그가 저장됩니다.
# 30일간 보관 후 자동 삭제 (비용 관리)
# ============================================================

resource "aws_cloudwatch_log_group" "services" {
  for_each = toset(["api-gateway", "auth-service", "core-banking"])

  name              = "/ecs/${var.project_name}/${each.value}"
  retention_in_days = 30

  tags = { Name = "${var.project_name}-${each.value}-logs" }
}

# ============================================================
# Task Definition (컨테이너 정의)
# ============================================================
# docker-compose의 각 service 블록에 대응합니다.
#
# 설정하는 것:
#   - 사용할 Docker 이미지 (ECR URL)
#   - CPU/메모리 할당량
#   - 환경변수 (DB 접속 정보 등)
#   - 포트 매핑
#   - 로그 설정
# ============================================================

# ── API Gateway ──
resource "aws_ecs_task_definition" "api_gateway" {
  family                   = "${var.project_name}-api-gateway"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"     # Fargate 필수 (각 Task에 ENI 할당)
  cpu                      = 256          # 0.25 vCPU
  memory                   = 512          # 512MB
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name      = "api-gateway"
    image     = "${var.ecr_repository_urls["api-gateway"]}:latest"
    essential = true  # 이 컨테이너가 죽으면 Task 전체 중지

    portMappings = [{
      containerPort = 8000
      protocol      = "tcp"
    }]

    # docker-compose의 environment 와 동일
    environment = [
      { name = "SPRING_CLOUD_GATEWAY_ROUTES_0_ID", value = "core-banking" },
      { name = "SPRING_CLOUD_GATEWAY_ROUTES_0_URI", value = "http://localhost:8080" },
      { name = "SPRING_CLOUD_GATEWAY_ROUTES_0_PREDICATES_0", value = "Path=/api/v1/banking/**" },
      { name = "SPRING_CLOUD_GATEWAY_ROUTES_0_FILTERS_0", value = "StripPrefix=2" },
      { name = "SPRING_CLOUD_GATEWAY_ROUTES_1_ID", value = "auth-service" },
      { name = "SPRING_CLOUD_GATEWAY_ROUTES_1_URI", value = "http://localhost:8081" },
      { name = "SPRING_CLOUD_GATEWAY_ROUTES_1_PREDICATES_0", value = "Path=/api/v1/auth/**" },
      { name = "SPRING_CLOUD_GATEWAY_ROUTES_1_FILTERS_0", value = "StripPrefix=2" },
      { name = "JWT_SECRET", value = var.jwt_secret },
    ]

    # docker logs 대신 CloudWatch로 로그 전송
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/${var.project_name}/api-gateway"
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])
}

# ── Auth Service ──
resource "aws_ecs_task_definition" "auth_service" {
  family                   = "${var.project_name}-auth-service"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512    # 0.5 vCPU
  memory                   = 1024   # 1GB (Spring Boot는 최소 512MB 권장)
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name      = "auth-service"
    image     = "${var.ecr_repository_urls["auth-service"]}:latest"
    essential = true

    portMappings = [{
      containerPort = 8081
      protocol      = "tcp"
    }]

    environment = [
      { name = "SERVER_PORT", value = "8081" },
      # RDS 엔드포인트를 datasource URL로 사용
      { name = "SPRING_DATASOURCE_URL", value = "jdbc:mysql://${var.db_endpoint}/auth_db?createDatabaseIfNotExist=true&serverTimezone=UTC" },
      { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
      { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
      { name = "JWT_SECRET", value = var.jwt_secret },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/${var.project_name}/auth-service"
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])
}

# ── Core Banking ──
resource "aws_ecs_task_definition" "core_banking" {
  family                   = "${var.project_name}-core-banking"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name      = "core-banking"
    image     = "${var.ecr_repository_urls["core-banking"]}:latest"
    essential = true

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_DATASOURCE_URL", value = "jdbc:mysql://${var.db_endpoint}/core_banking?createDatabaseIfNotExist=true&serverTimezone=UTC" },
      { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
      { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
      { name = "JWT_SECRET", value = var.jwt_secret },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/${var.project_name}/core-banking"
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])
}

# ============================================================
# ECS Service (서비스 = 항상 실행 상태 유지)
# ============================================================
# Task Definition을 "이 설정으로 항상 N개 실행해"라고 지시하는 것.
#
# 대응:
#   docker-compose up -d  →  aws_ecs_service (desired_count만큼 유지)
#   컨테이너 죽으면?      →  ECS가 자동으로 새 컨테이너 기동
# ============================================================

resource "aws_ecs_service" "api_gateway" {
  name            = "api-gateway-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.api_gateway.arn
  desired_count   = 1              # 실행할 컨테이너 수 (prod: 2 이상 권장)
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = var.private_subnet_ids
    security_groups = [var.ecs_security_group_id]
  }

  # ALB와 연결 → 외부 트래픽을 이 서비스로 전달
  load_balancer {
    target_group_arn = var.alb_target_group_arn
    container_name   = "api-gateway"
    container_port   = 8000
  }
}

resource "aws_ecs_service" "auth_service" {
  name            = "auth-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.auth_service.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = var.private_subnet_ids
    security_groups = [var.ecs_security_group_id]
  }
  # 내부 서비스이므로 ALB 연결 불필요 (api-gateway가 직접 호출)
}

resource "aws_ecs_service" "core_banking" {
  name            = "core-banking-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.core_banking.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = var.private_subnet_ids
    security_groups = [var.ecs_security_group_id]
  }
}
