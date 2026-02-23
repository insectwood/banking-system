# ============================================================
# ECR (Elastic Container Registry) - Docker 이미지 저장소
# ============================================================
# Docker Hub 대신 AWS 내부에 이미지를 저장합니다.
#
# 흐름:
#   개발자 PC → docker build → docker push → ECR
#   ECS Fargate → ECR에서 docker pull → 컨테이너 실행
#
# for_each로 3개 서비스의 저장소를 한번에 생성합니다.
# ============================================================

resource "aws_ecr_repository" "services" {
  for_each = toset(var.services)  # ["api-gateway", "auth-service", "core-banking"]

  name                 = "${var.project_name}-${each.value}"
  image_tag_mutability = "MUTABLE"  # latest 태그 덮어쓰기 허용
  force_delete         = true       # 이미지가 있어도 terraform destroy 가능

  # 이미지 push 시 자동 보안 스캔 (취약점 탐지)
  image_scanning_configuration {
    scan_on_push = true
  }

  tags = { Name = "${var.project_name}-${each.value}" }
}

# ── 오래된 이미지 자동 삭제 (비용 절감) ──
# 최근 5개만 유지하고 나머지는 자동 삭제합니다.
resource "aws_ecr_lifecycle_policy" "cleanup" {
  for_each   = toset(var.services)
  repository = aws_ecr_repository.services[each.key].name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "최근 5개 이미지만 유지"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 5
      }
      action = { type = "expire" }
    }]
  })
}
