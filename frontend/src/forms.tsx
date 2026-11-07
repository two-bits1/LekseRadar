// Общие куски форм: поле с ошибкой под ним и приведение любой ошибки к ApiError (формат из API.md).
import type { InputHTMLAttributes } from 'react';
import { FALLBACK_MESSAGE, isApiError } from './api/client';
import type { ApiError } from './api/types';

// keepServer: показывать текст бэкенда как есть (бэкенд пишет по-русски — это для языка ru).
// Иначе текст берётся по коду ответа (byStatus), ошибки полей — общим invalidField.
type ErrorTexts = { offline: string; failed: string; keepServer?: boolean; byStatus?: Partial<Record<number, string>>; invalidField?: string };
const NB_TEXTS: ErrorTexts = { offline: 'Får ikke kontakt med serveren. Prøv igjen.', failed: FALLBACK_MESSAGE, keepServer: true };

export const asApiError = (err: unknown, texts: ErrorTexts = NB_TEXTS): ApiError => {
  if (!isApiError(err)) return { status: 0, message: texts.offline, fields: null, timestamp: new Date().toISOString() };
  if (err.message === FALLBACK_MESSAGE) return { ...err, message: texts.failed };
  if (texts.keepServer !== false) return err;
  const fields = err.fields && Object.fromEntries(Object.keys(err.fields).map(k => [k, texts.invalidField ?? texts.failed]));
  return { ...err, message: texts.byStatus?.[err.status] ?? texts.failed, fields };
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
