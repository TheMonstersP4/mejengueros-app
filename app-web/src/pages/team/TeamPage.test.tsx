import { render, screen } from '@testing-library/react';

import { TeamPage } from './TeamPage';

describe('TeamPage', () => {
  it('renders the team identity and public navigation', () => {
    render(<TeamPage />);

    expect(
      screen.getByRole('heading', { name: /el equipo detrás de la mejenga/i }),
    ).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: /producto/i })[0]).toHaveAttribute('href', '/');
    expect(screen.getAllByRole('link', { name: /equipo/i })[0]).toHaveAttribute(
      'href',
      '/team',
    );
  });

  it('shows the team members and delivery tracks', () => {
    render(<TeamPage />);

    expect(screen.getByRole('heading', { name: /roles claros/i })).toBeInTheDocument();
    expect(screen.getByText('Shanty Cerdas')).toBeInTheDocument();
    expect(screen.getByText('David Gutiérrez')).toBeInTheDocument();
    expect(screen.getByText('Maxwell Chinchilla')).toBeInTheDocument();
    expect(screen.getByText('Daniel Nazario')).toBeInTheDocument();
    expect(screen.getByText('Carl Levey')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /kmp \+ react/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /arquitectura cloud/i })).toBeInTheDocument();
    expect(screen.getByText('API Gateway + Lambda')).toBeInTheDocument();
  });
});
