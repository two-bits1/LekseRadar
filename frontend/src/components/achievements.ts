// Ачивки профиля: считаются на фронте из GET /dashboard и лучшего результата из GET /quizzes/progress.
// Простые цели по прогрессу в учёбе; бэкенд для них не нужен.
import type { Dashboard, QuizProgress } from '../api/types';

export const ACHIEVEMENT_IDS = [
  'firstDay', 'firstQuestion', 'firstQuiz', 'firstReview', 'firstHomework', 'weekRhythm',
  'curious', 'quizFive', 'perfect', 'learnedTen', 'answersFifty', 'noDebts',
] as const;
export type AchievementId = typeof ACHIEVEMENT_IDS[number];
export type Achievement = { id: AchievementId; value: number; target: number; done: boolean };

export const bestQuizScore = (progress: QuizProgress[] | null | undefined): number =>
  Math.max(0, ...(progress ?? []).map(p => p.bestScore ?? 0));

// Порядок: сначала полученные, потом те, до которых ближе всего (по доле пути).
export function achievements(d: Dashboard, best: number): Achievement[] {
  const goals: Record<AchievementId, [number, number]> = {
    firstDay: [d.activeDays30, 1],
    firstQuestion: [d.questionsAsked, 1],
    firstQuiz: [d.quizzesCompleted, 1],
    firstReview: [d.flashcards.reviews, 1],
    firstHomework: [d.homeworkDone, 1],
    weekRhythm: [d.activeDays30, 7],
    curious: [d.questionsAsked, 10],
    quizFive: [d.quizzesCompleted, 5],
    perfect: [Math.round(best), 100],
    learnedTen: [d.flashcards.learned, 10],
    answersFifty: [d.exercisesAnswered, 50],
    noDebts: [d.homeworkOverdue > 0 ? 0 : d.homeworkDone, 3],  // 3 сданных и ни одной просроченной
  };
  const list = ACHIEVEMENT_IDS.map(id => {
    const [raw, target] = goals[id];
    const value = Math.max(0, Math.min(raw, target));
    return { id, value, target, done: raw >= target };
  });
  const share = (a: Achievement) => (a.done ? 2 : a.value / a.target);
  return list.map((a, i) => ({ a, i })).sort((x, y) => share(y.a) - share(x.a) || x.i - y.i).map(x => x.a);
}
