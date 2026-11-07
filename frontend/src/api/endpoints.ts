// Все URL бэкенда из API.md (таблица «Все URL бэкенда», #1–#34). Пути — относительно API_BASE (/api/v1).
import { api, API_BASE } from './client';
import type {
  AnswerRequest, AnswerResult, Assignment, AssignmentStatus, AuthResponse, Dashboard, HistoryEvent, UpdateAssignmentRequest, ChangePasswordRequest, Chat, ChatReply, CreateChatRequest, Deck, Flashcard,
  FlashcardStats, Health, LoginRequest, Message, Quiz, QuizProgress, QuizSummary, RegisterRequest, ReviewResult,
  StartQuizRequest, Subject, UpdateChatRequest, UpdateUserRequest, User, PlatformConnection, PlatformCredentials, PlatformKey,
} from './types';

const json = (method: string, body?: unknown): RequestInit => body === undefined ? { method } : { method, body: JSON.stringify(body) };

// 🔓 Авторизация
export const authApi = {
  register: (body: RegisterRequest) => api<AuthResponse>('/auth/register', json('POST', body)),  // #1
  login: (body: LoginRequest) => api<AuthResponse>('/auth/login', json('POST', body)),           // #2
};

// 🔒 Профиль
export const usersApi = {
  me: () => api<User>('/users/me'),                                                                      // #3
  update: (body: UpdateUserRequest) => api<User>('/users/me', json('PATCH', body)),                      // #4
  changePassword: (body: ChangePasswordRequest) => api<void>('/users/me/password', json('PUT', body)),   // #5
};

// 🔒 ИИ-чат (тьютор и квиз-чат)
export const chatsApi = {
  list: (archived?: boolean) => api<Chat[]>(archived === undefined ? '/chats' : `/chats?archived=${archived}`),  // #6
  create: (body: CreateChatRequest = {}) => api<Chat>('/chats', json('POST', body)),                              // #7
  get: (chatId: number) => api<Chat>(`/chats/${chatId}`),                                                         // #8
  update: (chatId: number, body: UpdateChatRequest) => api<Chat>(`/chats/${chatId}`, json('PATCH', body)),        // #9
  remove: (chatId: number) => api<void>(`/chats/${chatId}`, { method: 'DELETE' }),                                // #10
  messages: (chatId: number) => api<Message[]>(`/chats/${chatId}/messages`),                                      // #11
  send: (chatId: number, content: string) => api<ChatReply>(`/chats/${chatId}/messages`, json('POST', { content })), // #12
};

// 🔒 Квизы
export const subjectsApi = { list: () => api<Subject[]>('/subjects') };                                         // #13

export const quizzesApi = {
  list: () => api<QuizSummary[]>('/quizzes'),                                                                     // #14
  progress: () => api<QuizProgress[]>('/quizzes/progress'),                                                       // #15
  start: (body: StartQuizRequest) => api<Quiz>('/quizzes', json('POST', body)),                                   // #16, 3–30 с при новом квизе
  get: (quizId: number) => api<Quiz>(`/quizzes/${quizId}`),                                                       // #17
  answer: (quizId: number, body: AnswerRequest) => api<AnswerResult>(`/quizzes/${quizId}/answers`, json('POST', body)), // #18
  finish: (quizId: number) => api<{ score: number }>(`/quizzes/${quizId}/finish`, json('POST')),                  // #19
  startSet: (setId: number) => api<Quiz>(`/quiz-sets/${setId}/start`, json('POST')),                              // #20
  setToFlashcards: (setId: number) => api<Deck>(`/quiz-sets/${setId}/flashcards`, json('POST')),                  // #21
};

// 🔒 Флеш-карты
export const flashcardsApi = {
  list: (due?: boolean) => api<Flashcard[]>(due === undefined ? '/flashcards' : `/flashcards?due=${due}`),        // #22
  due: () => api<Flashcard[]>('/flashcards/due'),                                                                 // #23
  stats: () => api<FlashcardStats>('/flashcards/stats'),                                                          // #24
  review: (exerciseId: number, grade: number) => api<ReviewResult>(`/flashcards/${exerciseId}/review`, json('POST', { grade })), // #25
  generate: (body: StartQuizRequest) => api<Deck>('/flashcards/generate', json('POST', body)),                    // #26
};

// 🔒 🕓 Платформы — в API.md помечены «будет позже». Фронтенд готов: как только бэкенд добавит, заработает.
export const platformsApi = {
  list: () => api<PlatformConnection[]>('/platforms'),
  connect: (platform: PlatformKey, credentials: PlatformCredentials) => api<PlatformConnection>(`/platforms/${platform}`, json('POST', credentials)),
  disconnect: (platform: PlatformKey) => api<void>(`/platforms/${platform}`, { method: 'DELETE' }),
};

// 🔒 Домашка
export const homeworkApi = {
  list: (status?: AssignmentStatus) => api<Assignment[]>(status ? `/homework?status=${status}` : '/homework'),    // #30
  get: (id: number) => api<Assignment>(`/homework/${id}`),                                                        // #31
  update: (id: number, body: UpdateAssignmentRequest) => api<Assignment>(`/homework/${id}`, json('PATCH', body)), // #32
};

// 🔒 Дашборд и история
export const dashboardApi = {
  get: () => api<Dashboard>('/dashboard'),                                                                        // #33
  history: (limit?: number) => api<HistoryEvent[]>(limit ? `/history?limit=${limit}` : '/history'),               // #34
};

// 🔓 Служебное: /actuator/health лежит вне /api/v1 (#27)
export const health = async (): Promise<Health> => {
  const origin = new URL(API_BASE, window.location.origin).origin;
  const res = await fetch(`${origin}/actuator/health`);
  return res.json();
};
