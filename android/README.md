# moataz ai Android

طبقة Android تعيد استخدام نفس frontend/backend بدل إعادة كتابة DeerFlow. الحد الأدنى هو **Android 8.0 / API 26**، والهدف هو **API 36** وفق متطلبات Google Play اعتبارًا من 31 أغسطس 2026.

## السلوك

- عنوان HTTPS المطابق لـ`moatazTrustedOrigin` يفتح كـTrusted Web Activity.
- عنوان آخر أو LAN/HTTP يفتح داخل WebView fallback؛ يمكن ضبط الخادم من أول تشغيل أو من اختصار **إعدادات الخادم** عند الضغط المطوّل على الأيقونة.
- لا تُحفظ API keys في Android؛ التطبيق يخزن عنوان الخادم فقط، بينما جلسة الحساب تبقى في cookies الخاصة بالـTWA/WebView.
- رفع الملفات يستخدم Android file picker، والتنزيل يستخدم DownloadManager، والرجوع يستخدم سجل WebView/المتصفح الطبيعي.

## بناء محلي

يتطلب JDK 17 وAndroid SDK 36 وGradle 9.5+.

```bash
cd android
gradle :app:testReleaseUnitTest :app:assembleDebug
```

لبناء نسخة مرتبطة بدومين HTTPS نهائي:

```bash
gradle :app:assembleRelease \
  -PmoatazOrigin=https://app.example.com \
  -PmoatazTrustedOrigin=https://app.example.com
```

ثم اضبط في frontend:

```env
MOATAZ_ANDROID_PACKAGE_NAME=ai.moataz.app
MOATAZ_ANDROID_CERT_SHA256=AA:BB:...:FF
```

ليقدّم المسار `/.well-known/assetlinks.json` بيان Digital Asset Links المطابق لشهادة توقيع الـAPK.

> السماح بـHTTP داخل Android موجود لدعم مرحلة التشغيل المحلي على LAN فقط. الإنتاج العام يجب أن يستخدم HTTPS.
