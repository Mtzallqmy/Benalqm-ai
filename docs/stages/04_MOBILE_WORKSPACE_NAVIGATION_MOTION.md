# المرحلة 04 — Mobile-first Workspace + Navigation + Motion

**الحالة: مكتملة**

## ما تم إنجازه

- إضافة Mobile bottom navigation حقيقي داخل نفس Workspace وليس تطبيق محادثة منفصلًا.
- التنقل السفلي يوفر: المحادثات، الوكلاء، محادثة جديدة، المهام المجدولة، والإعدادات.
- استخدام `100dvh` بدل الاعتماد على `100vh` فقط لتجنب مشاكل شريط المتصفح ولوحة المفاتيح على Android.
- دعم `env(safe-area-inset-bottom)` في شريط التنقل ومساحة المحتوى.
- إبقاء Sidebar الأصلي كمسار تنقل Desktop وإبقاء drawer behavior الحالي للموبايل.
- تحديث Workspace header إلى touch targets أكبر ومسافات منطقية RTL/LTR.
- توجيه رابط GitHub الظاهر للمستخدم إلى مستودع moataz ai بدل upstream؛ attribution الأصلي يبقى في About/Notices.
- تحسين Artifact Viewer للموبايل: رأس مضغوط، أزرار أيقونية، ارتفاع ديناميكي، وشريط truncation responsive.
- إضافة طبقة `mobile-workspace.css` مستقلة عن upstream.
- إضافة اختبار Playwright على مقاسي Android شائعين للتحقق من عدم وجود horizontal overflow في الصفحة الرئيسية.

## مبادئ التنفيذ

- لم نكرر thread streaming أو auth أو artifacts أو state management.
- الموبايل والـDesktop يستخدمان نفس routes ونفس APIs ونفس مكونات المجال.
- touch chrome الجديد مجرد طبقة product UI فوق core الموجود.

## معيار القبول

- مسارات Workspace الرئيسية يمكن الوصول إليها بيد واحدة على الهاتف.
- شريط التنقل يحترم safe area.
- `100dvh` يمنع قفزات الارتفاع الشائعة عند ظهور browser chrome.
- Artifact Viewer يعمل ضمن عرض صغير دون الاعتماد على labels طويلة.
- لا horizontal overflow في اختبارات Android المضافة.

## التالي

**المرحلة 05 — Dynamic Provider & Model Registry (BYOK)**.
