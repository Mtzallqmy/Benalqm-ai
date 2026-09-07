# المرحلة 06 — التشغيل المحلي المتكامل

**الحالة: مكتملة**

## ما تم إنجازه

- إضافة Docker Compose overlay خاص بـmoataz ai بدل نسخ compose upstream بالكامل.
- إبقاء Nginx كنقطة دخول موحدة Web/Android.
- إضافة health checks للـfrontend وNginx فوق checks الموجودة أصلًا للGateway/Redis/Provisioner.
- تثبيت `cryptography==50.0.1` بصورة reproducible داخل build بعد `uv sync --locked` مع إبقاء upstream lockfile كما هو.
- تمرير `MOATAZ_PROVIDER_MASTER_KEY` و`MOATAZ_PROVIDER_NETWORK_POLICY` إلى Gateway.
- استخدام `host.docker.internal:host-gateway` الموجود أصلًا والمتوافق مع Linux؛ Docker Desktop يغطي Windows/macOS.
- إضافة `.env.moataz.example` منفصل حتى لا تختلط product secrets بملفات upstream.
- إضافة launch wrappers لـBash وPowerShell.
- إضافة local doctor لفحص UI/Gateway/readiness/provider.
- دليل عربي يغطي Ollama وLM Studio وvLLM وOpenAI-compatible LAN وAndroid على نفس الشبكة.

## ملاحظة الشبكة

- `BIND_HOST=127.0.0.1`: الجهاز المحلي فقط.
- `BIND_HOST=0.0.0.0`: وصول LAN، ويجب ضبط firewall.
- الهاتف يتصل بـNginx فقط؛ الاتصال بالمزود المحلي يتم من Gateway، لذلك لا نكرر provider logic في Android.

## معيار القبول

- التشغيل عبر base compose + moataz overlay موثق في أمر واحد.
- health/readiness قابلة للفحص دون حزم Python إضافية.
- المزودات المحلية لها مسار واضح عبر host/LAN.
- master key لا يدخل قاعدة البيانات أو provider registry.
- Windows/macOS/Linux لديهم تعليمات تشغيل ومسار host واضح.

## التالي

**المرحلة 07 — PWA + Android Trusted Web Activity foundation**.
