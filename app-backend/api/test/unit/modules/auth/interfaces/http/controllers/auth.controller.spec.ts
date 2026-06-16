import { AuthController } from '@/modules/auth/interfaces/http/controllers/auth.controller';

describe('AuthController', () => {
  it('returns the authenticated user attached by the guard', () => {
    const user = {
      sub: 'user-1',
      email: 'user@example.test',
      groups: ['admin']
    };

    expect(new AuthController().me(user)).toBe(user);
  });
});
