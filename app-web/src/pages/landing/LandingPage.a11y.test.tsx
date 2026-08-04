import { render } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';

import { LandingPage } from './LandingPage';

expect.extend(toHaveNoViolations);

describe('LandingPage accessibility', () => {
  it('has no basic accessibility violations', async () => {
    const { container } = render(<LandingPage />);

    await expect(axe(container)).resolves.toHaveNoViolations();
  });
});
