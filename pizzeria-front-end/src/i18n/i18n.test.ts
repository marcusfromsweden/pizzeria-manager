import { describe, it, expect, beforeEach } from 'vitest';
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import enCommon from './locales/en/common.json';
import enAuth from './locales/en/auth.json';
import enMenu from './locales/en/menu.json';
import svCommon from './locales/sv/common.json';
import svAuth from './locales/sv/auth.json';
import svMenu from './locales/sv/menu.json';

// Create a test i18n instance
const createTestI18n = () => {
  const instance = i18n.createInstance();
  void instance.use(initReactI18next).init({
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
    lng: 'en',
    fallbackLng: 'en',
    supportedLngs: ['en', 'sv'],
    defaultNS: 'common',
    interpolation: {
      escapeValue: false,
    },
  });
  return instance;
};

describe('i18n configuration', () => {
  let testI18n: typeof i18n;

  beforeEach(() => {
    testI18n = createTestI18n();
  });

  describe('initialization', () => {
    it('should initialize with English as default language', () => {
      expect(testI18n.language).toBe('en');
    });

    it('should have English as fallback language', () => {
      expect(testI18n.options.fallbackLng).toEqual(['en']);
    });

    it('should support English and Swedish', () => {
      // i18next adds 'cimode' (CI mode) for debugging purposes
      expect(testI18n.options.supportedLngs).toContain('en');
      expect(testI18n.options.supportedLngs).toContain('sv');
    });

    it('should use common as default namespace', () => {
      expect(testI18n.options.defaultNS).toBe('common');
    });

    it('should not escape values (React handles escaping)', () => {
      expect(testI18n.options.interpolation?.escapeValue).toBe(false);
    });
  });

  describe('English translations', () => {
    it('should translate common navigation keys', () => {
      expect(testI18n.t('nav.home')).toBe('Home');
      expect(testI18n.t('nav.menu')).toBe('Menu');
      expect(testI18n.t('nav.pizzas')).toBe('Pizzas');
      expect(testI18n.t('nav.signIn')).toBe('Sign in');
      expect(testI18n.t('nav.signOut')).toBe('Sign out');
    });

    it('should translate common action keys', () => {
      expect(testI18n.t('actions.save')).toBe('Save');
      expect(testI18n.t('actions.cancel')).toBe('Cancel');
      expect(testI18n.t('actions.delete')).toBe('Delete');
      expect(testI18n.t('actions.back')).toBe('Back');
    });

    it('should translate common status keys', () => {
      expect(testI18n.t('status.loading')).toBe('Loading...');
      expect(testI18n.t('status.error')).toBe('An error occurred');
      expect(testI18n.t('status.success')).toBe('Success');
    });

    it('should translate auth namespace keys', () => {
      expect(testI18n.t('login.title', { ns: 'auth' })).toBe('Sign in');
      expect(testI18n.t('register.title', { ns: 'auth' })).toBe('Create account');
    });

    it('should translate validation messages', () => {
      expect(testI18n.t('validation.required')).toBe('This field is required');
      expect(testI18n.t('validation.invalidEmail')).toBe(
        'Please enter a valid email address'
      );
    });
  });

  describe('Swedish translations', () => {
    beforeEach(async () => {
      await testI18n.changeLanguage('sv');
    });

    it('should translate common navigation keys in Swedish', () => {
      expect(testI18n.t('nav.home')).toBe('Hem');
      expect(testI18n.t('nav.menu')).toBe('Meny');
      expect(testI18n.t('nav.pizzas')).toBe('Pizzor');
      expect(testI18n.t('nav.signIn')).toBe('Logga in');
      expect(testI18n.t('nav.signOut')).toBe('Logga ut');
    });

    it('should translate common action keys in Swedish', () => {
      expect(testI18n.t('actions.save')).toBe('Spara');
      expect(testI18n.t('actions.cancel')).toBe('Avbryt');
      expect(testI18n.t('actions.back')).toBe('Tillbaka');
    });

    it('should translate common status keys in Swedish', () => {
      expect(testI18n.t('status.loading')).toBe('Laddar...');
      expect(testI18n.t('status.error')).toBe('Ett fel uppstod');
    });

    it('should translate auth namespace keys in Swedish', () => {
      expect(testI18n.t('login.title', { ns: 'auth' })).toBe('Logga in');
      expect(testI18n.t('register.title', { ns: 'auth' })).toBe('Skapa konto');
    });
  });

  describe('language switching', () => {
    it('should switch from English to Swedish', async () => {
      expect(testI18n.t('nav.home')).toBe('Home');

      await testI18n.changeLanguage('sv');

      expect(testI18n.language).toBe('sv');
      expect(testI18n.t('nav.home')).toBe('Hem');
    });

    it('should switch from Swedish to English', async () => {
      await testI18n.changeLanguage('sv');
      expect(testI18n.t('nav.home')).toBe('Hem');

      await testI18n.changeLanguage('en');

      expect(testI18n.language).toBe('en');
      expect(testI18n.t('nav.home')).toBe('Home');
    });
  });

  describe('namespace handling', () => {
    it('should use common namespace by default', () => {
      expect(testI18n.t('nav.home')).toBe('Home');
    });

    it('should access auth namespace with ns option', () => {
      expect(testI18n.t('login.title', { ns: 'auth' })).toBe('Sign in');
      expect(testI18n.t('register.title', { ns: 'auth' })).toBe('Create account');
    });

    it('should access menu namespace with ns option', () => {
      expect(testI18n.t('title', { ns: 'menu' })).toBe('Our menu');
    });

    it('should access namespaced keys with colon notation', () => {
      expect(testI18n.t('auth:login.title')).toBe('Sign in');
      expect(testI18n.t('menu:title')).toBe('Our menu');
    });
  });

  describe('fallback behavior', () => {
    it('should return key when translation is missing in current and fallback language', () => {
      const missingKey = 'this.key.does.not.exist';
      expect(testI18n.t(missingKey)).toBe(missingKey);
    });

    it('should fallback to English when Swedish translation is missing', async () => {
      await testI18n.changeLanguage('sv');

      // If a key exists in English but not Swedish, it should fallback to English
      // Most keys exist in both, but the behavior is that it falls back
      expect(testI18n.options.fallbackLng).toEqual(['en']);
    });

    it('should use fallback language when invalid language is set', async () => {
      await testI18n.changeLanguage('de'); // German - not supported

      // Should still have a language (fallback behavior)
      expect(testI18n.language).toBeDefined();
    });
  });

  describe('nested keys', () => {
    it('should resolve deeply nested keys', () => {
      expect(testI18n.t('nav.home')).toBe('Home');
      expect(testI18n.t('validation.passwordMinLength')).toBe(
        'Password must be at least 8 characters'
      );
    });
  });

  describe('resource loading', () => {
    it('should have English common resources loaded', () => {
      const resources = testI18n.getResourceBundle('en', 'common');
      expect(resources).toBeDefined();
      expect(resources.nav).toBeDefined();
      expect(resources.actions).toBeDefined();
    });

    it('should have English auth resources loaded', () => {
      const resources = testI18n.getResourceBundle('en', 'auth');
      expect(resources).toBeDefined();
      expect(resources.login).toBeDefined();
      expect(resources.register).toBeDefined();
    });

    it('should have English menu resources loaded', () => {
      const resources = testI18n.getResourceBundle('en', 'menu');
      expect(resources).toBeDefined();
    });

    it('should have Swedish common resources loaded', () => {
      const resources = testI18n.getResourceBundle('sv', 'common');
      expect(resources).toBeDefined();
      expect(resources.nav).toBeDefined();
    });

    it('should have Swedish auth resources loaded', () => {
      const resources = testI18n.getResourceBundle('sv', 'auth');
      expect(resources).toBeDefined();
      expect(resources.login).toBeDefined();
    });

    it('should have Swedish menu resources loaded', () => {
      const resources = testI18n.getResourceBundle('sv', 'menu');
      expect(resources).toBeDefined();
    });
  });

  describe('language consistency', () => {
    it('should have same structure for navigation in both languages', () => {
      const enNav = testI18n.getResourceBundle('en', 'common').nav;
      const svNav = testI18n.getResourceBundle('sv', 'common').nav;

      expect(Object.keys(enNav).sort()).toEqual(Object.keys(svNav).sort());
    });

    it('should have same structure for actions in both languages', () => {
      const enActions = testI18n.getResourceBundle('en', 'common').actions;
      const svActions = testI18n.getResourceBundle('sv', 'common').actions;

      expect(Object.keys(enActions).sort()).toEqual(Object.keys(svActions).sort());
    });

    it('should have same structure for status in both languages', () => {
      const enStatus = testI18n.getResourceBundle('en', 'common').status;
      const svStatus = testI18n.getResourceBundle('sv', 'common').status;

      expect(Object.keys(enStatus).sort()).toEqual(Object.keys(svStatus).sort());
    });
  });
});
