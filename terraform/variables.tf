# ============================================================
# 변수 정의
# ============================================================
# terraform apply 시 -var 또는 .tfvars 파일로 값을 전달합니다.
# sensitive = true 인 변수는 terraform plan 출력에서 마스킹됩니다.
# ============================================================

variable "aws_region" {
  description = "AWS 리전 (도쿄)"
  type        = string
  default     = "ap-northeast-1"
}

variable "project_name" {
  description = "프로젝트명 - 모든 리소스 이름에 prefix로 사용"
  type        = string
  default     = "banking-system"
}

variable "environment" {
  description = "환경 구분 (dev / staging / prod)"
  type        = string
  default     = "dev"
}

variable "vpc_cidr" {
  description = "VPC IP 대역 (10.0.0.0/16 = 65,536개 IP)"
  type        = string
  default     = "10.0.0.0/16"
}

# ── 민감 정보 (Git에 커밋 금지) ──
variable "db_username" {
  description = "RDS 마스터 사용자명"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "RDS 마스터 비밀번호"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT 서명 키 (auth-service, api-gateway, core-banking 공통)"
  type        = string
  sensitive   = true
}
