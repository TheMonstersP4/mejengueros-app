import { UserProfileResponse } from '@/modules/users/interfaces/http/dto/user-profile.response';

describe('UserProfileResponse', () => {
  it('can be populated by HTTP serialization layers', () => {
    const response = Object.assign(new UserProfileResponse(), {
      id: 'user-id',
      cognitoSub: 'cognito-sub',
      email: 'user@example.test',
      name: 'User Name',
      pictureUrl: 'https://example.test/avatar.png',
      provider: 'Google'
    });

    expect(response).toMatchObject({
      id: 'user-id',
      cognitoSub: 'cognito-sub',
      email: 'user@example.test',
      name: 'User Name',
      pictureUrl: 'https://example.test/avatar.png',
      provider: 'Google'
    });
  });
});
