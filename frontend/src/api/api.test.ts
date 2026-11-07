import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { authApi, chatsApi, dashboardApi, flashcardsApi, health, homeworkApi, platformsApi, quizzesApi, subjectsApi, usersApi } from './endpoints';
import { api, getToken, setToken } from './client';

// Тесты идут в node: подменяем localStorage, window.location и fetch.
const store = new Map<string, string>();
const assign = vi.fn();
beforeEach(() => {
  store.clear(); assign.mockReset();
  vi.stubGlobal('localStorage', {
    getItem: (k: string) => store.get(k) ?? null,
    setItem: (k: string, v: string) => { store.set(k, v); },
    removeItem: (k: string) => { store.delete(k); },
  });
  vi.stubGlobal('window', { location: { origin: 'http://localhost:5173', assign } });
});
afterEach(() => { vi.unstubAllGlobals(); });

const mockFetch = (status: number, body?: unknown) => {
  const fetch = vi.fn(async (_url: string, _init?: RequestInit) =>
    new Response(body === undefined ? null : typeof body === 'string' ? body : JSON.stringify(body), { status }));
  vi.stubGlobal('fetch', fetch);
  return fetch;
};

// Контракт с бэкендом: таблица «Все URL бэкенда» в docs/API.md.
describe('URL бэкенда из API.md', () => {
  const cases: [string, () => Promise<unknown>, string, string, unknown?][] = [
    ['#1 регистрация', () => authApi.register({ email: 'ann@example.com', password: 'secret123', displayName: 'Ann' }), 'POST', '/api/v1/auth/register', { email: 'ann@example.com', password: 'secret123', displayName: 'Ann' }],
    ['#2 вход', () => authApi.login({ email: 'demo@lekseradar.no', password: 'demo1234' }), 'POST', '/api/v1/auth/login', { email: 'demo@lekseradar.no', password: 'demo1234' }],
    ['#44 код из письма', () => authApi.verifyEmail({ email: 'ann@example.com', code: '123456' }), 'POST', '/api/v1/auth/register/verify', { email: 'ann@example.com', code: '123456' }],
    ['#3 мой профиль', () => usersApi.me(), 'GET', '/api/v1/users/me'],
    ['#4 изменить профиль', () => usersApi.update({ locale: 'en' }), 'PATCH', '/api/v1/users/me', { locale: 'en' }],
    ['#5 сменить пароль', () => usersApi.changePassword({ currentPassword: 'a', newPassword: 'newSecret123' }), 'PUT', '/api/v1/users/me/password', { currentPassword: 'a', newPassword: 'newSecret123' }],
    ['#6 все чаты', () => chatsApi.list(), 'GET', '/api/v1/chats'],
    ['#6 активные чаты', () => chatsApi.list(false), 'GET', '/api/v1/chats?archived=false'],
    ['#6 архив', () => chatsApi.list(true), 'GET', '/api/v1/chats?archived=true'],
    ['#7 новый чат', () => chatsApi.create({ title: 'Algebra' }), 'POST', '/api/v1/chats', { title: 'Algebra' }],
    ['#8 один чат', () => chatsApi.get(7), 'GET', '/api/v1/chats/7'],
    ['#9 изменить чат', () => chatsApi.update(7, { archived: true }), 'PATCH', '/api/v1/chats/7', { archived: true }],
    ['#10 удалить чат', () => chatsApi.remove(7), 'DELETE', '/api/v1/chats/7'],
    ['#11 история', () => chatsApi.messages(7), 'GET', '/api/v1/chats/7/messages'],
    ['#12 вопрос ИИ', () => chatsApi.send(7, 'Hei'), 'POST', '/api/v1/chats/7/messages', { content: 'Hei' }],
    ['#7 квиз-чат', () => chatsApi.create({ mode: 'quiz', subject: 'Matematikk', level: 'beginner' }), 'POST', '/api/v1/chats', { mode: 'quiz', subject: 'Matematikk', level: 'beginner' }],
    ['#13 предметы', () => subjectsApi.list(), 'GET', '/api/v1/subjects'],
    ['#14 мои квизы', () => quizzesApi.list(), 'GET', '/api/v1/quizzes'],
    ['#15 прогресс', () => quizzesApi.progress(), 'GET', '/api/v1/quizzes/progress'],
    ['#16 начать квиз', () => quizzesApi.start({ subject: 'Matematikk', topic: 'Brøk', level: 'beginner', count: 8 }), 'POST', '/api/v1/quizzes', { subject: 'Matematikk', topic: 'Brøk', level: 'beginner', count: 8 }],
    ['#17 квиз', () => quizzesApi.get(5), 'GET', '/api/v1/quizzes/5'],
    ['#18 ответ', () => quizzesApi.answer(5, { position: 1, answer: 'teller' }), 'POST', '/api/v1/quizzes/5/answers', { position: 1, answer: 'teller' }],
    ['#19 завершить', () => quizzesApi.finish(5), 'POST', '/api/v1/quizzes/5/finish'],
    ['#20 квиз по набору', () => quizzesApi.startSet(9), 'POST', '/api/v1/quiz-sets/9/start'],
    ['#21 квиз в колоду', () => quizzesApi.setToFlashcards(9), 'POST', '/api/v1/quiz-sets/9/flashcards'],
    ['#22 колода', () => flashcardsApi.list(), 'GET', '/api/v1/flashcards'],
    ['#22 колода due', () => flashcardsApi.list(true), 'GET', '/api/v1/flashcards?due=true'],
    ['#23 к повторению', () => flashcardsApi.due(), 'GET', '/api/v1/flashcards/due'],
    ['#24 статистика', () => flashcardsApi.stats(), 'GET', '/api/v1/flashcards/stats'],
    ['#25 оценка', () => flashcardsApi.review(11, 4), 'POST', '/api/v1/flashcards/11/review', { grade: 4 }],
    ['🕓 платформы', () => platformsApi.list(), 'GET', '/api/v1/platforms'],
    ['🕓 подключить Canvas', () => platformsApi.connect('canvas', { base_url: 'https://skole.instructure.com', token: 'abc' }), 'POST', '/api/v1/platforms/canvas', { base_url: 'https://skole.instructure.com', token: 'abc' }],
    ['🕓 отключить', () => platformsApi.disconnect('teams'), 'DELETE', '/api/v1/platforms/teams'],
    ['#26 карточки от ИИ', () => flashcardsApi.generate({ subject: 'Matematikk', topic: 'Brøk' }), 'POST', '/api/v1/flashcards/generate', { subject: 'Matematikk', topic: 'Brøk' }],
    ['#30 мои ДЗ', () => homeworkApi.list(), 'GET', '/api/v1/homework'],
    ['#30 просроченные ДЗ', () => homeworkApi.list('overdue'), 'GET', '/api/v1/homework?status=overdue'],
    ['#31 одно ДЗ', () => homeworkApi.get(3), 'GET', '/api/v1/homework/3'],
    ['#32 ДЗ сдано', () => homeworkApi.update(3, { status: 'submitted' }), 'PATCH', '/api/v1/homework/3', { status: 'submitted' }],
    ['#33 дашборд', () => dashboardApi.get(), 'GET', '/api/v1/dashboard'],
    ['#34 история', () => dashboardApi.history(), 'GET', '/api/v1/history'],
    ['#34 история limit', () => dashboardApi.history(20), 'GET', '/api/v1/history?limit=20'],
  ];
  it.each(cases)('%s', async (_name, call, method, url, body) => {
    const fetch = mockFetch(200, {});
    await call();
    const [calledUrl, init] = fetch.mock.calls[0];
    expect(calledUrl).toBe(url);
    expect(init?.method ?? 'GET').toBe(method);
    expect(init?.body ? JSON.parse(init.body as string) : undefined).toEqual(body);
  });

  it('#13 health — вне /api/v1', async () => {
    const fetch = mockFetch(200, { status: 'UP' });
    await expect(health()).resolves.toEqual({ status: 'UP' });
    expect(fetch.mock.calls[0][0]).toBe('http://localhost:5173/actuator/health');
  });
});

