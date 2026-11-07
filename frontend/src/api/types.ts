// Типы ответов и запросов бэкенда — как в API.md («Типы»).
export type Locale = 'nb' | 'nn' | 'en' | 'ru';

export type User = {
  id: number;
  email: string;
  displayName: string;
  locale: Locale;          // язык интерфейса и ИИ-тьютора
  timezone: string;        // "Europe/Oslo"
  createdAt: string;       // ISO-8601, UTC
  lastLoginAt: string | null;
};

export type AuthResponse = { token: string; user: User };

export type Chat = {
  id: number;
  title: string;           // "Ny samtale", пока нет первого вопроса
  topic: string | null;
  archived: boolean;
  createdAt: string;
  updatedAt: string;       // время последнего сообщения
};

export type Message = {
  id: number;
  role: 'student' | 'tutor';
  content: string;         // у tutor — Markdown
  model: string | null;    // какая модель ответила (только у tutor)
  createdAt: string;
};

export type ChatReply = { chat: Chat; userMessage: Message; assistantMessage: Message };

export type ApiError = {
  status: number;
  message: string;                        // готовый текст для показа пользователю
  fields: Record<string, string> | null;  // ошибки по полям формы (только 400)
  timestamp: string;
};

export type Health = { status: 'UP' | 'DOWN' };

// Тела запросов
export type RegisterRequest = { email: string; password: string; displayName: string };
export type LoginRequest = { email: string; password: string };
export type UpdateUserRequest = Partial<Pick<User, 'displayName' | 'locale' | 'timezone'>>;
export type ChangePasswordRequest = { currentPassword: string; newPassword: string };
export type CreateChatRequest = { title?: string; topic?: string };
export type UpdateChatRequest = { title?: string; topic?: string; archived?: boolean };
