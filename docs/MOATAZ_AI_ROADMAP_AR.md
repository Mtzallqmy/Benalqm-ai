# خارطة تطوير moataz ai

هذه الخطة مصممة لتنفيذ المشروع **مرحلة مستقلة في كل مرة**. لا ننتقل إلى المرحلة التالية قبل انتهاء معايير القبول للمرحلة الحالية ومراجعتها.

## قواعد ثابتة طوال التطوير

1. المشروع هو تطوير مباشر لـDeerFlow، وليس إعادة بناء منفصلة.
2. نحافظ على الـbackend والـharness والـskills والعقود الأصلية بلغاتها وبنيتها ما لم يوجد سبب وظيفي واضح للتعديل.
3. نستخدم TypeScript حديثًا في إضافات الواجهة، وKotlin في طبقة Android الأصلية عند الحاجة، ونستمر مع Python في امتدادات backend التي تتكامل مباشرة مع DeerFlow.
4. لا نضيف framework جديدًا إذا كانت التقنية الحالية تحل المهمة بكفاءة.
5. كل مرحلة يجب أن تكون قابلة للاختبار والرجوع عنها بصورة مستقلة.
6. المصدر والترخيص وattribution جزء من المنتج، وليس ملاحظة خارجية.
7. لا تخزن مفاتيح API في الواجهة أو بصيغة plaintext في قاعدة البيانات.
8. دعم العربية يعني RTL صحيحًا وظيفيًا، وليس مجرد ترجمة النصوص.
9. تجربة الهاتف يجب أن تحافظ على كامل وظائف DeerFlow، لا أن تتحول إلى chat مبسط.
10. تحديثات upstream المستقبلية يجب أن تظل ممكنة بأقل قدر من تعارضات Git.

---

## المرحلة 01 — Baseline + Provenance + Brand Foundation

### الهدف

تثبيت النسخة الأصلية الموثقة وإنشاء أساس هوية moataz ai دون المساس بقلب DeerFlow.

### نطاق العمل

- استيراد DeerFlow 2.1.0 عند commit مثبت.
- حفظ `LICENSE` الأصلي.
- إنشاء `THIRD_PARTY_NOTICES.md`.
- إنشاء سجل provenance تفصيلي.
- استبدال README الرئيسي بREADME عربي خاص بـmoataz ai.
- إضافة Brand module مركزي للواجهة.
- تغيير metadata الظاهر للمستخدم إلى moataz ai.
- تعديل صفحة About لعرض مصدر DeerFlow بوضوح.
- عدم تغيير أسماء packages/imports الداخلية.

### معيار القبول

- المصدر قابل للتتبع إلى commit محدد.
- attribution ظاهر في المستودع وداخل التطبيق.
- لا تعديل وظيفي على agent runtime.
- Brand config له نقطة مصدر واحدة.

---

## المرحلة 02 — الهوية البصرية ونظام التصميم

### الهدف

إنشاء لغة بصرية متماسكة ومختلفة عن DeerFlow مع الحفاظ على مكونات الواجهة الوظيفية.

### نطاق العمل

- شعار وأيقونة وهوية مرئية خاصة.
- نظام ألوان Light/Dark.
- semantic design tokens بدل الألوان المتفرقة.
- typography عربية/لاتينية متناسقة.
- elevation، borders، radius، spacing، surfaces.
- نظام motion موحد باستخدام أدوات المشروع الحالية حيث تكفي.
- حالات loading/skeleton/empty/error متسقة.
- App icon وfavicon وPWA assets لاحقًا من نفس النظام.

### معيار القبول

- لا توجد هوية DeerFlow ظاهرة في الشاشات الأساسية باستثناء attribution في About/Notices.
- contrast وfocus states قابلة للاستخدام.
- Light/Dark يعملان من نفس tokens.

---

## المرحلة 03 — العربية والإنجليزية + RTL/LTR

### الهدف

جعل العربية والإنجليزية لغتين أصليتين للمنتج.

### نطاق العمل

- إضافة `ar` إلى نظام i18n الحالي.
- locale selection وحفظ التفضيل.
- `dir="rtl"` و`dir="ltr"` ديناميكيًا.
- ترجمة workspace، chat، settings، agents، skills، tasks، errors، auth.
- إبقاء code blocks والterminal والروابط التقنية LTR داخل RTL.
- تنسيق التواريخ والأرقام والنصوص المختلطة.
- E2E tests للشاشات الحساسة في اللغتين.

### معيار القبول

- لا نصوص أساسية hardcoded خارج نظام الترجمة.
- الواجهة كاملة الاستخدام في العربية والإنجليزية.
- لا توجد انعكاسات RTL خاطئة في المحرر أو terminal أو artifacts.

---

## المرحلة 04 — Mobile-first Workspace + Navigation + Motion

### الهدف

تحويل الواجهة responsive الحالية إلى تجربة تطبيق جوال كاملة مع الحفاظ على واجهة Desktop.

### نطاق العمل

