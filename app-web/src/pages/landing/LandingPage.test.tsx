import { render, screen } from '@testing-library/react';

import { LandingPage } from './LandingPage';

describe('LandingPage', () => {
  it('renders the product landing content and primary calls to action', () => {
    render(<LandingPage />);

    expect(
      screen.getByRole('heading', { name: /reservá cancha y armá la mejenga/i }),
    ).toBeInTheDocument();
    expect(screen.getByText(/la app tica para encontrar cancha/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /descargar la app/i })).toHaveAttribute(
      'href',
      '#descargar',
    );
    expect(screen.getByRole('link', { name: /soy dueño de cancha/i })).toHaveAttribute(
      'href',
      '#duenos',
    );
  });

  it('shows both marketplace audiences and the guided steps', () => {
    render(<LandingPage />);

    expect(screen.getByRole('heading', { name: /para jugadores/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /para dueños/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /cómo funciona/i })).toBeInTheDocument();
    expect(screen.getByText('Buscá')).toBeInTheDocument();
    expect(screen.getByText('Elegí la hora')).toBeInTheDocument();
    expect(screen.getByText('Reservá y jugá')).toBeInTheDocument();
  });
});
