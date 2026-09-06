"use client";

import { CheckCircle2, CircleAlert, KeyRound, Loader2, PlugZap, RefreshCw, Server, ShieldCheck, Trash2 } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useI18n } from "@/core/i18n/hooks";
import {
  deleteProvider,
  discoverProviderModels,
  listProviders,
  probeProviderModel,
  saveProvider,
  setDefaultProviderModel,
  testProvider,
  type DiscoveredModel,
  type ProviderSummary,
  type ProviderType,
} from "@/core/providers/api";

const presets: { value: ProviderType; label: string; base?: string; local?: boolean }[] = [
  { value: "openai", label: "OpenAI", base: "https://api.openai.com/v1" },
  { value: "openrouter", label: "OpenRouter", base: "https://openrouter.ai/api/v1" },
  { value: "openai_compatible", label: "OpenAI-compatible" },
  { value: "ollama", label: "Ollama", base: "http://host.docker.internal:11434", local: true },
  { value: "vllm", label: "vLLM", local: true },
  { value: "lmstudio", label: "LM Studio", base: "http://host.docker.internal:1234/v1", local: true },
  { value: "litellm", label: "LiteLLM gateway" },
];

const copy = {
  ar: {
    title: "مزودو ونماذج الذكاء",
    description: "أضف مفتاحك أو endpoint متوافقًا، اختبر الاتصال، اكتشف النماذج ثم فعّل ما تريد استخدامه.",
    providerType: "نوع المزود",
    name: "اسم الاتصال",
    baseUrl: "Base URL",
    apiKey: "API Key",
    keyHint: "بعد الحفظ لن يعيد الخادم المفتاح كاملًا إلى المتصفح.",
    test: "اختبار الاتصال",
    discover: "اكتشاف النماذج",
    probe: "اختبار القدرات",
    save: "حفظ النماذج المحددة",
    saving: "جارٍ الحفظ...",
    select: "حدد النماذج التي تريد إتاحتها",
    default: "الافتراضي",
    saved: "الاتصالات المحفوظة",
    none: "لا توجد اتصالات ديناميكية محفوظة بعد.",
    localPolicy: "المزودات المحلية تتطلب MOATAZ_PROVIDER_NETWORK_POLICY=local على الخادم.",
    encryption: "تخزين المفاتيح يتطلب MOATAZ_PROVIDER_MASTER_KEY، ولا يوجد fallback إلى plaintext.",
  },
  en: {
    title: "AI providers & models",
    description: "Bring your own key or compatible endpoint, test it, discover models, then enable the ones you want to use.",
    providerType: "Provider type",
    name: "Connection name",
    baseUrl: "Base URL",
    apiKey: "API Key",
    keyHint: "After saving, the server never returns the full key to the browser.",
    test: "Test connection",
    discover: "Discover models",
    probe: "Probe capabilities",
    save: "Save selected models",
    saving: "Saving...",
    select: "Select models to make available",
    default: "Default",
    saved: "Saved connections",
    none: "No dynamic provider connections saved yet.",
    localPolicy: "Local providers require MOATAZ_PROVIDER_NETWORK_POLICY=local on the server.",
    encryption: "Credential storage requires MOATAZ_PROVIDER_MASTER_KEY and never falls back to plaintext.",
  },
};

