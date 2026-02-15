variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "eunhye-hymn"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "staging"
}

# ── EC2 ──────────────────────────────────────────────────────

variable "ec2_key_pair_name" {
  description = "Name of the EC2 key pair for SSH access"
  type        = string
}

variable "ec2_ami" {
  description = "AMI ID for EC2 instance (Amazon Linux 2023)"
  type        = string
  default     = "" # resolved by data source if empty
}

# ── RDS ──────────────────────────────────────────────────────

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "eunhye_hymn"
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
  default     = "postgres"
}

variable "db_password" {
  description = "PostgreSQL master password"
  type        = string
  sensitive   = true
}

# ── S3 ───────────────────────────────────────────────────────

variable "s3_bucket_name" {
  description = "S3 bucket name for hymn assets (must be globally unique)"
  type        = string
}

# ── App Config (passed to containers via .env) ───────────────

variable "jwt_secret" {
  description = "JWT signing secret"
  type        = string
  sensitive   = true
}

variable "jwt_access_ttl" {
  description = "JWT access token TTL in seconds"
  type        = number
  default     = 3600
}

variable "jwt_refresh_ttl" {
  description = "JWT refresh token TTL in seconds"
  type        = number
  default     = 604800
}

variable "invite_code" {
  description = "Invite code for new user registration"
  type        = string
  sensitive   = true
}

variable "allowed_ssh_cidrs" {
  description = "CIDR blocks allowed to SSH into EC2"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# ── Monitoring ───────────────────────────────────────────────

variable "alert_email" {
  description = "Email address to receive CloudWatch alarm notifications (empty = no email subscription)"
  type        = string
  default     = ""
}
