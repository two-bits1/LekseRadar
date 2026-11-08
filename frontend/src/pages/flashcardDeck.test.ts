import { describe, expect, it } from 'vitest';
import type { Flashcard } from '../api/types';
import { ALL_TOPICS, buildDeck, counts, isHard, isLearned, topics } from './flashcardDeck';

const card = (id: number, p: Partial<Flashcard> = {}): Flashcard => ({
  exerciseId: id, subject: 'Matte', topic: 'Brøk', front: `q${id}`, back: 'a', explanation: null,
  nextReviewAt: `2026-11-0${id}T10:00:00Z`, intervalDays: 0, reviews: 0, lapses: 0, ease: 2.5, due: false, ...p,
});

const cards = [
  card(1, { due: true }),                                               // ny, til repetisjon
  card(2, { reviews: 3, intervalDays: 6, ease: 2.6 }),                  // lært
  card(3, { reviews: 4, intervalDays: 1, ease: 2.36, lapses: 1, due: true }), // lært, men vanskelig
  card(4, { reviews: 2, intervalDays: 0, ease: 1.96, due: true, topic: 'Algebra' }), // vanskelig
];

describe('выбор карточек', () => {
  it('выученные и трудные', () => {
    expect(cards.filter(isLearned).map(c => c.exerciseId)).toEqual([2, 3]);
    expect(cards.filter(isHard).map(c => c.exerciseId)).toEqual([3, 4]);
  });

  it('счётчики по режимам и теме', () => {
    expect(counts(cards, ALL_TOPICS)).toEqual({ due: 3, hard: 2, learned: 2, new: 1, all: 4 });
    expect(counts(cards, 'Matte · Algebra')).toEqual({ due: 1, hard: 1, learned: 0, new: 0, all: 1 });
  });

  it('трудные: самая низкая лёгкость первой', () => {
    expect(buildDeck(cards, 'hard', ALL_TOPICS).map(c => c.exerciseId)).toEqual([4, 3]);
  });

  it('выученные: сначала короткий интервал', () => {
    expect(buildDeck(cards, 'learned', ALL_TOPICS).map(c => c.exerciseId)).toEqual([3, 2]);
  });

  it('тема фильтрует колоду', () => {
    expect(buildDeck(cards, 'all', 'Matte · Brøk').map(c => c.exerciseId)).toEqual([1, 2, 3]);
  });

  it('перемешивание не теряет карточки', () => {
    const d = buildDeck(cards, 'all', ALL_TOPICS, true, () => 0);
    expect(d.map(c => c.exerciseId).sort()).toEqual([1, 2, 3, 4]);
    expect(d.map(c => c.exerciseId)).not.toEqual([1, 2, 3, 4]);
  });

  it('темы с числом карточек', () => {
    expect(topics(cards)).toEqual([{ key: 'Matte · Algebra', count: 1 }, { key: 'Matte · Brøk', count: 3 }]);
  });
});
