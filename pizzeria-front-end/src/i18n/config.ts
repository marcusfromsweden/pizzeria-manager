import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import enCommon from './locales/en/common.json';
import enAuth from './locales/en/auth.json';
import enMenu from './locales/en/menu.json';
import svCommon from './locales/sv/common.json';
import svAuth from './locales/sv/auth.json';
import svMenu from './locales/sv/menu.json';

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: {
        common: enCommon,
        auth: enAuth,
        menu: enMenu,
      },
      sv: {
        common: svCommon,
        auth: svAuth,
        menu: svMenu,
      },
    },
    fallbackLng: 'en',
    supportedLngs: ['en', 'sv'],
    defaultNS: 'common',
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
    },
  });

export default i18n;
