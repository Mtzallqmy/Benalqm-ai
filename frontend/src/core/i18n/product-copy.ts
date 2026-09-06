import type { Locale } from "./locale";

export type ProductCopy = {
  languageSwitch: string;
  sourceLicense: string;
  openWorkspace: string;
  badge: string;
  heroPrefix: string;
  heroHighlight: string;
  heroSuffix: string;
  description: string;
  secondaryDescription: string;
  startWorking: string;
  exploreDocs: string;
  upstreamNotice: string;
  runtimeNotice: string;
  capabilities: { title: string; body: string }[];
  setup: {
    createAdmin: string;
    createAdminDescription: string;
    completeAdmin: string;
    completeAdminDescription: string;
    email: string;
    password: string;
    confirmPassword: string;
    currentPassword: string;
    newPassword: string;
    yourEmail: string;
    passwordsMismatch: string;
    passwordTooShort: string;
    networkError: string;
    creating: string;
    create: string;
    settingUp: string;
    complete: string;
    loading: string;
  };
};

const en: ProductCopy = {
  languageSwitch: "العربية",
  sourceLicense: "Source & license",
  openWorkspace: "Open workspace",
  badge: "New product layer · DeerFlow core preserved",
  heroPrefix: "AI that",
  heroHighlight: "works through",
  heroSuffix: "the task with you.",
  description: "A comprehensive AI agent workspace for research, execution, knowledge and artifacts.",
  secondaryDescription: "Built as a distinct bilingual product experience over the open-source DeerFlow agent harness.",
  startWorking: "Start working",
  exploreDocs: "Explore docs",
  upstreamNotice: "MIT upstream attribution retained",
  runtimeNotice: "One web workspace, extensible runtime",
  capabilities: [
    { title: "Agent orchestration", body: "Lead agent, sub-agents, tools, skills and MCP remain connected through the proven runtime." },
    { title: "Research to artifacts", body: "Move from exploration and analysis to files, code, media and structured outputs in one workspace." },
    { title: "Arabic + English native", body: "True RTL/LTR behavior, not translation-only localization." },
  ],
  setup: {
    createAdmin: "Create admin account",
    createAdminDescription: "Set up the administrator account to get started.",
    completeAdmin: "Complete admin account setup",
    completeAdminDescription: "Set your real email and a new password.",
    email: "Email",
    password: "Password",
    confirmPassword: "Confirm password",
    currentPassword: "Current password",
    newPassword: "New password",
    yourEmail: "Your email",
    passwordsMismatch: "Passwords do not match",
    passwordTooShort: "Password must be at least 8 characters",
    networkError: "Network error. Please try again.",
    creating: "Creating account…",
    create: "Create Admin Account",
    settingUp: "Setting up…",
    complete: "Complete Setup",
    loading: "Loading…",
  },
};

const ar: ProductCopy = {
  languageSwitch: "English",
  sourceLicense: "المصدر والترخيص",
  openWorkspace: "فتح مساحة العمل",
  badge: "طبقة منتج جديدة · مع الحفاظ على نواة DeerFlow",
  heroPrefix: "ذكاء",
  heroHighlight: "ينفّذ المهمة",
  heroSuffix: "معك خطوة بخطوة.",
  description: "مساحة عمل شاملة لوكلاء الذكاء الاصطناعي للبحث والتنفيذ وصناعة المعرفة والمخرجات.",
  secondaryDescription: "تجربة عربية وإنجليزية مستقلة مبنية فوق نواة DeerFlow مفتوحة المصدر.",
  startWorking: "ابدأ العمل",
  exploreDocs: "استكشف التوثيق",
  upstreamNotice: "الإسناد والترخيص الأصلي محفوظان",
  runtimeNotice: "مساحة عمل واحدة ونواة قابلة للتوسعة",
  capabilities: [
    { title: "تنسيق الوكلاء", body: "الوكيل الرئيسي والوكلاء الفرعيون والأدوات والمهارات وMCP تعمل ضمن نواة واحدة." },
    { title: "من البحث إلى المخرجات", body: "انتقل من الاستكشاف والتحليل إلى الملفات والكود والوسائط والمخرجات المنظمة." },
    { title: "العربية والإنجليزية أصلًا", body: "دعم RTL/LTR وظيفي حقيقي، وليس مجرد ترجمة للنصوص." },
  ],
  setup: {
    createAdmin: "إنشاء حساب المدير",
    createAdminDescription: "أنشئ حساب المدير لبدء استخدام moataz ai.",
    completeAdmin: "إكمال إعداد حساب المدير",
    completeAdminDescription: "أدخل بريدك الحقيقي وكلمة مرور جديدة.",
    email: "البريد الإلكتروني",
    password: "كلمة المرور",
    confirmPassword: "تأكيد كلمة المرور",
    currentPassword: "كلمة المرور الحالية",
    newPassword: "كلمة المرور الجديدة",
    yourEmail: "بريدك الإلكتروني",
    passwordsMismatch: "كلمتا المرور غير متطابقتين",
    passwordTooShort: "يجب ألا تقل كلمة المرور عن 8 أحرف",
    networkError: "خطأ في الشبكة. حاول مجددًا.",
    creating: "جارٍ إنشاء الحساب…",
    create: "إنشاء حساب المدير",
    settingUp: "جارٍ الإعداد…",
    complete: "إكمال الإعداد",
    loading: "جارٍ التحميل…",
  },
};

export function getProductCopy(locale: Locale): ProductCopy {
  if (locale === "ar-SA") return ar;
  return en;
}
