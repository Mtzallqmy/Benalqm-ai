"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";

import { MoatazAuthScene } from "@/components/auth/moataz-auth-scene";
import { RememberSessionOption } from "@/components/auth/remember-session-option";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/core/auth/AuthProvider";
import { resolveAuthNextPath } from "@/core/auth/next-path";
import { loadRememberLoginPreference, saveRememberLoginPreference } from "@/core/auth/remember-login";
import { canCreateRegularAccount, fetchSetupStatus, type SetupStatusResponse } from "@/core/auth/setup";
import { parseAuthError } from "@/core/auth/types";
import { useI18n } from "@/core/i18n/hooks";

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { isAuthenticated } = useAuth();
  const { t } = useI18n();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(true);
  const [isLogin, setIsLogin] = useState(true);
  const [ssoProviders, setSsoProviders] = useState<{ id: string; display_name: string; type: string }[]>([]);
  const [setupStatus, setSetupStatus] = useState<SetupStatusResponse | null>(null);
  const [setupStatusPhase, setSetupStatusPhase] = useState<"checking" | "ready" | "unavailable">("checking");
  const [setupStatusAttempt, setSetupStatusAttempt] = useState(0);
  const errorParam = searchParams.get("error");
  const [error, setError] = useState(errorParam ? (t.login.errors[errorParam as keyof typeof t.login.errors] ?? t.login.authFailed) : "");
  const [showSsoHint, setShowSsoHint] = useState(false);
  const [loading, setLoading] = useState(false);
  const nextParam = searchParams.get("next");
  const redirectPath = resolveAuthNextPath(nextParam);
  const regularSignupAllowed = canCreateRegularAccount({ checked: setupStatusPhase === "ready", status: setupStatus });
  const systemNeedsAdminSetup = setupStatus?.needs_setup === true;
  const showSetupStatusUnavailable = setupStatusPhase === "unavailable" || (setupStatusAttempt > 0 && setupStatusPhase === "checking");

  useEffect(() => { if (isAuthenticated) router.push(redirectPath); }, [isAuthenticated, redirectPath, router]);
  useEffect(() => {
    const preference = loadRememberLoginPreference();
    setRememberMe(preference.rememberMe);
    if (preference.email) setEmail(preference.email);
  }, []);
  useEffect(() => {
    let cancelled = false;
    setSetupStatusPhase("checking");
    void fetchSetupStatus().then((data) => {
      if (cancelled) return;
      setSetupStatus(data);
      setSetupStatusPhase("ready");
      if (data.needs_setup) setIsLogin(true);
    }).catch(() => { if (!cancelled) { setSetupStatus(null); setSetupStatusPhase("unavailable"); } });
    return () => { cancelled = true; };
  }, [setupStatusAttempt]);
  useEffect(() => {
    let cancelled = false;
    void fetch("/api/v1/auth/providers").then((r) => r.json()).then((data: { providers: { id: string; display_name: string; type: string }[] }) => {
      if (!cancelled) setSsoProviders(data.providers ?? []);
    }).catch(() => undefined);
    return () => { cancelled = true; };
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setShowSsoHint(false);
    setLoading(true);
    if (!isLogin && !regularSignupAllowed) {
      setError(t.login.adminSetupRequiredDescription);
      setLoading(false);
      return;
    }
    try {
      const endpoint = isLogin ? "/api/v1/auth/login/local" : "/api/v1/auth/register";
      const body = isLogin
        ? new URLSearchParams({ password, remember_me: String(rememberMe), username: email })
        : JSON.stringify({ email, password, remember_me: rememberMe });
      const headers: HeadersInit = isLogin ? { "Content-Type": "application/x-www-form-urlencoded" } : { "Content-Type": "application/json" };
      const res = await fetch(endpoint, { method: "POST", headers, body, credentials: "include" });
      if (!res.ok) {
        const data = await res.json();
        const authError = parseAuthError(data);
        setError(authError.message);
        if (isLogin && ssoProviders.length > 0) setShowSsoHint(true);
        return;
      }
      saveRememberLoginPreference({ email, rememberMe });
      router.push(redirectPath);
    } catch {
      setError(t.login.networkError);
    } finally {
      setLoading(false);
    }
  };

  return (
    <MoatazAuthScene title={isLogin ? t.login.signInTitle : t.login.createAccountTitle}>
      {showSetupStatusUnavailable && (
        <div role="status" aria-live="polite" className="rounded-2xl border border-amber-500/25 bg-amber-500/8 p-4 text-sm">
          <p className="font-medium">{t.login.serviceUnavailableTitle}</p>
          <p className="text-muted-foreground mt-1">{t.login.serviceUnavailableDescription}</p>
          <Button type="button" variant="outline" size="sm" className="mt-3" disabled={setupStatusPhase === "checking"} onClick={() => { setSetupStatusPhase("checking"); setSetupStatusAttempt((attempt) => attempt + 1); }}>
            {setupStatusPhase === "checking" ? t.login.pleaseWait : t.login.retry}
          </Button>
        </div>
      )}
      {systemNeedsAdminSetup && (
        <div className="rounded-2xl border border-primary/25 bg-primary/8 p-4 text-sm">
          <p className="font-medium">{t.login.adminSetupRequiredTitle}</p>
          <p className="text-muted-foreground mt-1">{t.login.adminSetupRequiredDescription}</p>
          <Link href="/setup" className="mt-2 inline-block font-medium text-primary hover:underline">{t.login.createAdminAccount}</Link>
        </div>
      )}
      <form onSubmit={handleSubmit} className="space-y-3">
        <div className="flex flex-col space-y-1.5"><label htmlFor="email" className="text-sm font-medium">{t.login.email}</label><Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder={t.login.emailPlaceholder} required data-ltr /></div>
        <div className="flex flex-col space-y-1.5"><label htmlFor="password" className="text-sm font-medium">{t.login.password}</label><Input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder={t.login.passwordPlaceholder} required minLength={isLogin ? 6 : 8} data-ltr /></div>
        <RememberSessionOption checked={rememberMe} onCheckedChange={setRememberMe} />
        {error && <p className="text-sm text-red-500">{error}</p>}
        <Button type="submit" className="w-full" disabled={loading}>{loading ? t.login.pleaseWait : isLogin ? t.login.signIn : t.login.createAccount}</Button>
      </form>
      {ssoProviders.length > 0 && (
        <div className="space-y-2">
          {isLogin && <div className="relative my-4"><div className="absolute inset-0 flex items-center"><span className="w-full border-t" /></div><div className="relative flex justify-center text-xs uppercase"><span className="bg-card px-2 text-muted-foreground">{t.login.orContinueWith}</span></div></div>}
          {showSsoHint && <p className="text-muted-foreground text-center text-sm">{t.login.ssoHint}</p>}
          {ssoProviders.map((provider) => <Button key={provider.id} type="button" variant="outline" className="w-full" disabled={loading} onClick={() => { window.location.href = `/api/v1/auth/oauth/${provider.id}?next=${encodeURIComponent(redirectPath)}&remember_me=${String(rememberMe)}`; }}>{t.login.continueWith(provider.display_name)}</Button>)}
        </div>
      )}
      {regularSignupAllowed && <div className="text-center text-sm"><button type="button" onClick={() => { setIsLogin(!isLogin); setError(""); setShowSsoHint(false); }} className="text-primary hover:underline">{isLogin ? t.login.noAccountSignUp : t.login.haveAccountSignIn}</button></div>}
      <div className="text-muted-foreground text-center text-xs"><Link href="/" className="hover:underline">{t.login.backToHome}</Link></div>
    </MoatazAuthScene>
  );
}
