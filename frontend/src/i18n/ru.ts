import type { Dict } from './nb';

export const ru: Dict = {
  nav: { main: 'Главное меню', calendar: 'Календарь', flashcards: 'Карточки', tutor: 'ИИ-наставник', profile: 'Профиль', settings: 'Настройки' },
  header: { home: 'LekseRadar — обзор', login: 'Войти', skip: 'Перейти к содержимому' },
  demoBar: { text: 'Немного о том, какой может быть учёба без лишнего стресса.', link: 'Посмотреть демо' },
  footer: { tagline: 'Учись понемногу. Понимай больше.', status: 'Прототип · Статус разработки' },
  settings: {
    eyebrow: 'ТВОЙ МАЛЕНЬКИЙ УГОЛОК', title: 'Всё так, как тебе удобно.', intro: 'Настрой LekseRadar под себя.',
    appearance: 'Вид и язык', theme: 'Тема',
    themeHint: (from: number, to: string) => `Авто: тёмная с ${from}:00 до ${to}:00 или когда на устройстве включён тёмный режим`,
    auto: 'Авто', light: 'Светлая', dark: 'Тёмная',
    language: 'Язык', languageHint: 'Для всего сайта и ИИ-наставника',
    reminders: 'Напоминания', remindersHint: 'Уведомления пока недоступны', soon: 'Скоро',
    platformsTitle: 'Всё в одном месте. Скоро.',
    platformsIntro: 'Подключение школьных платформ в разработке. Не вставляй в демо настоящие токены доступа.',
    seeSetup: 'Настройка', notYet: 'Пока недоступно',
    preview: 'Это предпросмотр. Никакие данные не отправляются и не сохраняются.',
    calendarUrl: 'Адрес календаря', schoolUrl: 'Адрес школы', connectLater: 'Подключение — позже',
    account: 'Аккаунт', loggedInAs: (email: string) => `Вход выполнен: ${email}.`, logout: 'Выйти',
  },
};
