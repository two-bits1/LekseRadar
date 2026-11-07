import { expect, it } from 'vitest';
import { asApiError } from './forms';
import { FALLBACK_MESSAGE } from './api/client';

const texts = { offline: 'Нет связи', failed: 'Что-то пошло не так' };

it('не ApiError (fetch упал) → status 0 и текст «нет связи» на языке интерфейса', () => {
  expect(asApiError(new TypeError('Failed to fetch'), texts)).toMatchObject({ status: 0, message: 'Нет связи', fields: null });
});

it('ответ без JSON (общий текст клиента) → «что-то пошло не так» на языке интерфейса', () => {
  const err = { status: 502, message: FALLBACK_MESSAGE, fields: null, timestamp: 't' };
  expect(asApiError(err, texts)).toEqual({ ...err, message: 'Что-то пошло не так' });
});

it('сообщение бэкенда показывается как пришло', () => {
  const err = { status: 401, message: 'Feil e-post eller passord', fields: null, timestamp: 't' };
  expect(asApiError(err, texts)).toBe(err);
});
