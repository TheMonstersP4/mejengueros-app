data "aws_caller_identity" "current" {}

resource "aws_iam_openid_connect_provider" "github_actions" {
  count = var.existing_oidc_provider_arn == "" ? 1 : 0

  url = "https://token.actions.githubusercontent.com"

  client_id_list = [
    "sts.amazonaws.com"
  ]

  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1"
  ]
}

locals {
  oidc_provider_arn = var.existing_oidc_provider_arn != "" ? var.existing_oidc_provider_arn : aws_iam_openid_connect_provider.github_actions[0].arn
  repositories      = distinct(compact(concat(var.repository != "" ? [var.repository] : [], var.repositories)))
  branch_subs       = [for repository in local.repositories : "repo:${repository}:ref:refs/heads/${var.branch}"]
  environment_subs  = var.environment != "" ? [for repository in local.repositories : "repo:${repository}:environment:${var.environment}"] : []
  repository_subs   = concat(local.branch_subs, local.environment_subs)
}

resource "aws_iam_role" "github_actions" {
  name = "${var.name_prefix}-github-actions"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = local.oidc_provider_arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          }
          StringLike = {
            "token.actions.githubusercontent.com:sub" = local.repository_subs
          }
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "direct_deploy" {
  name = "${var.name_prefix}-direct-deploy"
  role = aws_iam_role.github_actions.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      var.poc_site_bucket_name != "" ? [
        {
          Effect = "Allow"
          Action = [
            "s3:DeleteObject",
            "s3:ListBucket",
            "s3:PutObject"
          ]
          Resource = [
            "arn:aws:s3:::${var.poc_site_bucket_name}",
            "arn:aws:s3:::${var.poc_site_bucket_name}/*"
          ]
        }
      ] : [],
      var.ecr_repository_arn != "" ? [
        {
          Effect   = "Allow"
          Action   = ["ecr:GetAuthorizationToken"]
          Resource = ["*"]
        },
        {
          Effect = "Allow"
          Action = [
            "ecr:BatchCheckLayerAvailability",
            "ecr:CompleteLayerUpload",
            "ecr:InitiateLayerUpload",
            "ecr:PutImage",
            "ecr:UploadLayerPart"
          ]
          Resource = [var.ecr_repository_arn]
        }
      ] : [],
      length(var.lambda_function_arns) > 0 ? [
        {
          Effect = "Allow"
          Action = [
            "lambda:GetFunction",
            "lambda:GetFunctionConfiguration",
            "lambda:UpdateFunctionCode"
          ]
          Resource = var.lambda_function_arns
        }
      ] : [],
      length(var.secrets_manager_secret_arns) > 0 ? [
        {
          Effect = "Allow"
          Action = [
            "secretsmanager:DescribeSecret",
            "secretsmanager:PutSecretValue"
          ]
          Resource = var.secrets_manager_secret_arns
        }
      ] : []
    )
  })
}
