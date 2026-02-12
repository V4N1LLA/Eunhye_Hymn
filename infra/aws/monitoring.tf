# ── SNS Topic (알림 수신) ────────────────────────────────────

resource "aws_sns_topic" "alerts" {
  name = "${var.project_name}-alerts"

  tags = { Name = "${var.project_name}-alerts" }
}

resource "aws_sns_topic_subscription" "email" {
  count     = var.alert_email != "" ? 1 : 0
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# ── CloudWatch Log Groups (Docker 컨테이너 로그) ────────────

resource "aws_cloudwatch_log_group" "api" {
  name              = "/${var.project_name}/api"
  retention_in_days = 14

  tags = { Name = "${var.project_name}-api-logs" }
}

resource "aws_cloudwatch_log_group" "nginx" {
  name              = "/${var.project_name}/nginx"
  retention_in_days = 14

  tags = { Name = "${var.project_name}-nginx-logs" }
}

# ── EC2 Alarms ──────────────────────────────────────────────

resource "aws_cloudwatch_metric_alarm" "ec2_cpu_high" {
  alarm_name          = "${var.project_name}-ec2-cpu-high"
  alarm_description   = "EC2 CPU utilization > 80% for 5 minutes"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  treat_missing_data  = "notBreaching"

  dimensions = {
    InstanceId = aws_instance.app.id
  }

  alarm_actions = [aws_sns_topic.alerts.arn]
  ok_actions    = [aws_sns_topic.alerts.arn]

  tags = { Name = "${var.project_name}-ec2-cpu-high" }
}

resource "aws_cloudwatch_metric_alarm" "ec2_status_check" {
  alarm_name          = "${var.project_name}-ec2-status-check"
  alarm_description   = "EC2 instance or system status check failed"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "StatusCheckFailed"
  namespace           = "AWS/EC2"
  period              = 300
  statistic           = "Maximum"
  threshold           = 0
  treat_missing_data  = "breaching"

  dimensions = {
    InstanceId = aws_instance.app.id
  }

  alarm_actions = [aws_sns_topic.alerts.arn]

  tags = { Name = "${var.project_name}-ec2-status-check" }
}

# ── RDS Alarms ──────────────────────────────────────────────

resource "aws_cloudwatch_metric_alarm" "rds_cpu_high" {
  alarm_name          = "${var.project_name}-rds-cpu-high"
  alarm_description   = "RDS CPU utilization > 80% for 5 minutes"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  treat_missing_data  = "notBreaching"

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main.identifier
  }

  alarm_actions = [aws_sns_topic.alerts.arn]
  ok_actions    = [aws_sns_topic.alerts.arn]

  tags = { Name = "${var.project_name}-rds-cpu-high" }
}

resource "aws_cloudwatch_metric_alarm" "rds_storage_low" {
  alarm_name          = "${var.project_name}-rds-storage-low"
  alarm_description   = "RDS free storage < 5GB (25% of 20GB)"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 1
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 5368709120 # 5GB in bytes (25% of 20GB Free Tier)
  treat_missing_data  = "notBreaching"

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main.identifier
  }

  alarm_actions = [aws_sns_topic.alerts.arn]

  tags = { Name = "${var.project_name}-rds-storage-low" }
}

resource "aws_cloudwatch_metric_alarm" "rds_connections_high" {
  alarm_name          = "${var.project_name}-rds-connections-high"
  alarm_description   = "RDS database connections > 30 (t3.micro max ~58)"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "DatabaseConnections"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 30
  treat_missing_data  = "notBreaching"

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main.identifier
  }

  alarm_actions = [aws_sns_topic.alerts.arn]

  tags = { Name = "${var.project_name}-rds-connections-high" }
}

# ── CloudWatch Dashboard ────────────────────────────────────

resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "${var.project_name}-${var.environment}"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          title   = "EC2 CPU Utilization"
          metrics = [["AWS/EC2", "CPUUtilization", "InstanceId", aws_instance.app.id]]
          period  = 300
          stat    = "Average"
          region  = var.aws_region
          view    = "timeSeries"
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          title = "EC2 Network"
          metrics = [
            ["AWS/EC2", "NetworkIn", "InstanceId", aws_instance.app.id],
            ["AWS/EC2", "NetworkOut", "InstanceId", aws_instance.app.id]
          ]
          period = 300
          stat   = "Sum"
          region = var.aws_region
          view   = "timeSeries"
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 8
        height = 6
        properties = {
          title   = "RDS CPU Utilization"
          metrics = [["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", aws_db_instance.main.identifier]]
          period  = 300
          stat    = "Average"
          region  = var.aws_region
          view    = "timeSeries"
        }
      },
      {
        type   = "metric"
        x      = 8
        y      = 6
        width  = 8
        height = 6
        properties = {
          title   = "RDS Connections"
          metrics = [["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", aws_db_instance.main.identifier]]
          period  = 300
          stat    = "Average"
          region  = var.aws_region
          view    = "timeSeries"
        }
      },
      {
        type   = "metric"
        x      = 16
        y      = 6
        width  = 8
        height = 6
        properties = {
          title   = "RDS Free Storage (GB)"
          metrics = [["AWS/RDS", "FreeStorageSpace", "DBInstanceIdentifier", aws_db_instance.main.identifier]]
          period  = 300
          stat    = "Average"
          region  = var.aws_region
          view    = "timeSeries"
          yAxis   = { left = { label = "Bytes" } }
        }
      },
      {
        type   = "log"
        x      = 0
        y      = 12
        width  = 12
        height = 6
        properties = {
          title   = "API Logs (Recent Errors)"
          query   = "SOURCE '/${var.project_name}/api' | fields @timestamp, @message | filter @message like /(?i)(error|exception|fail)/ | sort @timestamp desc | limit 20"
          region  = var.aws_region
          view    = "table"
        }
      },
      {
        type   = "log"
        x      = 12
        y      = 12
        width  = 12
        height = 6
        properties = {
          title   = "Nginx Logs (5xx Errors)"
          query   = "SOURCE '/${var.project_name}/nginx' | fields @timestamp, @message | filter @message like /\" [5]\\d{2} / | sort @timestamp desc | limit 20"
          region  = var.aws_region
          view    = "table"
        }
      }
    ]
  })
}
