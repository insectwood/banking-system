# ============================================================
# 개발 환경 설정
# ============================================================
# 사용법: terraform apply -var-file=environments/dev/dev.tfvars
#
# 민감 정보(db_password 등)는 이 파일에 넣지 않고,
# 실행 시 -var로 전달하거나 환경변수(TF_VAR_xxx)로 설정합니다.
#
# 예시:
#   export TF_VAR_db_username=admin
#   export TF_VAR_db_password=MySecurePass123!
#   export TF_VAR_jwt_secret=my-jwt-secret-key
#   terraform apply -var-file=environments/dev/dev.tfvars
# ============================================================

aws_region   = "ap-northeast-1"
project_name = "banking-system"
environment  = "dev"
vpc_cidr     = "10.0.0.0/16"
