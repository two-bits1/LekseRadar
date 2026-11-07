import { expect, it } from 'vitest';
import { matchesFilter, sortHomework } from './Homework';
import type { Assignment } from '../api/types';

const hw = (status: Assignment['status'], id = 1, dueAt: string | null = null): Assignment =>
  ({ id, source: 'canvas', title: 'T', course: 'C', dueAt, status, url: null, kind: 'assignment', description: '' });

it('фильтр «Levert» показывает и сданные, и оценённые', () => {
  expect(matchesFilter(hw('submitted'), 'submitted')).toBe(true);
  expect(matchesFilter(hw('graded'), 'submitted')).toBe(true);
  expect(matchesFilter(hw('todo'), 'submitted')).toBe(false);
});

it('«Alle» показывает всё, остальные — свой статус', () => {
  expect(matchesFilter(hw('overdue'), 'all')).toBe(true);
  expect(matchesFilter(hw('overdue'), 'overdue')).toBe(true);
  expect(matchesFilter(hw('overdue'), 'todo')).toBe(false);
});

it('сортировка: просроченное, несданное по сроку, без срока, потом сданное (новое сверху)', () => {
  const list = [
    hw('graded', 1, '2026-10-01T10:00:00Z'), hw('todo', 2, null), hw('todo', 3, '2026-11-20T10:00:00Z'),
    hw('submitted', 4, '2026-11-01T10:00:00Z'), hw('overdue', 5, '2026-11-06T10:00:00Z'), hw('todo', 6, '2026-11-09T10:00:00Z'),
  ];
  expect(sortHomework(list).map(a => a.id)).toEqual([5, 6, 3, 2, 4, 1]);
});
