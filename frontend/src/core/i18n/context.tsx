"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

import type { Locale } from "@/core/i18n";
import { getLocaleDirection } from "@/core/i18n/locale";
import type { Translations } from "@/core/i18n/locales";

import { clientTranslations } from "./client-translations";

export interface I18nContextType {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: Translations;
}

export const I18nContext = createContext<I18nContextType | null>(null);

export function I18nProvider({
  children,
  initialLocale,
}: {
  children: ReactNode;
  initialLocale: Locale;
}) {
  const [locale, setLocale] = useState<Locale>(initialLocale);
  const [t, setTranslations] = useState<Translations>(
    clientTranslations[initialLocale],
  );

  const handleSetLocale = useCallback((newLocale: Locale) => {
    setLocale(newLocale);
    setTranslations(clientTranslations[newLocale]);
  }, []);

  const direction = getLocaleDirection(locale);

  useEffect(() => {
    document.documentElement.lang = locale;
    document.documentElement.dir = direction;
    document.documentElement.dataset.locale = locale;
  }, [locale, direction]);

  return (
    <I18nContext.Provider value={{ locale, setLocale: handleSetLocale, t }}>
      <div className="contents" lang={locale} dir={direction} data-locale={locale}>
        {children}
      </div>
    </I18nContext.Provider>
  );
}

export function useI18nContext() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error("useI18n must be used within I18nProvider");
  }
  return context;
}
