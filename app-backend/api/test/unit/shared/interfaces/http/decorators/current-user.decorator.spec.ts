const mockCreateParamDecorator = jest.fn((factory) => factory);

jest.mock('@nestjs/common', () => ({
  createParamDecorator: mockCreateParamDecorator
}));

import { CurrentUser } from '@/shared/interfaces/http/decorators/current-user.decorator';

describe('CurrentUser', () => {
  it('returns the authenticated user from the HTTP request', () => {
    const user = {
      subject: 'user-123',
      email: 'user@example.com',
      name: 'Example User',
      provider: 'Google'
    };
    const context = {
      switchToHttp: jest.fn().mockReturnValue({
        getRequest: jest.fn().mockReturnValue({ user })
      })
    };

    expect(CurrentUser(undefined, context as never)).toBe(user);
    expect(mockCreateParamDecorator).toHaveBeenCalledTimes(1);
  });
});
