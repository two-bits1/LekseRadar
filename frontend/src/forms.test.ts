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

const nbTexts = { offline: 'Ingen kontakt', failed: 'Noe gikk galt', keepServer: false,
  byStatus: { 401: 'Feil e-post eller passord', 409: 'E-posten er i bruk', 502: 'AI svarer ikke' }, invalidField: 'Sjekk feltet' };

it('не по-русски: текст бэкенда заменяется своим по коду ответа', () => {
  expect(asApiError({ status: 409, message: 'Email уже занят', fields: null, timestamp: 't' }, nbTexts).message).toBe('E-posten er i bruk');
  expect(asApiError({ status: 500, message: 'Ошибка', fields: null, timestamp: 't' }, nbTexts).message).toBe('Noe gikk galt');
});

it('не по-русски: ошибки полей тоже заменяются, ключи полей сохраняются', () => {
  const err = { status: 400, message: 'Проверьте данные', fields: { email: 'Некорректный email' }, timestamp: 't' };
  expect(asApiError(err, nbTexts).fields).toEqual({ email: 'Sjekk feltet' });
});

it('сообщение бэкенда показывается как пришло', () => {
  const err = { status: 401, message: 'Feil e-post eller passord', fields: null, timestamp: 't' };
  expect(asApiError(err, texts)).toBe(err);
});