- Mobile shell مستقل بصريًا فوق نفس core.
- Bottom navigation.
- mobile header/context actions.
- composer محسّن للوحة المفاتيح وsafe areas.
- تحويل side panels إلى full-screen sheets/screens عند الحاجة.
- Artifacts viewer للجوال.
- Sub-agent progress وتجربة tool calls مناسبة للشاشة الصغيرة.
- transitions وshared motion patterns.
- تحسين touch targets والسحب والرجوع.
- المحافظة على thread streaming والسجل والملفات دون إعادة كتابة core.

### معيار القبول

- الوظائف الأساسية نفسها متاحة Desktop/Mobile.
- اختبارات Playwright لأحجام Android الشائعة.
- لا horizontal overflow في المسارات الأساسية.

---

## المرحلة 05 — Dynamic Provider & Model Registry (BYOK)

### الهدف

السماح للمستخدم بإضافة مزود ذكاء اصطناعي من الواجهة بدل تعديل `config.yaml` يدويًا.

### تجربة المستخدم

```text
Provider type
API Key
Base URL
    ↓
Test connection
    ↓
Discover models
    ↓
Probe capabilities
    ↓
Show compatible models
    ↓
Select default model
```

### النطاق

- OpenAI-compatible provider adapter.
- OpenAI preset.
- OpenRouter metadata adapter.
- Custom OpenAI-compatible endpoint.
- Ollama native adapter.
- vLLM adapter المتوافق مع DeerFlow.
- LM Studio preset.
- LiteLLM gateway كخيار متقدم، لا اعتماد إجباري.
- model discovery عبر endpoint مناسب للمزود.
- capability probes: auth/chat/streaming/tools/reasoning/vision عندما تنطبق.
- حالات واضحة لـ401/403/404/429/timeouts.
- Dynamic Model Registry يندمج مع `create_chat_model()` بدل استبداله.

### الأمان

- تشفير credentials at rest.
- master key خارج قاعدة البيانات.
- عدم إعادة API Key كاملًا إلى frontend بعد الحفظ.
- SSRF policy مختلفة بين local trusted mode وhosted mode.
- redaction في logs/errors.

### معيار القبول

- المستخدم يستطيع بدء محادثة بنموذج مضاف من الواجهة بدون تعديل YAML.
- model/tool capability mismatch يظهر قبل بدء agent run قدر الإمكان.
- لا تظهر secrets في network responses أو logs الاعتيادية.

---

## المرحلة 06 — التشغيل المحلي المتكامل

### الهدف

جعل moataz ai سهل التشغيل على جهاز شخصي أو خادم منزلي مع مزود محلي أو سحابي.

### النطاق

- Docker Compose profile واضح لـmoataz ai.
- Nginx كنقطة دخول موحدة.
- health checks للfrontend/gateway/redis/sandbox/provider.
- إرشادات Windows/macOS/Linux.
- Ollama على host.
- LM Studio على host.
- vLLM على GPU host/server.
- OpenAI-compatible LAN endpoints.
- معالجة `host.docker.internal` وLinux host-gateway.
- local-network provider policy صريحة.

### معيار القبول

- تشغيل موثق وقابل للتكرار.
- Android/Web يستطيعان الوصول من LAN دون تغيير backend logic.
- فشل أحد المزودات يعطي تشخيصًا مفهومًا.

---

## المرحلة 07 — Android

### القرار المعماري الابتدائي

نبدأ بـPWA قوية ثم Trusted Web Activity حتى يبقى لدينا:

```text
1 frontend
1 backend
1 streaming implementation
1 auth implementation
1 artifacts implementation
```

ولا ننشئ تطبيق Flutter/React Native يعيد كتابة DeerFlow.

### النطاق

- Web App Manifest.
- installability وstandalone display.
- Android project / TWA shell.
- Digital Asset Links عند النشر.
- Android icons/splash/theme.
- deep links.
- file upload/download/share behavior.
- keyboard/back navigation.
- تقييم Capacitor فقط إذا احتجنا APIs أصلية لا توفرها TWA/PWA بصورة مناسبة.

### معيار القبول

- نفس الحساب/threads/skills/models تعمل من Android.
- لا API keys محفوظة في Android Web storage خارج السياسة المعتمدة.
- العودة والخروج والروابط والملفات تعمل بصورة طبيعية كتطبيق.

---

## المرحلة 08 — Hardening + QA + Release

### النطاق

- backend unit/integration tests.
- frontend unit/E2E.
- Arabic/English visual regression checks.
- security review للprovider manager وsandbox وMCP.
- accessibility audit.
- performance budgets.
- Android release build.
- documentation النهائية.
- upgrade procedure من DeerFlow upstream.
- release notes وversioning خاص بـmoataz ai.

### معيار القبول

- build نظيف.
- الاختبارات الحرجة ناجحة.
- المسارات الحساسة للأسرار والعزل مدققة.
- provenance وlicenses محدثة.

---

## سياسة الانتقال بين المراحل

بعد كل مرحلة:

1. نرفع التغييرات إلى مستودع `Mtzallqmy/Benalqm-ai`.
2. نتحقق من الملفات والـcommit(s) الناتجة.
3. نذكر بوضوح ما اكتمل وما لم يبدأ بعد.
4. نتوقف.
5. لا تبدأ المرحلة التالية إلا بأمر المستخدم.
