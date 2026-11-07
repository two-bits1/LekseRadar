// Общие куски форм: поле с ошибкой под ним и приведение любой ошибки к ApiError (формат из API.md).
import type { InputHTMLAttributes } from 'react';
import { FALLBACK_MESSAGE, isApiError } from './api/client';
import type { ApiError } from './api/types';

type ErrorTexts = { offline: string; failed: string };
const NB_TEXTS: ErrorTexts = { offline: 'Får ikke kontakt med serveren. Prøv igjen.', failed: FALLBACK_MESSAGE };

// Нет связи → status 0 и текст «нет связи»; ответ без JSON → «что-то пошло не так» на языке интерфейса.
// Тексты бэкенда (message из ApiError) показываются как пришли.
export const asApiError = (err: unknown, texts: ErrorTexts = NB_TEXTS): ApiError => {
  if (!isApiError(err)) return { status: 0, message: texts.offline, fields: null, timestamp: new Date().toISOString() };
  return err.message === FALLBACK_MESSAGE ? { ...err, message: texts.failed } : err;
};

// 400 → ошибка из fields; иначе undefined (общий message показывается отдельно).
export const fieldError = (error: ApiError | null, name: string) => error?.fields?.[name];

// Общее сообщение: когда ошибки не разложены по полям.
export const generalError = (error: ApiError | null) => error && !error.fields ? error.message : null;

type FieldProps = { name: string; label: string; error?: string } & InputHTMLAttributes<HTMLInputElement>;

export function Field({ name, label, error, ...input }: FieldProps) {
  return <label className="field">{label}
    <input name={name} {...input} aria-invalid={!!error} aria-describedby={error ? `${name}-error` : undefined} />
    {error && <span className="field-error" id={`${name}-error`}>{error}</span>}
  </label>;
}
