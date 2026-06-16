locals {
  name_prefix = "${var.project}-${var.env}"

  cognito_domain_prefix                  = "${local.name_prefix}-auth"
  cognito_identity_provider_redirect_url = "https://${local.cognito_domain_prefix}.auth.${var.aws_region}.amazoncognito.com/oauth2/idpresponse"
  api_lambda_image_uri                   = "${module.ecr_api.repository_url}:${var.api_lambda_image_tag}"
  api_http_deploy_enabled                = var.api_http_enabled
  api_lambda_vpc_enabled                 = local.api_http_deploy_enabled && var.api_lambda_vpc_enabled
  api_gateway_access_logs_enabled        = (local.api_http_deploy_enabled && var.http_api_access_log_enabled) || var.websocket_access_log_enabled
  api_database_secret_arn                = var.api_database_secret_arn != "" ? var.api_database_secret_arn : (var.api_database_secret_enabled ? aws_secretsmanager_secret.database_url[0].arn : "")
  websocket_lambda_deploy_enabled        = var.websocket_lambda_enabled && var.websocket_lambda_package_filename != ""
  websocket_lambda_source_code_hash      = local.websocket_lambda_deploy_enabled ? (var.websocket_lambda_package_source_code_hash != "" ? var.websocket_lambda_package_source_code_hash : filebase64sha256(var.websocket_lambda_package_filename)) : null
  websocket_connections_table_name       = var.websocket_connections_table_name != "" ? var.websocket_connections_table_name : "${local.name_prefix}-ws-connections"
  poc_site_domain                        = var.poc_site_subdomain != "" ? "${var.poc_site_subdomain}.${var.poc_site_domain_name}" : var.poc_site_domain_name
  github_deploy_repositories             = distinct(compact(concat(var.github_repository != "" ? [var.github_repository] : [], var.github_repositories)))

  database_url = var.postgres_enabled ? "postgresql://${var.postgres_master_username}:${urlencode(module.postgres[0].master_password)}@${module.postgres[0].endpoint}:${module.postgres[0].port}/${module.postgres[0].db_name}?schema=public" : ""

  default_tags = {
    Project     = var.project
    Environment = var.env
    Owner       = var.owner
    ManagedBy   = "Terraform"
  }
}

resource "aws_secretsmanager_secret" "database_url" {
  count = var.api_database_secret_enabled && var.api_database_secret_arn == "" ? 1 : 0

  name                    = "${local.name_prefix}/database-url"
  recovery_window_in_days = 7

  tags = local.default_tags
}

resource "terraform_data" "websocket_lambda_configuration_guard" {
  input = {
    websocket_lambda_enabled          = var.websocket_lambda_enabled
    websocket_lambda_package_filename = var.websocket_lambda_package_filename
  }

  lifecycle {
    precondition {
      condition     = !var.websocket_lambda_enabled || var.websocket_lambda_package_filename != ""
      error_message = "websocket_lambda_enabled=true requires websocket_lambda_package_filename. Build the API and package the WebSocket Lambdas first."
    }

    precondition {
      condition     = !var.websocket_lambda_enabled || fileexists(var.websocket_lambda_package_filename)
      error_message = "websocket_lambda_package_filename does not exist. Run npm run build and npm run lambda:package:websocket from api/."
    }
  }
}

module "api_gateway_account_cloudwatch" {
  source = "../modules/aws/api_gateway_account_cloudwatch"
  count  = local.api_gateway_access_logs_enabled ? 1 : 0

  name_prefix = local.name_prefix
}

module "microsoft_oauth" {
  source = "../modules/azuread/oauth_application"
  count  = var.azuread_enabled ? 1 : 0

  display_name               = "${local.name_prefix}-auth"
  sign_in_audience           = var.azuread_sign_in_audience
  redirect_uris              = [local.cognito_identity_provider_redirect_url]
  homepage_url               = var.azuread_homepage_url
  logout_url                 = var.azuread_logout_url
  owner_object_ids           = var.azuread_owner_object_ids
  client_secret_display_name = var.azuread_client_secret_display_name
  client_secret_end_date     = var.azuread_client_secret_end_date
}

