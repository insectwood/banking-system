# ============================================================
# ALB (Application Load Balancer)
# ============================================================
# 인터넷 트래픽을 받아서 ECS 컨테이너로 분배합니다.
#
# docker-compose에서의 대응:
#   로컬: localhost:8000 → api-gateway 컨테이너
#   AWS:  ALB DNS → ECS api-gateway Task
#
# 핵심 기능:
#   1. 로드밸런싱 - 여러 컨테이너에 트래픽 분산
#   2. 헬스체크 - 비정상 컨테이너에는 트래픽 안 보냄
#   3. HTTPS 종료 - SSL 인증서 처리 (본 설정에서는 HTTP만)
# ============================================================

# ── ALB ──
resource "aws_lb" "main" {
  name               = "${var.project_name}-${var.environment}-alb"
  internal           = false             # 인터넷 대면 (true면 내부 전용)
  load_balancer_type = "application"     # HTTP/HTTPS용 (L7)
  security_groups    = [var.alb_security_group_id]
  subnets            = var.public_subnet_ids  # Public Subnet에 배치

  tags = { Name = "${var.project_name}-alb" }
}

# ── Target Group ──
# "ALB가 트래픽을 보낼 대상"을 정의합니다.
# ECS 서비스가 여기에 자동 등록됩니다.
resource "aws_lb_target_group" "api" {
  name        = "${var.project_name}-api-tg"
  port        = 8000            # api-gateway 포트
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"            # Fargate는 반드시 "ip" (EC2는 "instance")

  # ── 헬스체크 ──
  # ALB가 주기적으로 이 경로를 호출해서 컨테이너 상태를 확인합니다.
  # 200 응답이 오면 정상, 연속 실패하면 비정상으로 판단하고 트래픽 차단.
  health_check {
    enabled             = true
    path                = "/actuator/health"  # Spring Actuator 헬스체크 엔드포인트
    port                = "traffic-port"
    healthy_threshold   = 3     # 3회 연속 성공 → 정상
    unhealthy_threshold = 3     # 3회 연속 실패 → 비정상
    timeout             = 5     # 5초 내 응답 없으면 실패
    interval            = 30    # 30초마다 체크
    matcher             = "200"
  }

  tags = { Name = "${var.project_name}-api-tg" }
}

# ── HTTP 리스너 ──
# 포트 80으로 들어오는 요청을 Target Group으로 전달합니다.
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}

# ── HTTPS 리스너 (SSL 인증서 필요 시 활성화) ──
# ACM에서 인증서를 발급받은 후 아래 주석을 해제합니다.
# resource "aws_lb_listener" "https" {
#   load_balancer_arn = aws_lb.main.arn
#   port              = 443
#   protocol          = "HTTPS"
#   ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
#   certificate_arn   = var.certificate_arn
#
#   default_action {
#     type             = "forward"
#     target_group_arn = aws_lb_target_group.api.arn
#   }
# }
