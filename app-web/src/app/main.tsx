import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { App } from './App';
import '../shared/styles/index.css';

createRoot(document.getElementById('root') as HTMLElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
