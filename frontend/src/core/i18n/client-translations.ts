import type { Locale } from "./locale";
import { arSA } from "./locales/ar-SA";
import { enMoataz } from "./locales/en-moataz";
import type { Translations } from "./locales/types";
import { zhCN } from "./locales/zh-CN";

export const clientTranslations: Record<Locale, Translations> = {
  "ar-SA": arSA,
  "en-US": enMoataz,
  "zh-CN": zhCN,
};
