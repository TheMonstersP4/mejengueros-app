resource "azuread_application" "oauth_client" {
  display_name     = var.display_name
  sign_in_audience = var.sign_in_audience
  owners           = var.owner_object_ids

  api {
    requested_access_token_version = 2
  }

  web {
    homepage_url  = var.homepage_url
    logout_url    = var.logout_url
    redirect_uris = var.redirect_uris

    implicit_grant {
      access_token_issuance_enabled = false
      id_token_issuance_enabled     = false
    }
  }
}

resource "azuread_application_password" "oauth_client_secret" {
  application_id = azuread_application.oauth_client.id
  display_name   = var.client_secret_display_name
  end_date       = var.client_secret_end_date
}