resource "aws_iam_role" "api_lambda" {
  count = local.api_http_deploy_enabled ? 1 : 0

  name = "${local.name_prefix}-http-lambda"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "api_lambda_basic_execution" {
  count = local.api_http_deploy_enabled ? 1 : 0

  role       = aws_iam_role.api_lambda[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "api_lambda_vpc_access" {
  count = local.api_lambda_vpc_enabled ? 1 : 0

  role       = aws_iam_role.api_lambda[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

resource "aws_iam_role_policy" "api_runtime" {
  count = local.api_http_deploy_enabled ? 1 : 0

  name = "${local.name_prefix}-http-runtime"
  role = aws_iam_role.api_lambda[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      [
        {
          Effect = "Allow"
          Action = [
            "s3:GetObject",
            "s3:PutObject"
          ]
          Resource = "${module.app_bucket.bucket_arn}/${var.env}/uploads/*"
        },
        {
          Effect = "Allow"
          Action = [
            "dynamodb:DeleteItem",
            "dynamodb:GetItem",
            "dynamodb:PutItem",
            "dynamodb:Query",
            "dynamodb:Scan",
            "dynamodb:UpdateItem"
          ]
          Resource = [
            module.websocket_api.connections_table_arn,
            "${module.websocket_api.connections_table_arn}/index/*"
          ]
        },
        {
          Effect = "Allow"
          Action = [
            "execute-api:ManageConnections"
          ]
          Resource = module.websocket_api.manage_connections_arn
        }
      ],
      local.api_database_secret_arn != "" ? [
        {
          Effect = "Allow"
          Action = [
            "secretsmanager:GetSecretValue"
          ]
          Resource = local.api_database_secret_arn
        }
      ] : []
    )
  })
}

resource "aws_iam_role" "websocket_lambda" {
  count = local.websocket_lambda_deploy_enabled ? 1 : 0

  name = "${local.name_prefix}-ws-lambda"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "websocket_lambda_basic_execution" {
  count = local.websocket_lambda_deploy_enabled ? 1 : 0

  role       = aws_iam_role.websocket_lambda[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "websocket_runtime" {
  count = local.websocket_lambda_deploy_enabled ? 1 : 0

  name = "${local.name_prefix}-ws-runtime"
  role = aws_iam_role.websocket_lambda[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "dynamodb:DeleteItem",
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:Query",
          "dynamodb:Scan",
          "dynamodb:UpdateItem"
        ]
        Resource = [
          module.websocket_api.connections_table_arn,
          "${module.websocket_api.connections_table_arn}/index/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "execute-api:ManageConnections"
        ]
        Resource = module.websocket_api.manage_connections_arn
      }
    ]
  })
}

module "cognito" {
  source = "../modules/aws/cognito_user_pool"

  name_prefix             = local.name_prefix
  domain_prefix           = local.cognito_domain_prefix
  mfa_configuration       = var.cognito_mfa_configuration
  password_minimum_length = var.cognito_password_minimum_length
  callback_urls           = var.cognito_callback_urls
  logout_urls             = var.cognito_logout_urls
  admin_users             = var.cognito_admin_users
  google_enabled          = var.google_enabled
  google_client_id        = var.google_client_id
  google_client_secret    = var.google_client_secret
  microsoft_enabled       = var.azuread_enabled
  microsoft_tenant_id     = var.microsoft_tenant_id
  microsoft_client_id     = var.azuread_enabled ? module.microsoft_oauth[0].client_id : ""
  microsoft_client_secret = var.azuread_enabled ? module.microsoft_oauth[0].client_secret : ""
  microsoft_provider_name = var.microsoft_provider_name
}

module "network" {
  source = "../modules/aws/network"

  name_prefix = local.name_prefix
  vpc_cidr    = var.network_vpc_cidr
}

module "poc_site" {
  source = "../modules/aws/s3_static_site"
  count  = var.poc_site_enabled ? 1 : 0

  bucket_name   = local.poc_site_domain
  force_destroy = var.poc_site_force_destroy
}

data "cloudflare_zone" "poc_site" {
  count = var.poc_site_enabled && var.poc_site_cloudflare_enabled && var.cloudflare_zone_id != "" ? 1 : 0

  zone_id = var.cloudflare_zone_id
}

resource "cloudflare_record" "poc_site" {
  count = var.poc_site_enabled && var.poc_site_cloudflare_enabled && var.cloudflare_zone_id != "" ? 1 : 0

  zone_id = data.cloudflare_zone.poc_site[0].id
  name    = var.poc_site_subdomain != "" ? var.poc_site_subdomain : "@"
  type    = "CNAME"
  content = module.poc_site[0].website_domain
  proxied = true
  ttl     = 1
}

resource "cloudflare_workers_script" "poc_site" {
  count = var.poc_site_enabled && var.poc_site_cloudflare_worker_enabled && var.cloudflare_account_id != "" ? 1 : 0

  account_id = var.cloudflare_account_id
  name       = "${local.name_prefix}-poc-site"
  content = templatefile("${path.module}/templates/poc-site-worker.js.tftpl", {
    origin = module.poc_site[0].website_endpoint
  })
}

resource "cloudflare_workers_route" "poc_site" {
  count = var.poc_site_enabled && var.poc_site_cloudflare_worker_enabled && var.cloudflare_zone_id != "" && var.cloudflare_account_id != "" ? 1 : 0

  zone_id     = data.cloudflare_zone.poc_site[0].id
  pattern     = "${local.poc_site_domain}/*"
  script_name = cloudflare_workers_script.poc_site[0].name
}

module "postgres_security_group" {
  source = "../modules/aws/security_group"
  count  = var.postgres_enabled ? 1 : 0

  name        = "${local.name_prefix}-postgres"
  description = "PostgreSQL access"
  vpc_id      = module.network.vpc_id

  ingress_rules = concat([
    for cidr in var.postgres_allowed_cidr_blocks : {
      description = "PostgreSQL from ${cidr}"
      from_port   = 5432
      to_port     = 5432
      protocol    = "tcp"
      cidr_blocks = [cidr]
    }
    ], local.api_lambda_vpc_enabled ? [
    {
      description              = "PostgreSQL from API Lambda"
      from_port                = 5432
      to_port                  = 5432
      protocol                 = "tcp"
      source_security_group_id = module.api_lambda_security_group[0].security_group_id
    }
  ] : [])
}

module "api_lambda_security_group" {
  source = "../modules/aws/security_group"
  count  = local.api_lambda_vpc_enabled ? 1 : 0

  name        = "${local.name_prefix}-http-lambda"
  description = "HTTP Lambda access"
  vpc_id      = module.network.vpc_id
}

module "postgres_subnet_group" {
  source = "../modules/aws/db_subnet_group"
  count  = var.postgres_enabled ? 1 : 0

  name       = "${local.name_prefix}-postgres"
  subnet_ids = module.network.private_subnet_ids
}

module "postgres" {
  source = "../modules/aws/postgres"
  count  = var.postgres_enabled ? 1 : 0

  name_prefix             = local.name_prefix
  db_name                 = var.postgres_db_name
  master_username         = var.postgres_master_username
  master_password         = var.postgres_master_password
  instance_class          = var.postgres_instance_class
  allocated_storage       = var.postgres_allocated_storage
  max_allocated_storage   = var.postgres_max_allocated_storage
  engine_version          = var.postgres_engine_version
  publicly_accessible     = var.postgres_publicly_accessible
  db_subnet_group_name    = module.postgres_subnet_group[0].db_subnet_group_name
  security_group_ids      = [module.postgres_security_group[0].security_group_id]
  deletion_protection     = var.postgres_deletion_protection
  skip_final_snapshot     = var.postgres_skip_final_snapshot
  backup_retention_period = var.postgres_backup_retention_period
}

module "app_bucket" {
  source = "../modules/aws/s3_bucket"

  name_prefix          = local.name_prefix
  bucket_name          = var.s3_bucket_name
  purpose              = "app"
  versioning           = var.s3_versioning
  force_destroy        = var.s3_force_destroy
  cors_allowed_origins = var.api_cors_allowed_origins
  lifecycle_expiration_rules = var.s3_upload_expiration_days > 0 ? [
    {
      id     = "expire-pending-uploads"
      prefix = "${var.env}/uploads/"
      days   = var.s3_upload_expiration_days
    }
  ] : []
}

module "ecr_api" {
  source = "../modules/aws/ecr_repository"

  repository_name      = local.name_prefix
  image_tag_mutability = var.ecr_image_tag_mutability
  scan_on_push         = var.ecr_scan_on_push
  keep_last_images     = var.ecr_keep_last_images
}

module "api_lambda_logs" {
  source = "../modules/aws/cloudwatch_log_group"
  count  = local.api_http_deploy_enabled ? 1 : 0

  name              = "/aws/lambda/${local.name_prefix}-http"
  retention_in_days = var.http_api_log_retention_days
}

module "api_lambda" {
  source = "../modules/aws/lambda_function"
  count  = local.api_http_deploy_enabled ? 1 : 0

  function_name = "${local.name_prefix}-http"
  role_arn      = aws_iam_role.api_lambda[0].arn
  package_type  = "Image"
  image_uri     = local.api_lambda_image_uri
  timeout       = var.api_lambda_timeout
  memory_size   = var.api_lambda_memory_size

  subnet_ids         = local.api_lambda_vpc_enabled ? module.network.private_subnet_ids : []
  security_group_ids = local.api_lambda_vpc_enabled ? [module.api_lambda_security_group[0].security_group_id] : []

  environment_variables = merge(
    {
      COGNITO_CLIENT_ID                = module.cognito.user_pool_client_id
      COGNITO_TOKEN_USE                = var.api_cognito_token_use
      COGNITO_USER_POOL_ID             = module.cognito.user_pool_id
      APP_CORS_ALLOWED_ORIGINS         = join(",", var.api_cors_allowed_origins)
      APP_S3_ALLOWED_IMAGE_MIME_TYPES  = join(",", var.api_s3_allowed_image_mime_types)
      APP_S3_BUCKET_NAME               = module.app_bucket.bucket_name
      APP_S3_KEY_PREFIX                = "${var.env}/uploads"
      APP_S3_PROFILE_IMAGE_MAX_BYTES   = tostring(var.api_s3_profile_image_max_bytes)
      APP_S3_REGION                    = var.aws_region
      APP_S3_UPLOAD_URL_TTL_SECONDS    = tostring(var.api_s3_upload_url_ttl_seconds)
      LOG_LEVEL                        = var.api_log_level
      NODE_ENV                         = "production"
      WEBSOCKET_CONNECTION_TTL_SECONDS = tostring(var.websocket_connection_ttl_seconds)
      WEBSOCKET_CONNECTIONS_TABLE_NAME = local.websocket_connections_table_name
      WEBSOCKET_ENDPOINT               = module.websocket_api.websocket_url
      WEBSOCKET_MANAGE_CONNECTIONS_ARN = module.websocket_api.manage_connections_arn
    },
    var.postgres_enabled ? {
      DATABASE_URL = local.database_url
    } : {},
    local.api_database_secret_arn != "" ? {
      DATABASE_SECRET_ARN = local.api_database_secret_arn
    } : {},
    var.error_documentation_base_url != "" ? {
      ERROR_DOCUMENTATION_BASE_URL = var.error_documentation_base_url
    } : {}
  )

  depends_on = [
    module.api_lambda_logs,
    aws_iam_role_policy.api_runtime,
    aws_iam_role_policy_attachment.api_lambda_basic_execution,
    aws_iam_role_policy_attachment.api_lambda_vpc_access
  ]
}

module "http_api" {
  source = "../modules/aws/http_api"
  count  = local.api_http_deploy_enabled ? 1 : 0

  name_prefix               = local.name_prefix
  stage_name                = var.http_api_stage_name
  lambda_invoke_arn         = module.api_lambda[0].invoke_arn
  lambda_function_name      = module.api_lambda[0].function_name
  access_log_enabled        = var.http_api_access_log_enabled
  access_log_retention_days = var.http_api_log_retention_days
  cors_allowed_origins      = var.api_cors_allowed_origins

  depends_on = [
    module.api_gateway_account_cloudwatch
  ]
}

module "websocket_connect_lambda_logs" {
  source = "../modules/aws/cloudwatch_log_group"
  count  = local.websocket_lambda_deploy_enabled ? 1 : 0

  name              = "/aws/lambda/${local.name_prefix}-ws-connect"
  retention_in_days = var.websocket_log_retention_days
}

module "websocket_disconnect_lambda_logs" {
  source = "../modules/aws/cloudwatch_log_group"
  count  = local.websocket_lambda_deploy_enabled ? 1 : 0

  name              = "/aws/lambda/${local.name_prefix}-ws-disconnect"
  retention_in_days = var.websocket_log_retention_days
}

module "websocket_default_lambda_logs" {
  source = "../modules/aws/cloudwatch_log_group"
  count  = local.websocket_lambda_deploy_enabled ? 1 : 0

  name              = "/aws/lambda/${local.name_prefix}-ws-default"
  retention_in_days = var.websocket_log_retention_days
}

module "websocket_connect_lambda" {
  source = "../modules/aws/lambda_function"
  count  = local.websocket_lambda_deploy_enabled ? 1 : 0

  function_name    = "${local.name_prefix}-ws-connect"
  role_arn         = aws_iam_role.websocket_lambda[0].arn
  package_type     = "Zip"
  filename         = var.websocket_lambda_package_filename
  source_code_hash = local.websocket_lambda_source_code_hash
  handler          = "functions/websocket/connect.handler"
  runtime          = var.websocket_lambda_runtime
  timeout          = var.websocket_lambda_timeout
  memory_size      = var.websocket_lambda_memory_size

  environment_variables = {
    COGNITO_CLIENT_ID                = module.cognito.user_pool_client_id
    COGNITO_TOKEN_USE                = var.api_cognito_token_use
    COGNITO_USER_POOL_ID             = module.cognito.user_pool_id
    LOG_LEVEL                        = var.api_log_level
    WEBSOCKET_CONNECTION_TTL_SECONDS = tostring(var.websocket_connection_ttl_seconds)
    WEBSOCKET_CONNECTIONS_TABLE_NAME = local.websocket_connections_table_name
  }

  depends_on = [
    module.websocket_connect_lambda_logs,
    aws_iam_role_policy_attachment.websocket_lambda_basic_execution
  ]
}

module "websocket_disconnect_lambda" {
  source = "../modules/aws/lambda_function"
  count  = local.websocket_lambda_deploy_enabled ? 1 : 0

  function_name    = "${local.name_prefix}-ws-disconnect"
  role_arn         = aws_iam_role.websocket_lambda[0].arn
  package_type     = "Zip"
  filename         = var.websocket_lambda_package_filename
  source_code_hash = local.websocket_lambda_source_code_hash
  handler          = "functions/websocket/disconnect.handler"
  runtime          = var.websocket_lambda_runtime
  timeout          = var.websocket_lambda_timeout
  memory_size      = var.websocket_lambda_memory_size

  environment_variables = {
    COGNITO_CLIENT_ID                = module.cognito.user_pool_client_id
    COGNITO_TOKEN_USE                = var.api_cognito_token_use
    COGNITO_USER_POOL_ID             = module.cognito.user_pool_id
    LOG_LEVEL                        = var.api_log_level
    WEBSOCKET_CONNECTIONS_TABLE_NAME = local.websocket_connections_table_name
    WEBSOCKET_CONNECTION_TTL_SECONDS = tostring(var.websocket_connection_ttl_seconds)
  }

  depends_on = [
    module.websocket_disconnect_lambda_logs,
    aws_iam_role_policy_attachment.websocket_lambda_basic_execution
  ]
}

module "websocket_default_lambda" {
  source = "../modules/aws/lambda_function"
  count  = local.websocket_lambda_deploy_enabled ? 1 : 0

  function_name    = "${local.name_prefix}-ws-default"
  role_arn         = aws_iam_role.websocket_lambda[0].arn
  package_type     = "Zip"
  filename         = var.websocket_lambda_package_filename
  source_code_hash = local.websocket_lambda_source_code_hash
  handler          = "functions/websocket/default.handler"
  runtime          = var.websocket_lambda_runtime
  timeout          = var.websocket_lambda_timeout
  memory_size      = var.websocket_lambda_memory_size

  environment_variables = {
    COGNITO_CLIENT_ID                = module.cognito.user_pool_client_id
    COGNITO_TOKEN_USE                = var.api_cognito_token_use
    COGNITO_USER_POOL_ID             = module.cognito.user_pool_id
    LOG_LEVEL                        = var.api_log_level
    WEBSOCKET_CONNECTIONS_TABLE_NAME = local.websocket_connections_table_name
    WEBSOCKET_CONNECTION_TTL_SECONDS = tostring(var.websocket_connection_ttl_seconds)
  }

  depends_on = [
    module.websocket_default_lambda_logs,
    aws_iam_role_policy_attachment.websocket_lambda_basic_execution
  ]
}

module "websocket_api" {
  source = "../modules/aws/websocket_api"

  name_prefix                                = local.name_prefix
  route_selection_expression                 = var.websocket_route_selection_expression
  stage_name                                 = var.websocket_stage_name
  auto_deploy                                = var.websocket_auto_deploy
  access_log_enabled                         = var.websocket_access_log_enabled
  access_log_retention_days                  = var.websocket_log_retention_days
  default_route_enabled                      = var.websocket_default_route_enabled
  connections_table_name                     = var.websocket_connections_table_name
  connections_ttl_attribute_name             = var.websocket_connections_ttl_attribute_name
  connections_point_in_time_recovery_enabled = var.websocket_connections_point_in_time_recovery_enabled
  lambda_routes = local.websocket_lambda_deploy_enabled ? {
    "$connect" = {
      invoke_arn    = module.websocket_connect_lambda[0].invoke_arn
      function_name = module.websocket_connect_lambda[0].function_name
    }
    "$disconnect" = {
      invoke_arn    = module.websocket_disconnect_lambda[0].invoke_arn
      function_name = module.websocket_disconnect_lambda[0].function_name
    }
    "$default" = {
      invoke_arn    = module.websocket_default_lambda[0].invoke_arn
      function_name = module.websocket_default_lambda[0].function_name
    }
  } : {}

  depends_on = [
    module.api_gateway_account_cloudwatch
  ]
}

module "github_oidc_deploy" {
  source = "../modules/aws/github_oidc_deploy"
  count  = var.github_actions_enabled && length(local.github_deploy_repositories) > 0 ? 1 : 0

  name_prefix                = local.name_prefix
  repository                 = var.github_repository
  repositories               = local.github_deploy_repositories
  branch                     = var.github_branch
  environment                = var.env
  existing_oidc_provider_arn = var.github_oidc_provider_arn
  poc_site_bucket_name       = var.poc_site_enabled ? module.poc_site[0].bucket_name : ""
  ecr_repository_arn         = module.ecr_api.repository_arn
  secrets_manager_secret_arns = local.api_database_secret_arn != "" ? [
    local.api_database_secret_arn
  ] : []
  lambda_function_arns = compact(concat(
    local.api_http_deploy_enabled ? [module.api_lambda[0].function_arn] : [],
    local.websocket_lambda_deploy_enabled ? [
      module.websocket_connect_lambda[0].function_arn,
      module.websocket_disconnect_lambda[0].function_arn,
      module.websocket_default_lambda[0].function_arn
    ] : []
  ))
}
