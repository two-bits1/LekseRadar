// Общие куски форм: поле с ошибкой под ним и приведение любой ошибки к ApiError (формат из API.md).
import type { InputHTMLAttributes } from 'react';
import { isApiError } from './api/client';
import type { ApiError, Locale } from './api/types';

export const asApiError = (err: unknown): ApiError => isApiError(err) ? err
  : { status: 0, message: 'Får ikke kontakt med serveren. Prøv igjen.', fields: null, timestamp: new Date().toISOString() };

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

export const LOCALES: Record<Locale, string> = { nb: 'Norsk bokmål', nn: 'Norsk nynorsk', en: 'English', ru: 'Русский' };
