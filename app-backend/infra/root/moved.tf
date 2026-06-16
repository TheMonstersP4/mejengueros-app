moved {
  from = aws_iam_role.api_gateway_cloudwatch[0]
  to   = module.api_gateway_account_cloudwatch[0].aws_iam_role.cloudwatch
}

moved {
  from = aws_iam_role_policy_attachment.api_gateway_cloudwatch[0]
  to   = module.api_gateway_account_cloudwatch[0].aws_iam_role_policy_attachment.cloudwatch
}

moved {
  from = aws_api_gateway_account.cloudwatch[0]
  to   = module.api_gateway_account_cloudwatch[0].aws_api_gateway_account.cloudwatch
}
