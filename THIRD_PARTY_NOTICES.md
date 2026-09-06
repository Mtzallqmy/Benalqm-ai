# Third-Party Notices — moataz ai

هذا الملف يوثّق المصادر الخارجية الأساسية التي يحتويها مستودع **moataz ai** أو بُني عليها. لا يحل هذا الملف محل ملفات الترخيص الأصلية الموجودة داخل المستودع.

## 1. DeerFlow

جزء جوهري من هذا المشروع مشتق مباشرة من:

- **Project:** DeerFlow
- **Repository:** https://github.com/bytedance/deer-flow
- **Pinned baseline commit:** `90359344323856ca31ba4b5e528ffa1c63782551`
- **Baseline version:** `2.1.0`
- **License:** MIT License
- **Copyright:**
  - Copyright (c) 2025 Bytedance Ltd. and/or its affiliates
  - Copyright (c) 2025-2026 DeerFlow Authors

تم الاحتفاظ بملف `LICENSE` الأصلي في جذر المستودع وفق شروط MIT.

moataz ai ليس مشروعًا رسميًا تابعًا لـByteDance ولا يعني استخدام الكود الأصلي وجود اعتماد أو رعاية من ByteDance أو DeerFlow Authors.

## 2. DeerFlow bundled skills with separate license files

يحتوي خط الأساس المثبت على Skills لها ملفات ترخيص مستقلة داخل مجلداتها، ومن بينها:

### frontend-design

- Path: `skills/public/frontend-design/`
- License file: `skills/public/frontend-design/LICENSE.txt`
- License: Apache License 2.0

### skill-creator

- Path: `skills/public/skill-creator/`
- License file: `skills/public/skill-creator/LICENSE.txt`
- License: Apache License 2.0

يجب الرجوع إلى ملفات الترخيص نفسها للحصول على النص القانوني الكامل والشروط الدقيقة.

## 3. Other dependencies

يستخدم DeerFlow وعدد من أجزائه مكتبات وأطرًا مفتوحة المصدر إضافية يحددها مديرو الحزم والملفات الأصلية، بما في ذلك ملفات مثل:

- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- `backend/pyproject.toml`
- `backend/uv.lock`

لكل dependency رخصته وشروطه الخاصة. لا يُقصد من هذه الوثيقة أن تكون قائمة قانونية كاملة لكل dependency transitive.

## 4. Provenance policy for moataz ai

عند أخذ كود أو ملفات أو تصميمات تشغيلية مباشرة من upstream في مراحل لاحقة سنقوم بأحد الآتي حسب الحالة:

1. إبقاء الملف الأصلي وترخيصه كما هو عندما يكون ذلك أنسب للتوافق مع upstream.
2. توثيق التعديل والمصدر في سجل provenance عندما نعدل ملفًا موروثًا بصورة جوهرية.
3. إبقاء ملفات الترخيص والإشعارات المطلوبة بجوار المكونات التي تتطلب ذلك.
4. عدم تقديم العلامة التجارية DeerFlow أو ByteDance على أنها العلامة التجارية الخاصة بـmoataz ai.

للتفاصيل التقنية عن خط الأساس راجع `docs/UPSTREAM_PROVENANCE.md`.
