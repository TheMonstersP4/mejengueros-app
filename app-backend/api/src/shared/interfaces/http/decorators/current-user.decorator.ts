import type { ExecutionContext } from '@nestjs/common';
import { createParamDecorator } from '@nestjs/common';
import type { FastifyRequest } from 'fastify';
import type { IAuthenticatedUserOutput } from '../../../../modules/auth/application/dto/authenticated-user.output';

/**
 * Reads the authenticated user attached to the Fastify request.
 *
 * @remarks
 * `CognitoAuthGuard` verifies the token and sets `request.user` before this
 * decorator is used by controllers.
 */
export const CurrentUser = createParamDecorator(
  (_data: unknown, context: ExecutionContext): IAuthenticatedUserOutput => {
    const request = context.switchToHttp().getRequest<
      FastifyRequest & { user: IAuthenticatedUserOutput }
    >();

    return request.user;
  }
);
