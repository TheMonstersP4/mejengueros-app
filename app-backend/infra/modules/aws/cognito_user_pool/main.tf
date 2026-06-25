data "aws_region" "current" {}

locals {
  admin_users_by_email = {
    for user in var.admin_users : lower(trimspace(user.email)) => user
    if trimspace(user.email) != ""
  }

  microsoft_authority = "https://login.microsoftonline.com/${var.microsoft_tenant_id}"
  microsoft_issuer    = "${local.microsoft_authority}/v2.0"

  supported_identity_providers = concat(
    ["COGNITO"],
    var.google_enabled ? ["Google"] : [],
    var.microsoft_enabled ? [var.microsoft_provider_name] : []
  )
}

# Email is the username so Cognito, Google, and Microsoft sign-ins share the
# same user-facing identifier.
resource "aws_cognito_user_pool" "user_pool" {
  name = "${var.name_prefix}-users"

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]
  mfa_configuration        = var.mfa_configuration
  email_verification_subject = var.email_verification_subject
  email_verification_message = var.email_verification_message

  admin_create_user_config {
    allow_admin_create_user_only = !var.self_signup_enabled
  }

  password_policy {
    minimum_length    = var.password_minimum_length
    require_lowercase = true
    require_numbers   = true
    require_symbols   = true
    require_uppercase = true
  }

  dynamic "software_token_mfa_configuration" {
    for_each = var.mfa_configuration == "OFF" ? [] : [1]

    content {
      enabled = true
    }
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  user_attribute_update_settings {
    attributes_require_verification_before_update = ["email"]
  }

  schema {
    attribute_data_type = "String"
    name                = "email"
    required            = true
    mutable             = true

    string_attribute_constraints {
      min_length = 5
      max_length = 256
    }
  }
}

# Enable only after the OAuth app exists and secrets are available locally.
resource "aws_cognito_identity_provider" "google" {
  count = var.google_enabled ? 1 : 0

  user_pool_id  = aws_cognito_user_pool.user_pool.id
  provider_name = "Google"
  provider_type = "Google"

  provider_details = {
    attributes_url                = "https://people.googleapis.com/v1/people/me?personFields="
    attributes_url_add_attributes = "true"
    authorize_scopes              = "openid email profile"
    authorize_url                 = "https://accounts.google.com/o/oauth2/v2/auth"
    client_id                     = var.google_client_id
    client_secret                 = var.google_client_secret
    oidc_issuer                   = "https://accounts.google.com"
    token_request_method          = "POST"
    token_url                     = "https://www.googleapis.com/oauth2/v4/token"
  }

  attribute_mapping = {
    email    = "email"
    name     = "name"
    username = "sub"
  }
}

resource "aws_cognito_identity_provider" "microsoft" {
  count = var.microsoft_enabled ? 1 : 0

  user_pool_id  = aws_cognito_user_pool.user_pool.id
  provider_name = var.microsoft_provider_name
  provider_type = "OIDC"

  provider_details = {
    attributes_url_add_attributes = "false"
    client_id                     = var.microsoft_client_id
    client_secret                 = var.microsoft_client_secret
    authorize_scopes              = "openid email profile"
    oidc_issuer                   = local.microsoft_issuer
    authorize_url                 = "${local.microsoft_authority}/oauth2/v2.0/authorize"
    token_url                     = "${local.microsoft_authority}/oauth2/v2.0/token"
    attributes_url                = "https://graph.microsoft.com/oidc/userinfo"
    attributes_request_method     = "GET"
  }

  attribute_mapping = {
    email    = "email"
    name     = "name"
    username = "sub"
  }
}

resource "aws_cognito_user_pool_client" "app_client" {
  name         = "${var.name_prefix}-web-client"
  user_pool_id = aws_cognito_user_pool.user_pool.id

  generate_secret                      = false
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["email", "openid", "profile"]
  supported_identity_providers         = local.supported_identity_providers
  callback_urls                        = var.callback_urls
  logout_urls                          = var.logout_urls
  prevent_user_existence_errors        = "ENABLED"

  explicit_auth_flows = [
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_SRP_AUTH",
    "ALLOW_USER_PASSWORD_AUTH"
  ]

  token_validity_units {
    access_token  = "hours"
    id_token      = "hours"
    refresh_token = "days"
  }

  access_token_validity  = 1
  id_token_validity      = 1
  refresh_token_validity = 30

  depends_on = [
    aws_cognito_identity_provider.google,
    aws_cognito_identity_provider.microsoft
  ]
}

resource "aws_cognito_user_pool_domain" "domain" {
  domain       = var.domain_prefix
  user_pool_id = aws_cognito_user_pool.user_pool.id
}

resource "aws_cognito_user_group" "admin" {
  name         = "Admin"
  user_pool_id = aws_cognito_user_pool.user_pool.id
  description  = "Administrative users"
}

resource "aws_cognito_user" "admin" {
  for_each = nonsensitive(local.admin_users_by_email)

  user_pool_id = aws_cognito_user_pool.user_pool.id
  username     = each.value.email

  attributes = {
    email          = each.value.email
    email_verified = true
  }

  temporary_password = try(each.value.temporary_password, null)
  message_action     = "SUPPRESS"
}

resource "aws_cognito_user_in_group" "admin" {
  for_each = aws_cognito_user.admin

  user_pool_id = aws_cognito_user_pool.user_pool.id
  group_name   = aws_cognito_user_group.admin.name
  username     = each.value.username
}
