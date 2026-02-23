# ECS Task Definition에서 참조할 이미지 URL
# 예: 123456789.dkr.ecr.ap-northeast-1.amazonaws.com/banking-system-core-banking
output "repository_urls" {
  value = { for k, v in aws_ecr_repository.services : k => v.repository_url }
}
