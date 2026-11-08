// Тексты страниц /privacy и /terms. Совпадают по смыслу с PRIVACY.md и TERMS.md в корне репозитория:
// меняете одно — меняйте и другое. В строках можно ссылки вида [текст](/privacy) или [текст](https://…).
export type LegalBlock = string | { list: string[] };
export type LegalSection = { id: string; title: string; body: LegalBlock[] };
export type LegalDoc = { eyebrow: string; title: string; intro: string; sections: LegalSection[] };
export type LegalTexts = {
  updated: string; contents: string;
  privacy: LegalDoc; terms: LegalDoc;
};

export const REPO_URL = 'https://github.com/two-bits1/LekseRadar';
export const ISSUES_URL = `${REPO_URL}/issues/new`;
export const SECURITY_URL = `${REPO_URL}/security/advisories/new`;
