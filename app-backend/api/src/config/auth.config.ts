/**
 * Cognito token type accepted by the API.
 */
export type CognitoTokenUse = 'access' | 'id';

/**
 * Authentication provider settings.
 */
export interface IAuthConfig {
  /**
   * AWS region where Cognito is deployed.
   */
  awsRegion: string;

  /**
   * Cognito user pool ID.
   */
  cognitoUserPoolId: string;

  /**
   * Cognito app client ID.
   */
  cognitoClientId: string;

  /**
   * Accepted Cognito token use.
   */
  cognitoTokenUse: CognitoTokenUse;
}

/**
 * Loads Cognito authentication settings.
 *
 * @returns Auth config section.
 */
export function authConfig(): IAuthConfig {
  return {
    awsRegion: process.env.AWS_REGION ?? 'us-east-2',
    cognitoUserPoolId: process.env.COGNITO_USER_POOL_ID ?? '',
    cognitoClientId: process.env.COGNITO_CLIENT_ID ?? '',
    cognitoTokenUse: (process.env.COGNITO_TOKEN_USE ?? 'id') as CognitoTokenUse
  };
}