export default function ProviderManagerPage() {
  const { locale } = useI18n();
  const c = locale === "ar-SA" ? copy.ar : copy.en;
  const [providerType, setProviderType] = useState<ProviderType>("openai");
  const selectedPreset = useMemo(() => presets.find((item) => item.value === providerType)!, [providerType]);
  const [name, setName] = useState("OpenAI");
  const [baseUrl, setBaseUrl] = useState(selectedPreset.base ?? "");
  const [apiKey, setApiKey] = useState("");
  const [models, setModels] = useState<DiscoveredModel[]>([]);
  const [selectedModels, setSelectedModels] = useState<Set<string>>(new Set());
  const [defaultModel, setDefaultModel] = useState<string | null>(null);
  const [providers, setProviders] = useState<ProviderSummary[]>([]);
  const [busy, setBusy] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try { setProviders(await listProviders()); } catch (error) { toast.error(error instanceof Error ? error.message : "Failed to load providers"); }
  }, []);

  useEffect(() => { void refresh(); }, [refresh]);

  const draft = useMemo(() => ({ provider_type: providerType, base_url: baseUrl || undefined, api_key: apiKey || undefined }), [apiKey, baseUrl, providerType]);

  const changeType = (value: ProviderType) => {
    const preset = presets.find((item) => item.value === value)!;
    setProviderType(value);
    setBaseUrl(preset.base ?? "");
    setName(preset.label);
    setModels([]);
    setSelectedModels(new Set());
    setDefaultModel(null);
  };

  const handleTest = async () => {
    setBusy("test");
    try { const result = await testProvider(draft); toast.success(`${c.test}: ${result.model_count} models`); } catch (error) { toast.error(error instanceof Error ? error.message : "Connection failed"); } finally { setBusy(null); }
  };

  const handleDiscover = async () => {
    setBusy("discover");
    try {
      const discovered = await discoverProviderModels(draft);
      setModels(discovered);
      setSelectedModels(new Set(discovered.slice(0, 1).map((model) => model.model)));
      setDefaultModel(discovered[0]?.model ?? null);
      toast.success(`${discovered.length} models`);
    } catch (error) { toast.error(error instanceof Error ? error.message : "Discovery failed"); } finally { setBusy(null); }
  };

  const handleProbe = async (model: string) => {
    setBusy(`probe:${model}`);
    try { await probeProviderModel(draft, model); toast.success(`${model}: chat OK`); } catch (error) { toast.error(error instanceof Error ? error.message : "Probe failed"); } finally { setBusy(null); }
  };

  const handleSave = async () => {
    const chosen = models.filter((model) => selectedModels.has(model.model));
    if (!chosen.length) return;
    setBusy("save");
    try {
      const saved = await saveProvider({ ...draft, name, models: chosen });
      const target = saved.models.find((model) => model.model === defaultModel)?.registry_name;
      if (target) await setDefaultProviderModel(target);
      setApiKey("");
      await refresh();
      toast.success(`${saved.name}: ${saved.models.length} models saved`);
    } catch (error) { toast.error(error instanceof Error ? error.message : "Save failed"); } finally { setBusy(null); }
  };

  return (
    <main className="h-full w-full overflow-y-auto px-4 py-5 sm:px-6 sm:py-8">
      <div className="mx-auto max-w-6xl space-y-6 pb-8">
        <header className="space-y-2"><div className="flex items-center gap-3"><div className="grid size-11 place-items-center rounded-2xl bg-primary/10 text-primary"><PlugZap className="size-5" /></div><div><h1 className="text-2xl font-semibold tracking-[-0.03em]">{c.title}</h1><p className="text-muted-foreground mt-1 max-w-3xl text-sm leading-6">{c.description}</p></div></div></header>

        <section data-moataz-surface="glass" className="grid gap-5 rounded-3xl p-4 sm:p-6 lg:grid-cols-[.85fr_1.15fr]">
          <div className="space-y-4">
            <label className="block space-y-1.5 text-sm font-medium"><span>{c.providerType}</span><select value={providerType} onChange={(e) => changeType(e.target.value as ProviderType)} className="border-input bg-background h-10 w-full rounded-xl border px-3 text-sm">{presets.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
            <label className="block space-y-1.5 text-sm font-medium"><span>{c.name}</span><Input value={name} onChange={(e) => setName(e.target.value)} /></label>
            <label className="block space-y-1.5 text-sm font-medium"><span>{c.baseUrl}</span><Input value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)} placeholder="https://.../v1" data-ltr /></label>
            <label className="block space-y-1.5 text-sm font-medium"><span>{c.apiKey}</span><Input type="password" value={apiKey} onChange={(e) => setApiKey(e.target.value)} autoComplete="off" data-ltr /><span className="text-muted-foreground block text-xs font-normal">{c.keyHint}</span></label>
            {selectedPreset.local && <p className="flex gap-2 rounded-2xl border border-amber-500/20 bg-amber-500/8 p-3 text-xs leading-5 text-amber-700 dark:text-amber-300"><CircleAlert className="mt-0.5 size-4 shrink-0" />{c.localPolicy}</p>}
            <p className="flex gap-2 rounded-2xl border border-emerald-500/20 bg-emerald-500/8 p-3 text-xs leading-5 text-emerald-700 dark:text-emerald-300"><ShieldCheck className="mt-0.5 size-4 shrink-0" />{c.encryption}</p>
            <div className="flex flex-wrap gap-2"><Button variant="outline" onClick={handleTest} disabled={!!busy}>{busy === "test" ? <Loader2 className="size-4 animate-spin" /> : <Server className="size-4" />}{c.test}</Button><Button onClick={handleDiscover} disabled={!!busy}>{busy === "discover" ? <Loader2 className="size-4 animate-spin" /> : <RefreshCw className="size-4" />}{c.discover}</Button></div>
          </div>

          <div className="min-h-64 rounded-3xl border border-border/70 bg-background/55 p-3 sm:p-4">
            <div className="mb-3 flex items-center justify-between"><h2 className="text-sm font-semibold">{c.select}</h2><span className="text-muted-foreground text-xs">{selectedModels.size}/{models.length}</span></div>
            <div className="max-h-[30rem] space-y-2 overflow-y-auto pe-1">
              {models.map((model) => {
                const checked = selectedModels.has(model.model);
                return <div key={model.model} className={`rounded-2xl border p-3 transition ${checked ? "border-primary/40 bg-primary/6" : "border-border/60"}`}><div className="flex items-start gap-3"><input type="checkbox" checked={checked} onChange={(e) => setSelectedModels((current) => { const next = new Set(current); e.target.checked ? next.add(model.model) : next.delete(model.model); return next; })} className="mt-1 size-4" /><div className="min-w-0 flex-1"><p className="truncate text-sm font-medium ltr-isolate" title={model.model}>{model.display_name ?? model.model}</p><div className="mt-2 flex flex-wrap gap-1.5 text-[10px] text-muted-foreground">{model.supports_tools && <span className="rounded-full bg-muted px-2 py-1">tools</span>}{model.supports_streaming && <span className="rounded-full bg-muted px-2 py-1">stream</span>}{model.supports_thinking && <span className="rounded-full bg-muted px-2 py-1">reasoning</span>}{model.supports_vision && <span className="rounded-full bg-muted px-2 py-1">vision</span>}{model.context_window && <span className="rounded-full bg-muted px-2 py-1">{model.context_window.toLocaleString()} ctx</span>}</div></div><button type="button" disabled={!!busy} onClick={() => void handleProbe(model.model)} className="rounded-xl border px-2.5 py-1.5 text-xs hover:bg-muted">{busy === `probe:${model.model}` ? <Loader2 className="size-3 animate-spin" /> : c.probe}</button></div>{checked && <label className="mt-3 flex items-center gap-2 text-xs"><input type="radio" name="default-model" checked={defaultModel === model.model} onChange={() => setDefaultModel(model.model)} />{c.default}</label>}</div>;
              })}
              {!models.length && <div className="grid min-h-52 place-items-center text-center text-sm text-muted-foreground"><div><KeyRound className="mx-auto mb-3 size-7 opacity-50" /><p>{c.discover}</p></div></div>}
            </div>
            <Button className="mt-4 w-full" onClick={handleSave} disabled={!selectedModels.size || !!busy}>{busy === "save" ? <><Loader2 className="size-4 animate-spin" />{c.saving}</> : <><CheckCircle2 className="size-4" />{c.save}</>}</Button>
          </div>
        </section>

        <section className="space-y-3"><div className="flex items-center justify-between"><h2 className="text-lg font-semibold">{c.saved}</h2><Button variant="ghost" size="sm" onClick={() => void refresh()}><RefreshCw className="size-4" /></Button></div>{!providers.length && <div className="rounded-3xl border border-dashed p-8 text-center text-sm text-muted-foreground">{c.none}</div>}<div className="grid gap-3 md:grid-cols-2">{providers.map((provider) => <article key={provider.id} className="rounded-3xl border border-border/70 bg-card/70 p-4"><div className="flex items-start gap-3"><div className="grid size-10 place-items-center rounded-2xl bg-primary/10 text-primary"><Server className="size-4" /></div><div className="min-w-0 flex-1"><h3 className="font-semibold">{provider.name}</h3><p className="text-muted-foreground mt-1 truncate text-xs ltr-isolate">{provider.base_url}</p><p className="text-muted-foreground mt-2 text-xs">{provider.models.length} models · key {provider.has_api_key ? "••••••••" : "—"}</p></div><Button variant="ghost" size="icon" aria-label="Delete provider" onClick={async () => { try { await deleteProvider(provider.id); await refresh(); } catch (error) { toast.error(error instanceof Error ? error.message : "Delete failed"); } }}><Trash2 className="size-4" /></Button></div></article>)}</div></section>
      </div>
    </main>
  );
}
