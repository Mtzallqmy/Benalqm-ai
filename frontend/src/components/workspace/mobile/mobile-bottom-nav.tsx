"use client";

import { Bot, MessageSquare, Plus, Settings2, PlugZap } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

import { openSettingsDialog } from "@/components/workspace/settings";
import { useI18n } from "@/core/i18n/hooks";
import { cn } from "@/lib/utils";

const itemClass = "flex min-h-11 min-w-14 flex-col items-center justify-center gap-0.5 rounded-2xl px-2 text-[10px] font-medium transition-all active:scale-95";

export function MobileBottomNav() {
  const pathname = usePathname();
  const { t, locale } = useI18n();
  const providerLabel = locale === "ar-SA" ? "المزودون" : "Providers";
  const items = [
    { href: "/workspace/chats", label: t.sidebar.chats, icon: MessageSquare },
    { href: "/workspace/agents", label: t.sidebar.agents, icon: Bot },
    { href: "/workspace/chats/new", label: t.sidebar.newChat, icon: Plus, primary: true, exact: true },
    { href: "/workspace/providers", label: providerLabel, icon: PlugZap },
  ] as const;

  return (
    <nav aria-label="Mobile workspace navigation" data-moataz-mobile-nav className="fixed inset-x-3 bottom-[max(.65rem,env(safe-area-inset-bottom))] z-40 mx-auto flex max-w-md items-center justify-around rounded-[1.65rem] border border-border/70 bg-background/88 p-1.5 shadow-[0_18px_50px_rgba(8,11,20,.22)] backdrop-blur-2xl md:hidden">
      {items.map(({ href, label, icon: Icon, ...item }) => {
        const active = item.exact ? pathname === href : pathname === href || pathname.startsWith(`${href}/`);
        return <Link key={href} href={href} aria-current={active ? "page" : undefined} className={cn(itemClass, item.primary ? "bg-primary text-primary-foreground shadow-[0_8px_22px_rgba(78,107,255,.28)]" : active ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-muted hover:text-foreground")}><Icon className={cn("size-4", item.primary && "size-5")} /><span className="max-w-16 truncate">{label}</span></Link>;
      })}
      <button type="button" onClick={() => openSettingsDialog("appearance")} className={cn(itemClass, "text-muted-foreground hover:bg-muted hover:text-foreground")}><Settings2 className="size-4" /><span>{t.common.settings}</span></button>
    </nav>
  );
}
