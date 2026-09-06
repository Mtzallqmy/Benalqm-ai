# Upstream Provenance — moataz ai

## الغرض

هذه الوثيقة تسجل بصورة قابلة للمراجعة النسخة الأصلية التي بدأ منها تطوير **moataz ai**، وما الذي استوردناه وما الذي استثنيناه عمدًا أثناء bootstrap الأول.

## المصدر المثبت

| الحقل | القيمة |
|---|---|
| Upstream project | ByteDance DeerFlow |
| Repository | `https://github.com/bytedance/deer-flow` |
| Version | `2.1.0` |
| Commit | `90359344323856ca31ba4b5e528ffa1c63782551` |
| Commit subject | `feat(mcp): add optional Parallel Search server (#5028)` |
| License | MIT |

## الملف الأصلي الذي استُخدم للمراجعة

الملف الذي تم توفيره لبدء المشروع كان أرشيف DeerFlow الأصلي.

```text
deer-flow-main.zip
SHA-256: cf11f5bafc8a895d7b31dba80a57cfe3fddc50cc5235772e5fb172685382afb8
```

احتوى الأرشيف المفحوص على `2,584` ملفًا تقريبًا بحجم إجمالي يقارب `55 MB` قبل بدء تخصيص moataz ai.

## التحقق من التطابق

قبل الاستيراد قارنا Git blob IDs لعدة ملفات عالية الدلالة بين الأرشيف المرفوع وupstream عند الـcommit المثبت:

| الملف | Git blob SHA |
|---|---|
| `README.md` | `7c71928feff0e63bd540ebbc2f8d22c0158908f6` |
| `frontend/package.json` | `1ede350f438daea4e52b4173a6ed22985d741d7c` |
| `backend/pyproject.toml` | `1a8149a6b72e0c343f75900ae380db133208ed49` |
| `config.example.yaml` | `d4acda969259466bda78b237b42004f001e47188` |

كانت القيم متطابقة مع upstream، ولذلك اعتُمد commit المذكور كمرجع المصدر الرسمي للـbaseline بدل إنشاء نسخة مجهولة الأصل.

## ما تم استيراده

تم استيراد كود التطبيق والـbackend والـharness والـskills والعقود والاختبارات والوثائق وملفات Docker/deployment من الـcommit المثبت، مع إبقاء لغاتها وبنيتها الأصلية كما هي في baseline.

## الاستثناء المتعمد

لم يُستورد المجلد التالي آليًا في bootstrap:

```text
.github/workflows/
```

السبب: GitHub رفض أن يقوم رمز GitHub Actions المستخدم أثناء bootstrap بإنشاء/استبدال workflows قادمة من مستودع آخر لعدم امتلاكه صلاحية `workflows` المطلوبة. بدل الالتفاف على هذا القيد، أبقينا CI الخاص بمستودع moataz ai تحت سيطرة المشروع وسجلنا الاستثناء هنا.

هذا الاستثناء يخص **أتمتة GitHub CI فقط** ولا يغير runtime أو backend أو frontend أو skills الخاصة بـDeerFlow.

## قاعدة التعامل مع upstream

للحفاظ على إمكانية استيعاب تحديثات DeerFlow مستقبلًا:

- لا نعيد تسمية Python package `deerflow` أو العقود الداخلية لمجرد تغيير العلامة التجارية.
- لا نترجم أسماء الدوال/classes/variables الموروثة.
- لا نعيد كتابة backend مستقر بلغة أخرى فقط لأغراض الهوية.
- إضافات moataz ai الجديدة تُعزل في modules واضحة قدر الإمكان.
- أي تعديل جوهري على ملف upstream يجب أن يكون محدودًا، مبررًا، وقابلًا للمقارنة.
- تبقى ملفات `LICENSE` الأصلية وإشعارات الطرف الثالث محفوظة.

## طبقات الملكية التقنية

```text
Upstream-compatible core
├── backend/packages/harness/deerflow
├── backend/app
├── skills
├── contracts
└── runtime/deployment foundations

moataz ai product layer
├── frontend/src/brand
├── localized product UX
├── mobile application shell
├── dynamic provider management
├── Android integration
└── moataz ai documentation and design system
```

## سجل baseline داخل المستودع

يوجد أيضًا سجل آلي مختصر في:

```text
.moataz-ai/upstream-baseline.json
```

ويجب تحديث هذه الوثيقة وذلك السجل إذا انتقلنا مستقبلًا إلى baseline جديد من DeerFlow.
