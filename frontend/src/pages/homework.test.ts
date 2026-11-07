import { expect, it } from 'vitest';
import { matchesFilter } from './Homework';
import type { Assignment } from '../api/types';

const hw = (status: Assignment['status']): Assignment =>
  ({ id: 1, source: 'canvas', title: 'T', course: 'C', dueAt: null, status, url: null, kind: 'assignment', description: '' });

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
