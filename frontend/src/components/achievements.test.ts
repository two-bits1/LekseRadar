import { expect, it } from 'vitest';
import { achievements, bestQuizScore } from './achievements';
import type { Dashboard, QuizProgress } from '../api/types';

const dash = (over: Partial<Dashboard> = {}): Dashboard => ({
  homeworkTodo: 0, homeworkOverdue: 0, homeworkDone: 0, quizzesCompleted: 0, avgQuizScore: null, exercisesAnswered: 0,
  questionsAsked: 0, activeDays30: 0, flashcards: { cards: 0, dueNow: 0, learned: 0, reviews: 0 }, topics: [], days: [], ...over,
});
const byId = (list: ReturnType<typeof achievements>) => Object.fromEntries(list.map(a => [a.id, a]));

it('новый ученик: ни одной ачивки, у всех прогресс 0', () => {
  const list = achievements(dash(), 0);
  expect(list).toHaveLength(12);
  expect(list.every(a => !a.done && a.value === 0)).toBe(true);
});

it('первые шаги открываются с первого действия', () => {
  const a = byId(achievements(dash({ activeDays30: 1, questionsAsked: 1, quizzesCompleted: 1, homeworkDone: 1,
    flashcards: { cards: 3, dueNow: 0, learned: 0, reviews: 2 } }), 40));
  for (const id of ['firstDay', 'firstQuestion', 'firstQuiz', 'firstReview', 'firstHomework']) expect(a[id].done).toBe(true);
  expect(a.weekRhythm).toMatchObject({ done: false, value: 1, target: 7 });
  expect(a.perfect).toMatchObject({ done: false, value: 40 });
});

it('прогресс не больше цели, полученные идут первыми', () => {
  const list = achievements(dash({ exercisesAnswered: 120, questionsAsked: 4 }), 100);
  const a = byId(list);
  expect(a.answersFifty).toMatchObject({ done: true, value: 50 });
  expect(a.perfect.done).toBe(true);
  expect(list.slice(0, 3).map(x => x.id)).toEqual(['firstQuestion', 'perfect', 'answersFifty']);
});

it('«без долгов»: просроченное задание обнуляет прогресс', () => {
  expect(byId(achievements(dash({ homeworkDone: 5 }), 0)).noDebts.done).toBe(true);
  expect(byId(achievements(dash({ homeworkDone: 5, homeworkOverdue: 1 }), 0)).noDebts).toMatchObject({ done: false, value: 0 });
});

it('лучший результат: незаконченные квизы (null) не считаются', () => {
  const p = (bestScore: number | null) => ({ subject: null, topic: 't', quizzesCompleted: 1, quizzesInProgress: 0, bestScore, lastScore: null, lastActivity: '' }) as QuizProgress;
  expect(bestQuizScore([p(null), p(80), p(65)])).toBe(80);
  expect(bestQuizScore(null)).toBe(0);
});
