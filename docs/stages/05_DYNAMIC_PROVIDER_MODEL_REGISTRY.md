# المرحلة 05 — Dynamic Provider & Model Registry (BYOK)

**الحالة: مكتملة وظيفيًا — مع متطلب تشغيل أمني موثق**

## ما تم إنجازه

- سجل Dynamic Provider مستقل عن `config.yaml` حتى يبقى upstream قابلًا للدمج.
- دعم presets: OpenAI، OpenRouter، OpenAI-compatible، Ollama، vLLM، LM Studio، LiteLLM.
- API إدارية: list/test/discover/probe/save/delete/default.
- model discovery من `/models` للواجهات المتوافقة و`/api/tags` لـOllama.
- capability metadata للـreasoning/vision/tools/streaming/context window عند توفر ما يكفي للاستدلال.
- chat probe اختياري قبل الحفظ.
- دمج النماذج الديناميكية في `/api/models` مع نفس authorization filtering.
- دمج dynamic defaults وdynamic model names في `deerflow.models.create_chat_model` مع تفويض النماذج الثابتة إلى factory الأصلي بلا تغيير.
- واجهة `/workspace/providers` كاملة: نوع المزود → المفتاح/base URL → test → discover → select → probe → save → default.
- عدم إعادة المفتاح المحفوظ إلى frontend؛ list API يعيد `has_api_key` فقط.
- تخزين سجل المزودات في ملف 0600 وكتابة atomic.

## الأمان

- `MOATAZ_PROVIDER_MASTER_KEY` مطلوب لحفظ أي API key ويجب أن يكون عالي entropy وطوله 32 byte على الأقل.
- التشفير يستخدم Fernet من مكتبة `cryptography` مع مفتاح مشتق من master key؛ إذا كانت المكتبة غير متاحة يرفض النظام حفظ credential بدل أي plaintext fallback.
- لا تُدرج API keys في رسائل HTTP errors الخاصة بالاتصال.
- `MOATAZ_PROVIDER_NETWORK_POLICY=hosted` هو الوضع الافتراضي ويحظر localhost/private/link-local/reserved targets بما فيها DNS التي تحل إلى نطاق خاص.
- لاستخدام Ollama/LM Studio/vLLM على LAN أو host يجب تعيين `MOATAZ_PROVIDER_NETWORK_POLICY=local` صراحةً.
- إدارة المزودات Admin-only وPAT لا يرث admin capability عبر `require_admin_user` الأصلي.

## متطلب الحزمة

ميزة حفظ المفاتيح تعتمد على `cryptography.fernet`. الكود fail-closed إذا لم تكن `cryptography` موجودة. سنثبت متطلب التشغيل بصورة reproducible ضمن Docker/profile في المرحلة 06 بدل تعديل lockfile upstream عشوائيًا في هذه المرحلة.

## الاختبارات المضافة

- secret لا يظهر في ملف السجل.
- غياب master key يرفض التخزين.
- hosted SSRF policy تحظر loopback.
- local policy تسمح بالمزود المحلي عند opt-in.

## معيار القبول

- يمكن إضافة نموذج من الواجهة واختياره من قائمة النماذج دون تعديل YAML.
- static models تستمر عبر factory الأصلي.
- dynamic models تمر عبر create_chat_model public entry point.
- credential لا يعود في network response ولا يُكتب plaintext.
- أخطاء 401/403/404/429/timeout تظهر كتشخيص provider-safe دون echo للسر.

## التالي

**المرحلة 06 — التشغيل المحلي المتكامل**.
