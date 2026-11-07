import { expect, it } from 'vitest';
import { GRADES, correctAnswers, encodeAnswer } from './quizLogic';

it('multiple_choice: ответ — JSON-массив строкой (API.md)', () => {
  expect(encodeAnswer('multiple_choice', ['1/3', '4/12'])).toBe('["1/3","4/12"]');
});

it('single_choice и text: ответ — строка как есть (text без лишних пробелов по краям)', () => {
  expect(encodeAnswer('single_choice', 'teller')).toBe('teller');
  expect(encodeAnswer('text', '  teller ')).toBe('teller');
});

it('правильный ответ multiple_choice приходит JSON-массивом строкой → список', () => {
  expect(correctAnswers('multiple_choice', '["A","C"]')).toEqual(['A', 'C']);
});

it('битый JSON у multiple_choice не роняет страницу — показываем как есть', () => {
  expect(correctAnswers('multiple_choice', 'A, C')).toEqual(['A, C']);
});

it('оценки карточек: 0–2 не помню, 3 с трудом, 4 помню, 5 легко', () => {
  expect(GRADES).toEqual({ again: 1, hard: 3, good: 4, easy: 5 });
});
