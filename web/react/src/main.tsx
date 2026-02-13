import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { PreferencesProvider } from './hooks/usePreferences';
import App from './App';
import './i18n';

// Register service worker
if ('serviceWorker' in navigator) {
	window.addEventListener('load', () => {
		navigator.serviceWorker.register(import.meta.env.BASE_URL + 'service-worker.js');
	});
}

createRoot(document.getElementById('root')!).render(
	<StrictMode>
		<PreferencesProvider>
			<App />
		</PreferencesProvider>
	</StrictMode>,
);
