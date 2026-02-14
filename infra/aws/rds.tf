# ── DB Subnet Group ──────────────────────────────────────────

resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet"
  subnet_ids = [aws_subnet.public_a.id, aws_subnet.public_b.id]

  tags = { Name = "${var.project_name}-db-subnet" }
}

# ── RDS PostgreSQL (Free Tier) ──────────────────────────────

resource "aws_db_instance" "main" {
  identifier = "${var.project_name}-db"

  engine         = "postgres"
  engine_version = "15.8"
  instance_class = "db.t3.micro"

  allocated_storage     = 20
  max_allocated_storage = 20
  storage_type          = "gp2"

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  # Free Tier settings
  multi_az            = false
  publicly_accessible = false
  storage_encrypted   = false

  # Maintenance
  backup_retention_period = 1
  skip_final_snapshot     = true

  tags = { Name = "${var.project_name}-db" }
}
