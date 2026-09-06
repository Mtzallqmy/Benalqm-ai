"use client";

import { ArrowRight, FileStack, Languages, Orbit, ShieldCheck, Sparkles, Workflow } from "lucide-react";
import Link from "next/link";

import { MoatazBrand, MoatazMark } from "@/components/brand/moataz-logo";
import { Button } from "@/components/ui/button";
import { useI18n } from "@/core/i18n/hooks";
import { getProductCopy } from "@/core/i18n/product-copy";

const capabilityIcons = [Workflow, FileStack, Languages] as const;

export function MoatazLanding() {
  const { locale, changeLocale } = useI18n();
  const copy = getProductCopy(locale);
  const nextLocale = locale === "ar-SA" ? "en-US" : "ar-SA";

  return (
    <div className="relative min-h-screen overflow-hidden bg-background text-foreground">
      <div className="moataz-grid pointer-events-none absolute inset-0 opacity-45" />
      <div className="moataz-orb pointer-events-none absolute -top-40 left-1/2 h-[34rem] w-[34rem] -translate-x-1/2 rounded-full bg-[radial-gradient(circle,rgba(120,87,255,.30),rgba(37,208,232,.09)_42%,transparent_70%)] blur-3xl" />

      <header className="fixed inset-x-0 top-0 z-30 mx-auto flex h-18 max-w-6xl items-center justify-between px-5 md:px-8">
        <Link href="/" className="rounded-xl"><MoatazBrand showTagline /></Link>
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" onClick={() => changeLocale(nextLocale)} aria-label={copy.languageSwitch}>
            <Languages className="size-4" /><span className="hidden sm:inline">{copy.languageSwitch}</span>
          </Button>
          <Button variant="ghost" size="sm" asChild className="hidden md:inline-flex">
            <Link href="/workspace/settings/about">{copy.sourceLicense}</Link>
          </Button>
          <Button size="sm" asChild className="rounded-full px-4 shadow-[0_8px_28px_rgba(78,107,255,.22)]">
            <Link href="/workspace">{copy.openWorkspace} <ArrowRight className="rtl-flip size-4" /></Link>
          </Button>
        </div>
      </header>

      <main className="relative z-10 mx-auto flex min-h-screen max-w-6xl flex-col justify-center px-5 pt-28 pb-16 md:px-8">
        <section className="grid items-center gap-12 lg:grid-cols-[1.08fr_.92fr]">
          <div className="moataz-enter">
            <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/8 px-3 py-1.5 text-xs font-medium text-primary">
              <Sparkles className="size-3.5" />{copy.badge}
            </div>
            <h1 className="max-w-4xl text-5xl leading-[0.98] font-semibold tracking-[-0.055em] sm:text-6xl md:text-7xl">
              {copy.heroPrefix} <span className="moataz-gradient-text">{copy.heroHighlight}</span> {copy.heroSuffix}
            </h1>
            <p className="text-muted-foreground mt-7 max-w-2xl text-base leading-7 sm:text-lg">{copy.description}</p>
            <p className="text-muted-foreground mt-3 max-w-2xl text-sm leading-7 sm:text-base">{copy.secondaryDescription}</p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Button size="lg" asChild className="rounded-full px-6"><Link href="/workspace">{copy.startWorking} <ArrowRight className="rtl-flip size-4" /></Link></Button>
              <Button size="lg" variant="outline" asChild className="rounded-full border-border/80 bg-background/55 px-6 backdrop-blur"><Link href={locale === "ar-SA" ? "/en/docs" : "/en/docs"}>{copy.exploreDocs}</Link></Button>
            </div>
            <div className="text-muted-foreground mt-8 flex flex-wrap items-center gap-x-5 gap-y-2 text-xs">
              <span className="inline-flex items-center gap-1.5"><ShieldCheck className="size-4 text-emerald-500" />{copy.upstreamNotice}</span>
              <span className="inline-flex items-center gap-1.5"><Orbit className="size-4 text-primary" />{copy.runtimeNotice}</span>
            </div>
          </div>

          <div className="relative mx-auto w-full max-w-md lg:justify-self-end">
            <div data-moataz-surface="glass" className="relative overflow-hidden rounded-[2rem] p-5 md:p-6">
              <div className="absolute inset-x-10 top-0 h-px bg-gradient-to-r from-transparent via-primary/70 to-transparent" />
              <div className="mb-5 flex items-center justify-between">
                <MoatazBrand />
                <span className="rounded-full border border-emerald-500/25 bg-emerald-500/10 px-2.5 py-1 text-[10px] font-semibold text-emerald-600 dark:text-emerald-400">READY</span>
              </div>
              <div className="rounded-3xl border border-border/70 bg-background/65 p-5">
                <div className="flex items-start gap-4">
                  <div className="rounded-2xl bg-primary/10 p-2.5"><MoatazMark className="size-11" /></div>
                  <div className="min-w-0 flex-1"><p className="text-sm font-semibold">moataz ai</p><p className="text-muted-foreground mt-1 text-xs leading-5">agent workspace · MCP · skills · artifacts</p></div>
                </div>
                <div className="mt-5 space-y-2.5">
                  {copy.capabilities.map((item, index) => (
                    <div key={item.title} className="flex items-center gap-3 rounded-2xl border border-border/60 bg-card/70 px-3.5 py-3 text-xs">
                      <span className="grid size-6 place-items-center rounded-full bg-primary/10 font-mono text-[10px] font-semibold text-primary">0{index + 1}</span>
                      <span className="font-medium">{item.title}</span>
                      <span className="ms-auto size-1.5 rounded-full bg-emerald-400 shadow-[0_0_12px_rgba(85,230,181,.75)]" />
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="mt-16 grid gap-3 md:grid-cols-3">
          {copy.capabilities.map((item, index) => {
            const Icon = capabilityIcons[index] ?? Workflow;
            return (
              <article key={item.title} data-moataz-surface="glass" className="rounded-3xl p-5 transition-transform duration-300 hover:-translate-y-1">
                <div className="mb-4 grid size-10 place-items-center rounded-2xl bg-primary/10 text-primary"><Icon className="size-5" /></div>
                <h2 className="font-semibold tracking-[-0.02em]">{item.title}</h2>
                <p className="text-muted-foreground mt-2 text-sm leading-6">{item.body}</p>
              </article>
            );
          })}
        </section>
      </main>
    </div>
  );
}
