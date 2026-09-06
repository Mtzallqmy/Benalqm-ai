import type { Locale } from "./locale";
import type { Translations } from "./locales";

const translationLoaders: Record<Locale, () => Promise<Translations>> = {
  "ar-SA": async () => (await import("./locales/ar-SA")).arSA,
  "en-US": async () => (await import("./locales/en-moataz")).enMoataz,
  "zh-CN": async () => (await import("./locales/zh-CN")).zhCN,
};

export async function loadTranslations(locale: Locale): Promise<Translations> {
  return await translationLoaders[locale]();
}
