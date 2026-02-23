# ============================================================
# 출력값
# ============================================================
# terraform apply 완료 후 표시되는 값들입니다.
# 이 값들로 서비스에 접근하거나 CI/CD에서 참조합니다.
# ============================================================

output "alb_dns_name" {
  description = "ALB 도메인 (API 접근용) - http://이 주소/api/v1/banking/..."
  value       = module.alb.alb_dns_name
}

output "rds_endpoint" {
  description = "RDS 엔드포인트 - application.properties의 datasource.url에 사용"
  value       = module.rds.db_endpoint
}

output "ecr_repository_urls" {
  description = "ECR 저장소 URL - docker push 대상"
  value       = module.ecr.repository_urls
}
