import { PATHS } from './paths';
// Сайт-карта для /status — те же адреса, что в API.md (см. src/paths.ts).
export const routes = [
  [PATHS.home,'home','api'], [PATHS.homework,'homework','later'], [PATHS.chats,'tutor','api'], [PATHS.chat(':chatId'),'chat','api'],
  [PATHS.quizzes,'quiz','api'], [PATHS.flashcards,'flashcards','api'], [PATHS.progress,'progress','later'], [PATHS.history,'history','later'],
  [PATHS.profile,'profile','api'], [PATHS.settings,'settings','partial'], [PATHS.platforms,'platforms','later'], [PATHS.login,'login','api'], [PATHS.register,'register','api'],
] as const;  // [адрес, ключ названия в t.status.pages, ключ статуса в t.status.states]
