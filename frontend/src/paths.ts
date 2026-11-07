// Адреса страниц фронтенда — как в таблице «Страницы фронтенда» в API.md бэкенда.
// Меняйте URL только здесь и только вместе с бэкендом.
export const PATHS = {
  home: '/',
  login: '/login',
  register: '/register',
  profile: '/profile',
  chats: '/chat',
  chat: (chatId: number | string) => `/chat/${chatId}`,
  homework: '/homework',
  quizzes: '/quiz',
  quiz: (quizId: number | string) => `/quiz/${quizId}`,
  flashcards: '/flashcards',
  progress: '/progress',
  history: '/history',
  platforms: '/settings/platforms',
  // Только фронтенд, в API.md их нет:
  settings: '/settings',
  status: '/status',
} as const;

// Демо-ссылки, пока задания не приходят с бэкенда (GET /homework 🕓).
export const chatAbout = (assignmentId: string) => `${PATHS.chats}?assignment=${assignmentId}`;
export const homeworkItem = (assignmentId: string) => `${PATHS.homework}?assignment=${assignmentId}`;