describe('клиент API', () => {
  it('шлёт Bearer-токен, если он есть', async () => {
    setToken('abc');
    const fetch = mockFetch(200, {});
    await usersApi.me();
    expect((fetch.mock.calls[0][1]?.headers as Record<string, string>).Authorization).toBe('Bearer abc');
  });

  it('без токена заголовка Authorization нет', async () => {
    const fetch = mockFetch(200, {});
    await usersApi.me();
    expect(fetch.mock.calls[0][1]?.headers).not.toHaveProperty('Authorization');
  });

  it('204 → undefined', async () => {
    mockFetch(204);
    await expect(chatsApi.remove(1)).resolves.toBeUndefined();
  });

  it('401 на защищённом запросе → токен удалён, переход на /login', async () => {
    setToken('old');
    mockFetch(401, { status: 401, message: 'Ikke innlogget', fields: null, timestamp: 't' });
    await expect(usersApi.me()).rejects.toMatchObject({ status: 401 });
    expect(getToken()).toBeNull();
    expect(assign).toHaveBeenCalledWith('/login');
  });

  it('401 на /auth/login (неверный пароль) → без редиректа, ошибка для формы', async () => {
    mockFetch(401, { status: 401, message: 'Feil e-post eller passord', fields: null, timestamp: 't' });
    await expect(authApi.login({ email: 'a@b.no', password: 'x' })).rejects.toMatchObject({ status: 401, message: 'Feil e-post eller passord' });
    expect(assign).not.toHaveBeenCalled();
  });

  it('400 с fields передаётся как есть', async () => {
    const error = { status: 400, message: 'Sjekk feltene', fields: { email: 'Ugyldig e-post' }, timestamp: 't' };
    mockFetch(400, error);
    await expect(authApi.register({ email: 'x', password: 'y', displayName: '' })).rejects.toEqual(error);
  });

  it('ответ не JSON (бэкенд выключен, proxy вернул 502) → понятная ApiError', async () => {
    mockFetch(502, 'Bad Gateway');
    await expect(api('/chats')).rejects.toMatchObject({ status: 502, message: 'Noe gikk galt. Prøv igjen.', fields: null });
  });
});
