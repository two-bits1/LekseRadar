import { afterEach, expect, it, vi } from 'vitest';
import { get, setOrbThinking, subscribe } from './orbState';

afterEach(() => setOrbThinking(false));

it('по умолчанию шар не думает', () => {
  expect(get()).toBe(false);
});

it('setOrbThinking(true) меняет состояние и один раз зовёт подписчика', () => {
  const listener = vi.fn();
  const unsubscribe = subscribe(listener);
  setOrbThinking(true);
  expect(get()).toBe(true);
  expect(listener).toHaveBeenCalledTimes(1);
  unsubscribe();
});

it('повторное то же значение подписчика не зовёт', () => {
  setOrbThinking(true);
  const listener = vi.fn();
  const unsubscribe = subscribe(listener);
  setOrbThinking(true);
  expect(listener).not.toHaveBeenCalled();
  unsubscribe();
});

it('после отписки подписчик больше не вызывается', () => {
  const listener = vi.fn();
  subscribe(listener)();
  setOrbThinking(true);
  expect(listener).not.toHaveBeenCalled();
});
