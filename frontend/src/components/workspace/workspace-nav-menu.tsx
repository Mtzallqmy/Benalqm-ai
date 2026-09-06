"use client";

import { BugIcon, ChevronsUpDown, GlobeIcon, InfoIcon, PlugZapIcon, Settings2Icon, SettingsIcon } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";

import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { SidebarMenu, SidebarMenuButton, SidebarMenuItem, useSidebar } from "@/components/ui/sidebar";
import { useI18n } from "@/core/i18n/hooks";

import { GithubIcon } from "./github-icon";
import { useSettingsDialog } from "./settings";

function NavMenuButtonContent({ isSidebarOpen, t }: { isSidebarOpen: boolean; t: ReturnType<typeof useI18n>["t"] }) {
  return isSidebarOpen ? <div className="text-muted-foreground flex w-full items-center gap-2 text-start text-sm"><SettingsIcon className="size-4" /><span>{t.workspace.settingsAndMore}</span><ChevronsUpDown className="text-muted-foreground ms-auto size-4" /></div> : <div className="flex size-full items-center justify-center"><SettingsIcon className="text-muted-foreground size-4" /></div>;
}

export function WorkspaceNavMenu() {
  const { openSettings } = useSettingsDialog();
  const [mounted, setMounted] = useState(false);
  const { open: isSidebarOpen } = useSidebar();
  const { t, locale } = useI18n();
  useEffect(() => setMounted(true), []);
  const providersLabel = locale === "ar-SA" ? "مزودو الذكاء والنماذج" : "AI providers & models";

  return <SidebarMenu className="w-full"><SidebarMenuItem>{mounted ? <DropdownMenu><DropdownMenuTrigger asChild><SidebarMenuButton size="lg" className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"><NavMenuButtonContent isSidebarOpen={isSidebarOpen} t={t} /></SidebarMenuButton></DropdownMenuTrigger><DropdownMenuContent className="w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg" align="end" sideOffset={4}><DropdownMenuGroup><DropdownMenuItem onClick={() => openSettings("appearance")}><Settings2Icon />{t.common.settings}</DropdownMenuItem><DropdownMenuItem asChild><Link href="/workspace/providers"><PlugZapIcon />{providersLabel}</Link></DropdownMenuItem><DropdownMenuSeparator /><a href="/" target="_blank" rel="noopener noreferrer"><DropdownMenuItem><GlobeIcon />{t.workspace.officialWebsite}</DropdownMenuItem></a><a href="https://github.com/Mtzallqmy/Benalqm-ai" target="_blank" rel="noopener noreferrer"><DropdownMenuItem><GithubIcon />{t.workspace.visitGithub}</DropdownMenuItem></a><a href="https://github.com/Mtzallqmy/Benalqm-ai/issues" target="_blank" rel="noopener noreferrer"><DropdownMenuItem><BugIcon />{t.workspace.reportIssue}</DropdownMenuItem></a></DropdownMenuGroup><DropdownMenuSeparator /><DropdownMenuItem onClick={() => openSettings("about")}><InfoIcon />{t.workspace.about}</DropdownMenuItem></DropdownMenuContent></DropdownMenu> : <SidebarMenuButton size="lg" className="pointer-events-none"><NavMenuButtonContent isSidebarOpen={isSidebarOpen} t={t} /></SidebarMenuButton>}</SidebarMenuItem></SidebarMenu>;
}
