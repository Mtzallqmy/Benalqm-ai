# المرحلة 01 — Baseline + Provenance + Brand Foundation

**الحالة: مكتملة**

## ما تم إنجازه

- استيراد DeerFlow 2.1.0 من المصدر المثبت عند commit `90359344323856ca31ba4b5e528ffa1c63782551`.
- commit الاستيراد داخل مستودع moataz ai: `487ed3fbc30508683289145c70ab82ea246576ba`.
- الحفاظ على `LICENSE` الأصلي وإضافة `THIRD_PARTY_NOTICES.md`.
- توثيق مصدر الأرشيف وبصمته وملفات التحقق في `docs/UPSTREAM_PROVENANCE.md`.
- استبدال README الرئيسي بوثيقة عربية خاصة بـmoataz ai.
- إنشاء `frontend/src/brand/` كمصدر مركزي لاسم المنتج والهوية الأولية.
- تعريف foundation أولي للألوان والحركة دون إعادة كتابة نظام التصميم في هذه المرحلة.
- تغيير metadata الظاهر إلى `moataz ai`.
- تغيير محتوى Settings → About ليذكر DeerFlow/ByteDance والمستودع والـbaseline commit والترخيص بوضوح.
- إزالة workflow المؤقت المستخدم للاستيراد بعد نجاح العملية لمنع إعادة bootstrap بالخطأ.
- إغلاق issue bootstrap بعد اكتمال العملية.

## ما لم نبدأه عمدًا

هذه المرحلة لا تتضمن بعد:

- إعادة تصميم الشاشات.
- الشعار والأيقونات النهائية.
- ربط palette الجديدة بكل CSS tokens.
- العربية داخل نظام i18n.
- RTL.
- Mobile navigation.
- Provider Manager.
- Android.

هذه البنود تبدأ في مراحلها المحددة فقط بعد اعتماد المرحلة الحالية.

## حدود upstream المحفوظة

لم نغير في هذه المرحلة agent runtime أو harness أو tools أو skills أو sandbox أو contracts. كما لم نعد تسمية الحزم الداخلية `deerflow`.

## ملاحظة CI

المجلد `.github/workflows/` من upstream لم يدخل في bootstrap بسبب قيد صلاحيات GitHub Actions الخاص بإنشاء/استبدال workflows. هذا موثق في `docs/UPSTREAM_PROVENANCE.md` ولا يؤثر في كود التطبيق أو runtime.

## المرحلة التالية عند اعتماد المستخدم

**المرحلة 02 — الهوية البصرية ونظام التصميم**.

لن تبدأ تلقائيًا.
