import { expect, it } from 'vitest';
import { tier } from './ResultCard';

it('итог: вывод по проценту', () => {
  expect([tier(100), tier(90), tier(89), tier(60), tier(59), tier(0)]).toEqual(['great', 'great', 'good', 'good', 'keep', 'keep']);
});
