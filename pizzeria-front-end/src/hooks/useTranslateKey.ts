import { useTranslation } from 'react-i18next';

export const useTranslateKey = () => {
  const { t, i18n } = useTranslation('menu');

  const translateKey = (key: string, fallback?: string): string => {
    // The key might be like "translation.key.disc.pizza.margarita"
    // We need to look it up in the menu namespace
    const translated = t(key, { defaultValue: '' });

    // If we got a translation, return it
    if (translated && translated !== key) {
      return translated;
    }

    // Try to extract a readable name from the key itself
    if (fallback) {
      return fallback;
    }

    // Last resort: extract the last part of the key and format it
    const parts = key.split('.');
    const lastPart = parts[parts.length - 1];
    return lastPart
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (c) => c.toUpperCase());
  };

  return { translateKey, currentLanguage: i18n.language };
};

export default useTranslateKey;
