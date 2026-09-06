export const MOATAZ_AI_BRAND = {
  id: "moataz-ai",
  name: "moataz ai",
  shortName: "moataz ai",
  description: {
    ar: "منصة وكلاء ذكاء اصطناعي شاملة للبحث والتنفيذ وصناعة المعرفة والملفات.",
    en: "A comprehensive AI agent workspace for research, execution, knowledge, and artifacts.",
  },
  tagline: {
    ar: "ذكاء يعمل معك، لا يكتفي بالإجابة.",
    en: "Intelligence that works with you, beyond answers.",
  },
  locales: ["ar", "en-US"] as const,
  repository: "https://github.com/Mtzallqmy/Benalqm-ai",
  upstream: {
    name: "DeerFlow",
    organization: "ByteDance",
    repository: "https://github.com/bytedance/deer-flow",
    baselineVersion: "2.1.0",
    baselineCommit: "90359344323856ca31ba4b5e528ffa1c63782551",
    license: "MIT",
  },
} as const;

export type MoatazAiBrand = typeof MOATAZ_AI_BRAND;
