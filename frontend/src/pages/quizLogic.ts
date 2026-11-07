// Правила из API.md для квиза и карточек — чистые функции, чтобы их можно было тестировать.
import type { Question } from '../api/types';

// multiple_choice → JSON-массив строкой: '["1/3","4/12"]'; single_choice и text → строка.
export const encodeAnswer = (type: Question['type'], value: string | string[]) =>
  type === 'multiple_choice' ? JSON.stringify(value) : String(value).trim();

// correctAnswer у multiple_choice приходит JSON-массивом строкой; если формат сломан — показываем как есть.
export function correctAnswers(type: Question['type'], correct: string): string[] {
  if (type !== 'multiple_choice') return [correct];
  try { const v = JSON.parse(correct); return Array.isArray(v) ? v.map(String) : [correct]; } catch { return [correct]; }
}

// Оценка карточки 0–5 (SM-2): 0–2 — не помню, 3 — с трудом, 4 — помню, 5 — легко.
export const GRADES = { again: 1, hard: 3, good: 4, easy: 5 } as const;
