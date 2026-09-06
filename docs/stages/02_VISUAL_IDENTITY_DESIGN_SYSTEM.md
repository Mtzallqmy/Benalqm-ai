# المرحلة 02 — الهوية البصرية ونظام التصميم

**الحالة: مكتملة**

## ما تم إنجازه

- إنشاء طبقة CSS مستقلة `frontend/src/styles/moataz-ai.css` تُحمّل بعد CSS المصدر، بحيث نستطيع تغيير المنتج دون تشتيت تعديلات داخل upstream.
- تعريف semantic tokens موحدة لـLight/Dark تشمل background/card/popover/primary/accent/border/input/ring/sidebar.
- اعتماد signature gradient خاصة بـmoataz ai: violet → indigo → cyan، مع mint للحالات الإيجابية.
- تعريف elevation وglass surfaces وambient grid وmotion primitives وreduced-motion policy.
- رفع radius الأساسي وتحسين focus-visible وselection states.
- اعتماد typography stack تدعم العربية واللاتينية من نفس النظام بدون شحن ملفات خطوط خاصة.
- إنشاء شعار vector مستقل (orbital M mark) داخل `frontend/public/brand/moataz-mark.svg` ومكون React قابل لإعادة الاستخدام.
- إنشاء App Router icon SVG من نفس الشعار.
- بناء Landing جديدة خاصة بـmoataz ai دون حذف مكونات DeerFlow القديمة، لتقليل تعارضات upstream.
- استبدال علامة DF/DeerFlow داخل Workspace header بعلامة moataz ai.
- تحديث metadata لتستخدم نظام الاسم المركزي.

## قرارات معمارية

- لم نعد كتابة `globals.css` الأصلي؛ طبقة التصميم الجديدة override منفصلة وقابلة للإزالة أو المقارنة.
- لم نحذف الأصول والمكونات الأصلية غير المستخدمة لأنها جزء من upstream وقد نحتاجها عند دمج تحديثات مستقبلية.
- icon النهائي لـAndroid/PWA سيشتق من نفس العلامة في المرحلة 07.

## معيار القبول

- Light/Dark يستخدمان نفس semantic token vocabulary.
- focus states واضحة على عناصر التفاعل.
- الصفحة الرئيسية والـWorkspace يحملان هوية moataz ai.
- الحركة تحترم `prefers-reduced-motion`.
- المصدر الأصلي لا يزال مذكورًا في About/Notices/README.

## التالي

**المرحلة 03 — العربية والإنجليزية + RTL/LTR**.
