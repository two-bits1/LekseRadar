// Общее состояние шара: «думает», пока ИИ отвечает в чате. Чат пишет, шапка читает.
import { useSyncExternalStore } from 'react';

let thinking = false;
const listeners = new Set<() => void>();

export const get = () => thinking;

export function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => { listeners.delete(listener); };
}

export function setOrbThinking(on: boolean) {
  if (on === thinking) return;
  thinking = on;
  listeners.forEach(l => l());
}

export const useOrbThinking = () => useSyncExternalStore(subscribe, get, get);
