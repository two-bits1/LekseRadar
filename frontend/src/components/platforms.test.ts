import { expect, it } from 'vitest';
import { cardState, classroomMissing, missingFields, oauthReturn, toCredentials } from './platformLogic';

const conn = (over: object = {}) => ({ platform: 'canvas' as const, connected: false, needsReconnect: false, lastSyncedAt: null, lastError: null, ...over });

it('Canvas: base_url + token, пробелы по краям убираются', () => {
  expect(toCredentials('canvas', { base_url: ' https://skole.instructure.com ', token: 'abc ' })).toEqual({ base_url: 'https://skole.instructure.com', token: 'abc' });
});

it('InSchool: cookies внутри, пустой Authorization не уходит', () => {
  expect(toCredentials('inschool', { host: 'skole.inschool.visma.no', JSESSIONID: 'j', Authorization: '  ' }))
    .toEqual({ host: 'skole.inschool.visma.no', cookies: { JSESSIONID: 'j' } });
  expect(toCredentials('inschool', { host: 'h', JSESSIONID: 'j', Authorization: 'a' })).toEqual({ host: 'h', cookies: { JSESSIONID: 'j', Authorization: 'a' } });
});

it('Teams: хватает одного refresh_token, пустые поля не уходят', () => {
  expect(toCredentials('teams', { access_token: '', refresh_token: 'y' })).toEqual({ refresh_token: 'y' });
});

it('обязательные поля: что серверу хватает', () => {
  expect(missingFields('canvas', { base_url: 'u' })).toEqual(['token']);
  expect(missingFields('inschool', { host: 'h', JSESSIONID: 'j' })).toEqual([]);
  expect(missingFields('itslearning', { refresh_token: 'r' })).toEqual(['base_url']);
  expect(missingFields('teams', {})).toEqual(['refresh_token']);
  expect(missingFields('teams', { access_token: 'a' })).toEqual([]);
});

it('состояние карточки', () => {
  expect(cardState(undefined)).toBe('off');
  expect(cardState(conn())).toBe('off');
  expect(cardState(conn({ connected: true }))).toBe('on');
  expect(cardState(conn({ connected: true, needsReconnect: true, lastError: 'x' }))).toBe('reconnect');
  expect(cardState(conn({ connected: true, lastError: 'timeout' }))).toBe('error');
});

it('возврат из Google: code и state из адреса, иначе null', () => {
  expect(oauthReturn('?code=4%2Fabc&state=s1&scope=x')).toEqual({ code: '4/abc', state: 's1' });
  expect(oauthReturn('?error=access_denied&state=s1')).toEqual({ error: 'access_denied' });
  expect(oauthReturn('')).toBeNull();
});

it('Classroom «ещё не поддерживается» только на 404/405/501; 400 — настоящая ошибка', () => {
  expect(classroomMissing(404)).toBe(true);
  expect(classroomMissing(405)).toBe(true);
  expect(classroomMissing(501)).toBe(true);
  expect(classroomMissing(400)).toBe(false);
  expect(classroomMissing(422)).toBe(false);
});
