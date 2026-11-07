// Все URL бэкенда из API.md (таблица «Все URL бэкенда», #1–#13). Пути — относительно API_BASE (/api/v1).
import { api, API_BASE } from './client';
import type {
  AuthResponse, ChangePasswordRequest, Chat, ChatReply, CreateChatRequest, Health, LoginRequest,
  Message, RegisterRequest, UpdateChatRequest, UpdateUserRequest, User,
} from './types';

const json = (method: string, body: unknown): RequestInit => ({ method, body: JSON.stringify(body) });

// 🔓 Авторизация
export const authApi = {
  register: (body: RegisterRequest) => api<AuthResponse>('/auth/register', json('POST', body)),  // #1 → 201
  login: (body: LoginRequest) => api<AuthResponse>('/auth/login', json('POST', body)),           // #2 → 200
};

// 🔒 Профиль
export const usersApi = {
  me: () => api<User>('/users/me'),                                                                      // #3
  update: (body: UpdateUserRequest) => api<User>('/users/me', json('PATCH', body)),                      // #4
  changePassword: (body: ChangePasswordRequest) => api<void>('/users/me/password', json('PUT', body)),   // #5 → 204
};

// 🔒 ИИ-чат
export const chatsApi = {
  list: (archived?: boolean) => api<Chat[]>(archived === undefined ? '/chats' : `/chats?archived=${archived}`),  // #6
  create: (body: CreateChatRequest = {}) => api<Chat>('/chats', json('POST', body)),                              // #7 → 201
  get: (chatId: number) => api<Chat>(`/chats/${chatId}`),                                                         // #8
  update: (chatId: number, body: UpdateChatRequest) => api<Chat>(`/chats/${chatId}`, json('PATCH', body)),        // #9
  remove: (chatId: number) => api<void>(`/chats/${chatId}`, { method: 'DELETE' }),                                // #10 → 204
  messages: (chatId: number) => api<Message[]>(`/chats/${chatId}/messages`),                                      // #11
  send: (chatId: number, content: string) => api<ChatReply>(`/chats/${chatId}/messages`, json('POST', { content })), // #12 → 201, ответ 2–15 с
};

// 🔓 Служебное: /actuator/health лежит вне /api/v1 (#13)
export const health = async (): Promise<Health> => {
  const origin = new URL(API_BASE, window.location.origin).origin;
  const res = await fetch(`${origin}/actuator/health`);
  return res.json();
};
