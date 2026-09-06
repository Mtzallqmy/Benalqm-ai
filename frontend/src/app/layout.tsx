import "@/styles/globals.css";
import "@/styles/moataz-ai.css";
import "@/styles/rtl.css";
import "@/styles/mobile-workspace.css";

import { type Metadata } from "next";

import { MOATAZ_AI_BRAND } from "@/brand";
import { ThemeProvider } from "@/components/theme-provider";
import { DEFAULT_LOCALE } from "@/core/i18n/locale";

export const metadata: Metadata = {
  title: {
    default: MOATAZ_AI_BRAND.name,
    template: `%s · ${MOATAZ_AI_BRAND.name}`,
  },
  description: MOATAZ_AI_BRAND.description.en,
  applicationName: MOATAZ_AI_BRAND.name,
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html
      lang={DEFAULT_LOCALE}
      dir="rtl"
      suppressContentEditableWarning
      suppressHydrationWarning
    >
      <body>
        <ThemeProvider attribute="class" enableSystem disableTransitionOnChange>
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}
