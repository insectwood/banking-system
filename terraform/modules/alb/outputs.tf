# API 접근용 도메인: http://banking-system-dev-alb-xxxx.ap-northeast-1.elb.amazonaws.com
output "alb_dns_name" { value = aws_lb.main.dns_name }

# ECS 서비스에서 참조할 Target Group ARN
output "target_group_arn" { value = aws_lb_target_group.api.arn }
