import { expect, it } from 'vitest';
import { toCredentials } from './Platforms';

it('Canvas: base_url + token, пробелы по краям убираются (формат homework-collector)', () => {
  expect(toCredentials('canvas', { base_url: ' https://skole.instructure.com ', token: 'abc ' })).toEqual({ base_url: 'https://skole.instructure.com', token: 'abc' });
});

it('InSchool: host и cookies внутри (как в README homework-collector)', () => {
  expect(toCredentials('inschool', { host: 'skole.inschool.visma.no', JSESSIONID: 'j', Authorization: 'a' }))
    .toEqual({ host: 'skole.inschool.visma.no', cookies: { JSESSIONID: 'j', Authorization: 'a' } });
});

it('Teams: access_token + refresh_token, лишние поля не уходят', () => {
  expect(toCredentials('teams', { access_token: 'x', refresh_token: 'y', url: 'z' })).toEqual({ access_token: 'x', refresh_token: 'y' });
});
