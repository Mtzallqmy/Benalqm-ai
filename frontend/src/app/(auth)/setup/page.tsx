"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MoatazAuthScene } from "@/components/auth/moataz-auth-scene";
import { RememberSessionOption } from "@/components/auth/remember-session-option";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { getCsrfHeaders } from "@/core/api/fetcher";
import { useAuth } from "@/core/auth/AuthProvider";
import { loadRememberLoginPreference } from "@/core/auth/remember-login";
import { fetchSetupStatus, isSystemAlreadyInitializedError } from "@/core/auth/setup";
import { parseAuthError } from "@/core/auth/types";
import { useI18n } from "@/core/i18n/hooks";
import { getProductCopy } from "@/core/i18n/product-copy";

type SetupMode = "loading" | "init_admin" | "change_password" | "unavailable";

export default function SetupPage() {
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();
  const { t, locale } = useI18n();
  const copy = getProductCopy(locale).setup;
  const [mode, setMode] = useState<SetupMode>("loading");
  const [setupStatusAttempt, setSetupStatusAttempt] = useState(0);
  const [email, setEmail] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [rememberMe, setRememberMe] = useState(() => loadRememberLoginPreference().rememberMe);
  const [currentPassword, setCurrentPassword] = useState("");

  useEffect(() => {
    let cancelled = false;
    if (isAuthenticated && user?.needs_setup) setMode("change_password");
    else if (!isAuthenticated) {
      setMode("loading");
      void fetchSetupStatus().then((data: { needs_setup?: boolean }) => {
        if (cancelled) return;
        if (data.needs_setup) setMode("init_admin"); else router.replace("/login");
      }).catch(() => { if (!cancelled) setMode("unavailable"); });
    } else router.replace("/workspace");
    return () => { cancelled = true; };
  }, [isAuthenticated, user, router, setupStatusAttempt]);

  const handleInitAdmin = async (e: React.SubmitEvent) => {
    e.preventDefault(); setError("");
    if (newPassword !== confirmPassword) { setError(copy.passwordsMismatch); return; }
    setLoading(true);
    try {
      const res = await fetch("/api/v1/auth/initialize", { method: "POST", headers: { "Content-Type": "application/json" }, credentials: "include", body: JSON.stringify({ email, password: newPassword, remember_me: rememberMe }) });
      if (!res.ok) {
        const data = await res.json();
        if (isSystemAlreadyInitializedError(data)) { router.replace("/login"); return; }
        setError(parseAuthError(data).message); return;
      }
      router.push("/workspace");
    } catch { setError(copy.networkError); } finally { setLoading(false); }
  };

  const handleChangePassword = async (e: React.SubmitEvent) => {
    e.preventDefault(); setError("");
    if (newPassword !== confirmPassword) { setError(copy.passwordsMismatch); return; }
    if (newPassword.length < 8) { setError(copy.passwordTooShort); return; }
    setLoading(true);
    try {
      const res = await fetch("/api/v1/auth/change-password", { method: "POST", headers: { "Content-Type": "application/json", ...getCsrfHeaders() }, credentials: "include", body: JSON.stringify({ current_password: currentPassword, new_password: newPassword, new_email: email || undefined, remember_me: rememberMe }) });
      if (!res.ok) { setError(parseAuthError(await res.json()).message); return; }
      router.push("/workspace");
    } catch { setError(copy.networkError); } finally { setLoading(false); }
  };

  if (mode === "loading") return <div className="flex min-h-screen items-center justify-center"><p className="text-muted-foreground text-sm">{copy.loading}</p></div>;
  if (mode === "unavailable") return (
    <div className="flex min-h-screen items-center justify-center px-4"><div className="w-full max-w-md space-y-4 text-center"><div><h1 className="text-xl font-semibold">{t.login.serviceUnavailableTitle}</h1><p className="text-muted-foreground mt-2 text-sm">{t.login.serviceUnavailableDescription}</p></div><div className="flex justify-center gap-3"><Button type="button" onClick={() => { setMode("loading"); setSetupStatusAttempt((attempt) => attempt + 1); }}>{t.login.retry}</Button><Button type="button" variant="outline" onClick={() => router.replace("/login")}>{t.login.signIn}</Button></div></div></div>
  );

  if (mode === "init_admin") return (
    <MoatazAuthScene title={copy.createAdmin} description={copy.createAdminDescription}>
      <form onSubmit={handleInitAdmin} className="space-y-3">
        <div className="flex flex-col space-y-1.5"><label htmlFor="email" className="text-sm font-medium">{copy.email}</label><Input id="email" type="email" placeholder="you@example.com" value={email} onChange={(e) => setEmail(e.target.value)} required data-ltr /></div>
        <div className="flex flex-col space-y-1.5"><label htmlFor="password" className="text-sm font-medium">{copy.password}</label><Input id="password" type="password" placeholder="••••••••" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={8} data-ltr /></div>
        <div className="flex flex-col space-y-1.5"><label htmlFor="confirmPassword" className="text-sm font-medium">{copy.confirmPassword}</label><Input id="confirmPassword" type="password" placeholder="••••••••" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required minLength={8} data-ltr /></div>
        <RememberSessionOption checked={rememberMe} onCheckedChange={setRememberMe} />
        {error && <p className="text-sm text-red-500">{error}</p>}
        <Button type="submit" className="w-full" disabled={loading}>{loading ? copy.creating : copy.create}</Button>
      </form>
    </MoatazAuthScene>
  );

  return (
    <MoatazAuthScene title={copy.completeAdmin} description={copy.completeAdminDescription}>
      <form onSubmit={handleChangePassword} className="space-y-3">
        <Input type="email" placeholder={copy.yourEmail} value={email} onChange={(e) => setEmail(e.target.value)} required data-ltr />
        <Input type="password" placeholder={copy.currentPassword} value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required data-ltr />
        <Input type="password" placeholder={copy.newPassword} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={8} data-ltr />
        <Input type="password" placeholder={copy.confirmPassword} value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required minLength={8} data-ltr />
        <RememberSessionOption checked={rememberMe} onCheckedChange={setRememberMe} />
        {error && <p className="text-sm text-red-500">{error}</p>}
        <Button type="submit" className="w-full" disabled={loading}>{loading ? copy.settingUp : copy.complete}</Button>
      </form>
    </MoatazAuthScene>
  );
}
