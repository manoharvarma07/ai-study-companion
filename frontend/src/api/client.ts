import axios, { AxiosError } from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

export const TOKEN_KEY = 'asc_token';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}
export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

export const api = axios.create({
  baseURL: API_BASE || undefined,
  timeout: 30000,
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let onUnauthorized: (() => void) | null = null;
export function setOnUnauthorized(cb: (() => void) | null) {
  onUnauthorized = cb;
}

api.interceptors.response.use(
  (res) => res,
  (err: AxiosError) => {
    if (err.response?.status === 401 && onUnauthorized) onUnauthorized();
    return Promise.reject(err);
  },
);

export function apiErrorMessage(err: unknown, fallback = 'Something went wrong'): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as unknown;
    if (typeof data === 'string' && data.length > 0) return data;
    if (data && typeof data === 'object') {
      const d = data as Record<string, unknown>;
      if (typeof d.message === 'string') return d.message;
      if (typeof d.error === 'string') return d.error;
    }
    if (!err.response) return 'Backend unreachable. Start the Spring Boot API or enable dev mocks.';
    return fallback;
  }
  return fallback;
}

export function mocksEnabled(): boolean {
  // Mocks are strictly opt-in (dev only). Production builds run with
  // VITE_USE_MOCKS unset/false, so all API errors surface for real.
  return (import.meta.env.VITE_USE_MOCKS ?? 'false') === 'true';
}

/** True for transport-level failures (backend down), false for HTTP errors. */
export function isNetworkError(err: unknown): boolean {
  return axios.isAxiosError(err) && !err.response;
}
