# ============================================================
# Networking 모듈 - VPC / Subnet / Security Group
# ============================================================
# 모든 AWS 리소스가 배치되는 네트워크 기반입니다.
#
# 구조:
#   VPC (10.0.0.0/16)
#     ├── Public Subnet x2   (10.0.0.0/24, 10.0.1.0/24)  ← ALB가 여기에
#     └── Private Subnet x2  (10.0.10.0/24, 10.0.11.0/24) ← ECS, RDS가 여기에
#
# 왜 2개씩?
#   → AWS의 ALB, RDS는 최소 2개 AZ(가용영역)가 필수입니다.
#   → 하나의 AZ가 장애나도 다른 AZ에서 서비스 계속 가능 (고가용성)
#
# Public vs Private 차이:
#   Public  = 인터넷에서 직접 접근 가능 (ALB)
#   Private = 인터넷에서 접근 불가 (ECS, RDS) ← 보안상 필수
# ============================================================

# ── VPC (Virtual Private Cloud) ──
# AWS 내에서 격리된 네트워크 공간. 모든 리소스는 VPC 안에 생성됩니다.
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr           # IP 대역: 10.0.0.0/16 (65,536개)
  enable_dns_hostnames = true                    # RDS 등에서 DNS 호스트명 사용 가능
  enable_dns_support   = true

  tags = { Name = "${var.project_name}-${var.environment}-vpc" }
}

# ── 가용영역(AZ) 자동 탐색 ──
# ap-northeast-1a, ap-northeast-1c 같은 AZ를 자동으로 가져옵니다.
data "aws_availability_zones" "available" {
  state = "available"
}

# ── Public Subnet (2개) ──
# ALB가 배치되는 서브넷. 인터넷에서 접근 가능.
resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)  # 10.0.0.0/24, 10.0.1.0/24
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true  # 이 서브넷의 리소스에 공인 IP 자동 부여

  tags = { Name = "${var.project_name}-public-${count.index + 1}" }
}

# ── Private Subnet (2개) ──
# ECS, RDS가 배치되는 서브넷. 인터넷에서 직접 접근 불가.
resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 10)  # 10.0.10.0/24, 10.0.11.0/24
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = { Name = "${var.project_name}-private-${count.index + 1}" }
}

# ── Internet Gateway ──
# VPC가 인터넷과 통신하기 위한 출입구.
# Public Subnet의 트래픽이 이것을 통해 인터넷으로 나갑니다.
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = "${var.project_name}-igw" }
}

# ── NAT Gateway ──
# Private Subnet → 인터넷 (아웃바운드만 허용)
# 용도: ECS가 ECR에서 Docker 이미지를 pull할 때 필요
# 주의: 이것이 비용의 주요 원인 (~$32/월). 개발 시에만 사용하고 삭제 권장.
resource "aws_eip" "nat" {
  domain = "vpc"
  tags   = { Name = "${var.project_name}-nat-eip" }
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id  # Public Subnet에 배치
  tags          = { Name = "${var.project_name}-nat-gw" }

  depends_on = [aws_internet_gateway.main]
}

# ── 라우팅 테이블 ──
# "이 서브넷에서 나가는 트래픽은 어디로 보낼까?"를 정의합니다.

# Public: 모든 트래픽 → Internet Gateway (인터넷으로)
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = "${var.project_name}-public-rt" }

  route {
    cidr_block = "0.0.0.0/0"           # 모든 외부 IP
    gateway_id = aws_internet_gateway.main.id  # → 인터넷으로
  }
}

# Private: 모든 트래픽 → NAT Gateway (인터넷 아웃바운드만)
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = "${var.project_name}-private-rt" }

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id  # → NAT 경유로 인터넷
  }
}

# 서브넷에 라우팅 테이블 연결
resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count          = 2
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

# ============================================================
# Security Groups (방화벽 규칙)
# ============================================================
# 누가 어디에 접근할 수 있는지를 제어합니다.
#
# 트래픽 흐름:
#   인터넷(80/443) → ALB SG → ECS SG(전포트) → RDS SG(3306만)
#
# 핵심: RDS는 ECS에서만 접근 가능. 인터넷에서 DB 직접 접근은 불가.
# ============================================================

# ALB Security Group: 인터넷에서 HTTP/HTTPS 허용
resource "aws_security_group" "alb" {
  name   = "${var.project_name}-alb-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    description = "HTTP from Internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS from Internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-alb-sg" }
}

# ECS Security Group: ALB에서 오는 트래픽만 허용
resource "aws_security_group" "ecs" {
  name   = "${var.project_name}-ecs-sg"
  vpc_id = aws_vpc.main.id

  # ALB → ECS 트래픽 허용
  ingress {
    description     = "Traffic from ALB"
    from_port       = 0
    to_port         = 65535
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]  # ALB의 SG를 참조
  }

  # ECS 컨테이너 간 통신 허용 (마이크로서비스 간 호출)
  ingress {
    description = "Inter-service communication"
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    self        = true  # 같은 SG끼리 통신 허용
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-ecs-sg" }
}

# RDS Security Group: ECS에서 MySQL(3306) 접근만 허용
resource "aws_security_group" "rds" {
  name   = "${var.project_name}-rds-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    description     = "MySQL from ECS only"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]  # ECS의 SG만 허용
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-rds-sg" }
}
