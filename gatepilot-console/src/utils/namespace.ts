const STORAGE_KEY = 'gatepilot.console.namespace';
const CHANGE_EVENT = 'gatepilot:namespace-change';

export function getGlobalNamespace(fallback = 'default') {
  return window.localStorage.getItem(STORAGE_KEY) || fallback;
}

export function setGlobalNamespace(namespace: string) {
  const value = namespace || '';
  window.localStorage.setItem(STORAGE_KEY, value);
  window.dispatchEvent(new CustomEvent(CHANGE_EVENT, { detail: value }));
}

export function onGlobalNamespaceChange(handler: (namespace: string) => void) {
  const listener = (event: Event) => {
    handler((event as CustomEvent<string>).detail || '');
  };
  window.addEventListener(CHANGE_EVENT, listener);
  return () => window.removeEventListener(CHANGE_EVENT, listener);
}
