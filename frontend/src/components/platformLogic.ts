// Логика платформ без React: тело запроса, обязательные поля, состояние карточки, возврат из Google.
// Тела — как ждёт бэкенд (POST /platforms/{platform}, snake_case, см. FIX-frontend-auth-platforms).
import type { PlatformConnection, PlatformCredentials, PlatformKey } from '../api/types';

export type FieldKey = 'base_url' | 'token' | 'refresh_token' | 'access_token' | 'host' | 'JSESSIONID' | 'Authorization';
export type Values = Partial<Record<FieldKey, string>>;

// Поля ручной формы и какие из них серверу обязательны.
export const FIELDS: Record<Exclude<PlatformKey, 'classroom'>, FieldKey[]> = {
  canvas: ['base_url', 'token'],
  inschool: ['host', 'JSESSIONID', 'Authorization'],
  itslearning: ['base_url', 'refresh_token'],
  teams: ['refresh_token', 'access_token'],
};
const REQUIRED: Record<Exclude<PlatformKey, 'classroom'>, FieldKey[]> = {
  canvas: ['base_url', 'token'],
  inschool: ['host', 'JSESSIONID'],
  itslearning: ['base_url', 'refresh_token'],
  teams: [],  // хватает одного из двух токенов, см. missingFields
};

const clean = (values: Values) => (k: FieldKey) => (values[k] ?? '').trim();

export function missingFields(key: Exclude<PlatformKey, 'classroom'>, values: Values): FieldKey[] {
  const v = clean(values);
  if (key === 'teams') return v('refresh_token') || v('access_token') ? [] : ['refresh_token'];
  return REQUIRED[key].filter(f => !v(f));
}

// Пустые строки сервер считает «не указано» — их просто не шлём.
export function toCredentials(key: Exclude<PlatformKey, 'classroom'>, values: Values): PlatformCredentials {
  const v = clean(values);
  const pick = (keys: FieldKey[]) => Object.fromEntries(keys.filter(k => v(k)).map(k => [k, v(k)]));
  if (key === 'inschool') return { host: v('host'), cookies: pick(['JSESSIONID', 'Authorization']) };
  return pick(FIELDS[key]);
}

export type CardState = 'off' | 'on' | 'reconnect' | 'error';
export function cardState(c: PlatformConnection | undefined): CardState {
  if (!c || !c.connected) return c?.needsReconnect ? 'reconnect' : 'off';
  if (c.needsReconnect) return 'reconnect';
  return c.lastError ? 'error' : 'on';
}

// Google возвращает ученика на /settings/platforms?code=…&state=… (или ?error=…).
export type OAuthReturn = { code: string; state: string } | { error: string } | null;
export function oauthReturn(search: string): OAuthReturn {
  const q = new URLSearchParams(search);
  if (q.get('error')) return { error: q.get('error')! };
  const code = q.get('code'); const state = q.get('state');
  return code && state ? { code, state } : null;
}
