# ============================================================
# Banking System - Terraform 메인 설정
# ============================================================
# 이 파일이 모든 모듈을 조합하는 엔트리포인트입니다.
# terraform apply 실행 시 여기서부터 각 모듈이 호출됩니다.
#
# 인프라 구성도:
#   Internet → ALB (Public Subnet)
#                → ECS Fargate (Private Subnet)
#                    ├── api-gateway
#                    ├── auth-service
#                    └── core-banking
#                        → RDS MySQL (Private Subnet)
# ============================================================

terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # ── State 관리 (팀 개발 시 활성화) ──
  # 여러 명이 같은 인프라를 관리할 때, state 파일을 S3에 저장하여 공유합니다.
  # 개인 프로젝트에서는 로컬 state로 충분합니다.
  #
  # backend "s3" {
  #   bucket = "banking-system-terraform-state"
  #   key    = "env/dev/terraform.tfstate"
  #   region = "ap-northeast-1"
  # }
}

# AWS 프로바이더 설정
# - region: 도쿄 리전 (후쿠오카 FG 대응)
# - default_tags: 모든 리소스에 자동으로 태그 부여 (비용 추적, 관리 용이)
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "banking-system"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# ============================================================
# 모듈 호출 (아래에서 위로 의존 관계)
#
# networking (VPC) ← 모든 모듈의 기반
#     ↑
# rds (DB)        ← ECS보다 먼저 생성 (DB 엔드포인트 필요)
# ecr (이미지)    ← ECS보다 먼저 생성 (이미지 URL 필요)
# alb (로드밸런서) ← ECS보다 먼저 생성 (타겟그룹 ARN 필요)
#     ↑
# ecs (컨테이너)  ← 위 모든 모듈의 출력값을 받아서 생성
# ============================================================

# ── 1. Networking: VPC, Subnet, Security Group ──
# 모든 AWS 리소스가 올라갈 네트워크 기반
module "networking" {
  source = "./modules/networking"

  project_name = var.project_name
  environment  = var.environment
  vpc_cidr     = var.vpc_cidr
}

# ── 2. ECR: Docker 이미지 저장소 ──
# docker push로 이미지를 올리면, ECS가 여기서 pull 합니다.
module "ecr" {
  source = "./modules/ecr"

  project_name = var.project_name
  services     = ["api-gateway", "auth-service", "core-banking"]
}

# ── 3. RDS: MySQL 데이터베이스 ──
# Private Subnet에 배치 → 외부 접근 불가, ECS에서만 접근 가능
module "rds" {
  source = "./modules/rds"

  project_name          = var.project_name
  environment           = var.environment
  private_subnet_ids    = module.networking.private_subnet_ids
  rds_security_group_id = module.networking.rds_security_group_id
  db_username           = var.db_username
  db_password           = var.db_password
}

# ── 4. ALB: 로드밸런서 ──
# Public Subnet에 배치 → 인터넷 트래픽을 받아서 ECS로 전달
module "alb" {
  source = "./modules/alb"

  project_name          = var.project_name
  environment           = var.environment
  vpc_id                = module.networking.vpc_id
  public_subnet_ids     = module.networking.public_subnet_ids
  alb_security_group_id = module.networking.alb_security_group_id
}

# ── 5. ECS Fargate: 컨테이너 실행 ──
# 위 모듈의 출력값을 모두 받아서 서비스를 기동합니다.
module "ecs" {
  source = "./modules/ecs"

  project_name          = var.project_name
  environment           = var.environment
  aws_region            = var.aws_region
  private_subnet_ids    = module.networking.private_subnet_ids
  ecs_security_group_id = module.networking.ecs_security_group_id
  alb_target_group_arn  = module.alb.target_group_arn
  ecr_repository_urls   = module.ecr.repository_urls
  db_endpoint           = module.rds.db_endpoint
  db_username           = var.db_username
  db_password           = var.db_password
  jwt_secret            = var.jwt_secret
}
