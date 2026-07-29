import { render } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';

import { TeamPage } from './TeamPage';

expect.extend(toHaveNoViolations);

describe('TeamPage accessibility', () => {
  it('has no basic accessibility violations', async () => {
    const { container } = render(<TeamPage />);

    await expect(axe(container)).resolves.toHaveNoViolations();
  });
});
