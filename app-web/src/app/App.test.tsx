import { render, screen } from '@testing-library/react';

import { App } from './App';

describe('App routing', () => {
  afterEach(() => {
    window.history.pushState({}, '', '/');
  });

  it('renders the product landing page at the root route', () => {
    window.history.pushState({}, '', '/');

    render(<App />);

    expect(screen.getByText(/canchas reservables hoy/i)).toBeInTheDocument();
  });

  it('renders the team landing page at the team route', () => {
    window.history.pushState({}, '', '/team');

    render(<App />);

    expect(
      screen.getByRole('heading', { name: /el equipo detrás de la mejenga/i }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /volver al producto/i })).toHaveAttribute(
      'href',
      '/',
    );
  });
});
