# ============================================================
# RDS (Relational Database Service) - MySQL
# ============================================================
# docker-compose의 MySQL 컨테이너를 AWS 관리형 DB로 교체합니다.
#
# 로컬 vs AWS:
#   로컬: MySQL 컨테이너 (데이터 유실 가능, 백업 수동)
#   AWS:  RDS MySQL (자동 백업, 암호화, 장애 자동 복구)
#
# 보안:
#   - Private Subnet에 배치 → 인터넷에서 접근 불가
#   - ECS Security Group에서만 3306 포트 접근 허용
#   - 스토리지 암호화 활성화 (금융 시스템 필수)
# ============================================================

# ── DB Subnet Group ──
# RDS가 배치될 서브넷 그룹 (최소 2개 AZ 필수)
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-db-subnet"
  subnet_ids = var.private_subnet_ids  # Private Subnet 2개

  tags = { Name = "${var.project_name}-db-subnet-group" }
}

# ── RDS MySQL 인스턴스 ──
resource "aws_db_instance" "main" {
  identifier     = "${var.project_name}-${var.environment}-mysql"
  engine         = "mysql"
  engine_version = "8.0"
  instance_class = "db.t3.micro"  # 개발용 최소 사양 (프리 티어 대상)

  # ── 스토리지 ──
  allocated_storage     = 20    # 초기 20GB
  max_allocated_storage = 50    # 자동 확장 상한 50GB
  storage_type          = "gp3" # 범용 SSD (비용 효율)
  storage_encrypted     = true  # 암호화 필수 (금융 시스템)

  # ── 접속 정보 ──
  db_name  = "banking"         # 초기 생성 DB명
  username = var.db_username   # terraform apply 시 입력
  password = var.db_password   # terraform apply 시 입력

  # ── 네트워크 ──
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.rds_security_group_id]  # networking 모듈의 RDS SG
  publicly_accessible    = false  # 인터넷 접근 차단

  # ── 가용성 ──
  multi_az = false  # dev: 단일 AZ (비용 절감), prod에서는 true로 변경

  # ── 백업 ──
  backup_retention_period = 7                       # 7일간 백업 보관
  backup_window           = "03:00-04:00"           # UTC 03:00 = KST 12:00
  maintenance_window      = "sun:04:00-sun:05:00"   # 일요일 정기 유지보수

  skip_final_snapshot = true  # dev 환경: 삭제 시 스냅샷 생략

  tags = { Name = "${var.project_name}-${var.environment}-mysql" }
}
