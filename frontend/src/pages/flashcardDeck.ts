// Выбор карточек для тренировки: какие карточки показать и в каком порядке (страница Flashcards).
import type { Flashcard } from '../api/types';

export type DeckMode = 'due' | 'hard' | 'learned' | 'new' | 'all';
export const DECK_MODES: DeckMode[] = ['due', 'hard', 'learned', 'new', 'all'];
export const ALL_TOPICS = '';

const START_EASE = 2.5;
/** Выучена: последний ответ «помню» (интервал от 1 дня) — так же считает «выучено» в статистике (V24). */
export const isLearned = (c: Flashcard) => (c.intervalDays ?? 0) >= 1;
/** Трудная: хоть раз забыта после того, как помнили, или лёгкость SM-2 упала («Трудно», «Не помню»). */
export const isHard = (c: Flashcard) => (c.lapses ?? 0) > 0 || (c.ease ?? START_EASE) < START_EASE - 1e-6;
export const isNew = (c: Flashcard) => (c.reviews ?? 0) === 0;
export const isDue = (c: Flashcard) => c.due ?? new Date(c.nextReviewAt).getTime() <= Date.now();

const MATCH: Record<DeckMode, (c: Flashcard) => boolean> = {
  due: isDue, hard: isHard, learned: isLearned, new: isNew, all: () => true,
};

/** Ключ темы: предмет и тема, как в подписи над карточкой. */
export const topicKey = (c: Flashcard) => [c.subject, c.topic].filter(Boolean).join(' · ');

/** Темы с числом карточек, по алфавиту. */
export function topics(cards: Flashcard[]): { key: string; count: number }[] {
  const counts = new Map<string, number>();
  for (const c of cards) counts.set(topicKey(c), (counts.get(topicKey(c)) ?? 0) + 1);
  return [...counts].map(([key, count]) => ({ key, count })).sort((a, b) => a.key.localeCompare(b.key));
}

/** Сколько карточек в каждом режиме (в выбранной теме). */
export function counts(cards: Flashcard[], topic: string): Record<DeckMode, number> {
  const inTopic = cards.filter(c => topic === ALL_TOPICS || topicKey(c) === topic);
  return Object.fromEntries(DECK_MODES.map(m => [m, inTopic.filter(MATCH[m]).length])) as Record<DeckMode, number>;
}

/**
 * Колода для занятия. Порядок: «к повторению» и «все» — по сроку повторения; «трудные» — самые трудные первыми
 * (меньше лёгкость, больше ошибок); «выученные» — сначала с самым коротким интервалом (их легче забыть).
 * shuffle — перемешать (random для тестов можно подменить).
 */
export function buildDeck(cards: Flashcard[], mode: DeckMode, topic: string, shuffle = false, random = Math.random): Flashcard[] {
  const deck = cards.filter(c => (topic === ALL_TOPICS || topicKey(c) === topic) && MATCH[mode](c));
  const byDate = (a: Flashcard, b: Flashcard) => new Date(a.nextReviewAt).getTime() - new Date(b.nextReviewAt).getTime();
  if (mode === 'hard') deck.sort((a, b) => (a.ease ?? START_EASE) - (b.ease ?? START_EASE) || (b.lapses ?? 0) - (a.lapses ?? 0) || byDate(a, b));
  else if (mode === 'learned') deck.sort((a, b) => (a.intervalDays ?? 0) - (b.intervalDays ?? 0) || byDate(a, b));
  else deck.sort(byDate);
  if (shuffle) for (let i = deck.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1)); [deck[i], deck[j]] = [deck[j], deck[i]];
  }
  return deck;
}
