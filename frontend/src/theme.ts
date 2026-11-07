// Тема сайта: 'auto' включает тёмную тему вечером и ночью или когда устройство в тёмном режиме.
// Подсвеченный экран перед сном подавляет мелатонин (Chang et al., PNAS 2015) — см. COLORS.md.
export type ThemeChoice = 'light' | 'dark' | 'auto';
export type Theme = 'light' | 'dark';

export const EVENING_START = 20; // с 20:00…
export const MORNING_END = 7;    // …до 07:00 — тёмная тема в режиме auto

const STORAGE_KEY = 'lekseradar-theme';
const THEME_COLOR: Record<Theme, string> = { light: '#faf8f3', dark: '#1e2124' };

export const isEvening = (date: Date) => { const h = date.getHours(); return h >= EVENING_START || h < MORNING_END; };

export const resolveTheme = (choice: ThemeChoice, systemDark: boolean, date: Date): Theme =>
  choice === 'auto' ? (systemDark || isEvening(date) ? 'dark' : 'light') : choice;

export function loadThemeChoice(): ThemeChoice {
  try { const v = localStorage.getItem(STORAGE_KEY); if (v === 'light' || v === 'dark' || v === 'auto') return v; } catch {}
  return 'auto';
}

export function saveThemeChoice(choice: ThemeChoice) {
  try { localStorage.setItem(STORAGE_KEY, choice); } catch {}
}

export const themeColor = (theme: Theme) => THEME_COLOR[theme];
