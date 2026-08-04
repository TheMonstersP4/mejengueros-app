import { LandingPage } from '../pages/landing/LandingPage';
import { TeamPage } from '../pages/team/TeamPage';

function normalizePath(pathname: string) {
  const normalizedPath = pathname.replace(/\/+$/, '');

  return normalizedPath === '' ? '/' : normalizedPath;
}

export function App() {
  const currentPath = normalizePath(window.location.pathname);

  if (currentPath === '/team') {
    return <TeamPage />;
  }

  return <LandingPage />;
}
