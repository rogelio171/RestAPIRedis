const TOKEN_KEY = "restapi-redis-jwt";
const AUTH_EVENT = "restapi-redis-auth";

export function getStoredToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

function notifyAuthListeners(): void {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new Event(AUTH_EVENT));
}

export function setStoredToken(token: string): void {
  window.localStorage.setItem(TOKEN_KEY, token);
  notifyAuthListeners();
}

export function clearStoredToken(): void {
  window.localStorage.removeItem(TOKEN_KEY);
  notifyAuthListeners();
}

export function subscribeAuth(callback: () => void): () => void {
  if (typeof window === "undefined") {
    return () => {};
  }
  window.addEventListener(AUTH_EVENT, callback);
  window.addEventListener("storage", callback);
  return () => {
    window.removeEventListener(AUTH_EVENT, callback);
    window.removeEventListener("storage", callback);
  };
}

export function getAuthSnapshot(): boolean {
  return !!getStoredToken();
}

export function getServerAuthSnapshot(): boolean {
  return false;
}
