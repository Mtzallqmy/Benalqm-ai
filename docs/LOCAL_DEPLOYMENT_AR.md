# تشغيل moataz ai محليًا وعلى الشبكة الداخلية

هذا الدليل يضيف طبقة تشغيل خاصة بـ **moataz ai** فوق Docker Compose الأصلي، من دون تغيير أسماء متغيرات `DEER_FLOW_*` الداخلية حتى يبقى التوافق مع upstream محفوظًا.

## 1) المتطلبات

- Docker Desktop على Windows/macOS أو Docker Engine + Compose v2 على Linux.
- نسخة `config.yaml` و`extensions_config.json` وفق تعليمات المشروع الأصلية.
- لمزود محلي: Ollama أو LM Studio أو vLLM على الجهاز المضيف/LAN.

## 2) ملف البيئة

```bash
cp .env.moataz.example .env.moataz
python -c "import secrets; print(secrets.token_urlsafe(48))"
```

ضع الناتج في `MOATAZ_PROVIDER_MASTER_KEY`. لا تُعد استخدامه ككلمة مرور أخرى ولا ترفعه إلى Git.

للتشغيل المحلي نستخدم:

```env
MOATAZ_PROVIDER_NETWORK_POLICY=local
BIND_HOST=127.0.0.1
```

وللوصول من هاتف Android على نفس الشبكة:

```env
BIND_HOST=0.0.0.0
```

استخدم جدار حماية الجهاز، ولا تعرض المنفذ مباشرةً للإنترنت دون TLS وسياسة وصول مناسبة.

## 3) التشغيل

Linux/macOS:

```bash
./scripts/moataz-local.sh up -d --build
```

Windows PowerShell:

```powershell
./scripts/moataz-local.ps1 up -d --build
```

الواجهة الموحدة ستكون افتراضيًا على `http://127.0.0.1:2026`.

## 4) Ollama على الجهاز المضيف

شغل Ollama بصورة طبيعية على المضيف. من داخل Gateway استخدم preset **Ollama** في صفحة `/workspace/providers`؛ Docker يصل للمضيف عبر `host.docker.internal`، والـCompose الأصلي/overlay يضيف `host-gateway` على Linux كذلك.

للتأكد من المضيف نفسه:

```bash
python scripts/moataz-local-doctor.py --provider http://127.0.0.1:11434
```

إذا كان Ollama يستمع فقط على loopback ولم يستطع Container الوصول إليه، اضبط binding الخاص بـOllama بحسب نظام التشغيل وسياسة الشبكة لديك؛ لا تفتح الخدمة على LAN دون حاجة.

## 5) LM Studio

فعّل Local Server/OpenAI-compatible API في LM Studio. preset التطبيق يستخدم افتراضيًا:

```text
http://host.docker.internal:1234/v1
```

ثم Test → Discover → Probe → Save من واجهة المزودات.

## 6) vLLM

شغل vLLM على GPU host/server مع OpenAI-compatible API ثم أدخل URL مثل:

```text
http://host.docker.internal:8000/v1
```

أو عنوان خادم LAN. في local policy يُسمح بالعناوين الخاصة عمدًا؛ في hosted policy تُحظر.

## 7) مزود OpenAI-compatible على LAN

استخدم **OpenAI-compatible** وأدخل `http://IP:PORT/v1`. تأكد أن الخادم متاح من شبكة Docker، وأن الجدار الناري يسمح فقط للنطاق الداخلي المطلوب.

## 8) Android على LAN

اعرف IP جهاز الكمبيوتر، مثال `192.168.1.20`، ثم افتح على الهاتف:

```text
http://192.168.1.20:2026
```

لا يحتاج Android إلى معرفة عنوان Ollama/vLLM؛ الهاتف يتصل بـNginx فقط، والـGateway هو من يصل للمزود.

## 9) التشخيص

```bash
python scripts/moataz-local-doctor.py --base http://127.0.0.1:2026
```

يفحص Nginx/UI وGateway وreadiness. ويمكن إضافة `--provider` لفحص مزود محلي من المضيف.

ثم راجع:

```bash
./scripts/moataz-local.sh ps
./scripts/moataz-local.sh logs gateway
```

أخطاء provider نفسها تظهر في واجهة Provider Manager كـ401/403/404/429/timeout بدون إعادة API key في الرسالة.

## 10) ملاحظة التشفير

صورة Gateway الخاصة بالمشروع تثبّت `cryptography==50.0.1` بعد `uv sync --locked`. هذا متعمد: lockfile الأصلي لـDeerFlow يبقى كما هو، وإضافة المنتج تُثبت بصورة منفصلة وقابلة لإعادة الإنتاج داخل Docker.
