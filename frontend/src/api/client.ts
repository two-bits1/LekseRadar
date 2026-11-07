// Обёртка над fetch для бэкенда — по разделу «Как работает вход» в API.md.
import { PATHS } from '../paths';
import type { ApiError } from './types';

// В dev запросы /api/... идут через proxy Vite на http://localhost:8080 (vite.config.ts).
// Для продакшена задайте VITE_API_URL, например https://api.example.no/api/v1.
export const API_BASE: string = import.meta.env.VITE_API_URL ?? '/api/v1';

const TOKEN_KEY = 'token';
export const getToken = () => { try { return localStorage.getItem(TOKEN_KEY); } catch { return null; } };
export const setToken = (token: string) => { try { localStorage.setItem(TOKEN_KEY, token); } catch {} };
export const clearToken = () => { try { localStorage.removeItem(TOKEN_KEY); } catch {} };

export const isApiError = (e: unknown): e is ApiError =>
  typeof e === 'object' && e !== null && 'status' in e && 'message' in e;

async function toApiError(res: Response): Promise<ApiError> {
  try { const body = await res.json(); if (isApiError(body)) return body; } catch {}
  // Ответ не в формате ApiError (например, бэкенд не запущен и proxy вернул 502 без JSON).
  return { status: res.status, message: 'Noe gikk galt. Prøv igjen.', fields: null, timestamp: new Date().toISOString() };
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const res = await fetch(API_BASE + path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  // 401 на /auth/login — это «неверный пароль», его показываем в форме, а не выкидываем на вход.
  if (res.status === 401 && !path.startsWith('/auth/')) { clearToken(); window.location.assign(PATHS.login); }
  if (!res.ok) throw await toApiError(res);
  return res.status === 204 ? (undefined as T) : res.json();
}
