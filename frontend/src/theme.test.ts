import { describe, expect, it } from 'vitest';
import { resolveTheme } from './theme';

const at = (hour: number) => new Date(2026, 10, 7, hour, 30);

describe('resolveTheme', () => {
  it.each([[6, 'dark'], [7, 'light'], [12, 'light'], [19, 'light'], [20, 'dark'], [23, 'dark'], [0, 'dark']] as const)(
    'auto в %i:30 → %s', (hour, expected) => {
      expect(resolveTheme('auto', false, at(hour))).toBe(expected);
    });

  it('auto днём, но устройство в тёмном режиме → dark', () => {
    expect(resolveTheme('auto', true, at(12))).toBe('dark');
  });

  it('ручной выбор важнее времени и режима устройства', () => {
    expect(resolveTheme('light', true, at(22))).toBe('light');
    expect(resolveTheme('dark', false, at(12))).toBe('dark');
  });
});
