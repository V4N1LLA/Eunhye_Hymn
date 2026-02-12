output "ec2_public_ip" {
  description = "EC2 Elastic IP — access the app at http://<this-ip>"
  value       = aws_eip.app.public_ip
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint (host:port)"
  value       = aws_db_instance.main.endpoint
}

output "rds_address" {
  description = "RDS hostname (without port)"
  value       = aws_db_instance.main.address
}

output "s3_bucket_name" {
  description = "S3 bucket name for assets"
  value       = aws_s3_bucket.assets.id
}

output "s3_bucket_url" {
  description = "S3 bucket public URL base"
  value       = "https://${aws_s3_bucket.assets.bucket_regional_domain_name}"
}

output "ecr_api_url" {
  description = "ECR repository URL for API image"
  value       = aws_ecr_repository.api.repository_url
}

output "ecr_admin_url" {
  description = "ECR repository URL for Admin image"
  value       = aws_ecr_repository.admin.repository_url
}

output "ecr_registry" {
  description = "ECR registry URL (account.dkr.ecr.region.amazonaws.com)"
  value       = split("/", aws_ecr_repository.api.repository_url)[0]
}

output "sns_topic_arn" {
  description = "SNS topic ARN for CloudWatch alarms"
  value       = aws_sns_topic.alerts.arn
}

output "cloudwatch_dashboard_url" {
  description = "CloudWatch dashboard URL"
  value       = "https://${var.aws_region}.console.aws.amazon.com/cloudwatch/home?region=${var.aws_region}#dashboards:name=${var.project_name}-${var.environment}"
}
