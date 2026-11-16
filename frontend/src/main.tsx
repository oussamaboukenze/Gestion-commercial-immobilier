import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { keycloak } from './auth/keycloak';
import App from './App';

const root = createRoot(document.getElementById('root')!);

keycloak
  .init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false })
  .then(() => root.render(<StrictMode><App /></StrictMode>))
  .catch(() => {
    root.render(
      <div style={{ fontFamily: 'sans-serif', padding: 40 }}>
        <h2>Connexion impossible</h2>
        <p>Keycloak ne répond pas ({import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180'}).
          Vérifiez que <code>docker compose up -d</code> est lancé.</p>
      </div>,
    );
  });
