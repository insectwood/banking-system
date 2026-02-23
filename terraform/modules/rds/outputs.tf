# ECS에서 접속할 DB 엔드포인트
# 예: banking-system-dev-mysql.xxxx.ap-northeast-1.rds.amazonaws.com:3306
output "db_endpoint" { value = aws_db_instance.main.endpoint }
output "db_name" { value = aws_db_instance.main.db_name }
