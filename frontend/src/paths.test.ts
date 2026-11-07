import { expect, it } from 'vitest';
import { PATHS } from './paths';

// Контракт с бэкендом: таблица «Страницы фронтенда» в docs/API.md.
it('адреса страниц совпадают с API.md', () => {
  expect([
    PATHS.home, PATHS.login, PATHS.register, PATHS.profile, PATHS.chats, PATHS.chat(':chatId'),
    PATHS.homework, PATHS.quizzes, PATHS.quiz(':quizId'), PATHS.flashcards, PATHS.progress, PATHS.history, PATHS.platforms,
  ]).toEqual([
    '/', '/login', '/register', '/profile', '/chat', '/chat/:chatId',
    '/homework', '/quiz', '/quiz/:quizId', '/flashcards', '/progress', '/history', '/settings/platforms',
  ]);
});
