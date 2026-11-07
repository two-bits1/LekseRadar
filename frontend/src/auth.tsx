// Вход по API.md, «Как работает вход»: токен в localStorage, Bearer во все запросы,
// 🔒-страницы без токена → /login, «Выйти» = удалить токен (отдельного API нет).
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { authApi, usersApi } from './api/endpoints';
import { clearToken, getToken, setToken } from './api/client';
import type { AuthResponse, LoginRequest, RegisterRequest, RegisterStarted, User, VerifyEmailRequest } from './api/types';
import { PATHS } from './paths';

type Auth = {
  token: string | null;
  user: User | null;  // null, пока GET /users/me не ответил
  login: (body: LoginRequest) => Promise<void>;
  register: (body: RegisterRequest) => Promise<RegisterStarted>;  // шаг 1: токена ещё нет
  verifyEmail: (body: VerifyEmailRequest) => Promise<void>;       // шаг 2: код → токен
  logout: () => void;
  updateUser: (user: User) => void;  // после PATCH /users/me
};

const AuthContext = createContext<Auth | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState(getToken);
  const [user, setUser] = useState<User | null>(null);

  // Токен остался с прошлого визита → подтягиваем профиль. Ответ 401 клиент API сам отправит на /login.
  useEffect(() => {
    if (!token || user) return;
    let cancelled = false;
    usersApi.me().then(u => { if (!cancelled) setUser(u); }).catch(() => {});
    return () => { cancelled = true; };
  }, [token]); // user намеренно не в зависимостях: после входа он уже пришёл в AuthResponse

  const accept = (res: AuthResponse) => { setToken(res.token); setUser(res.user); setTokenState(res.token); };

  const value: Auth = {
    token,
    user,
    login: async body => accept(await authApi.login(body)),
    register: body => authApi.register(body),
    verifyEmail: async body => accept(await authApi.verifyEmail(body)),
    logout: () => { clearToken(); setUser(null); setTokenState(null); },
    updateUser: setUser,
  };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const auth = useContext(AuthContext);
  if (!auth) throw new Error('useAuth() должен вызываться внутри <AuthProvider>');
  return auth;
}

// Обёртка для 🔒-маршрутов: нет токена → /login.
export function RequireAuth() {
  const { token } = useAuth();
  return token ? <Outlet /> : <Navigate to={PATHS.login} replace />;
}

export const initialOf = (user: User | null) => (user?.displayName || user?.email || '?').charAt(0).toUpperCase();
