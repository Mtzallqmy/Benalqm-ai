import "@/styles/globals.css";
import "@/styles/moataz-ai.css";
import "@/styles/rtl.css";
import "@/styles/mobile-workspace.css";

import { type Metadata, type Viewport } from "next";

import { MOATAZ_AI_BRAND } from "@/brand";
import { ServiceWorkerRegistration } from "@/components/pwa/service-worker-registration";
import { ThemeProvider } from "@/components/theme-provider";
import { DEFAULT_LOCALE } from "@/core/i18n/locale";

export const metadata: Metadata = {
  title: {
    default: MOATAZ_AI_BRAND.name,
    template: `%s · ${MOATAZ_AI_BRAND.name}`,
  },
  description: MOATAZ_AI_BRAND.description.en,
  applicationName: MOATAZ_AI_BRAND.name,
  manifest: "/manifest.webmanifest",
  icons: {
    icon: [
      { url: "/brand/moataz-192.png", sizes: "192x192", type: "image/png" },
      { url: "/brand/moataz-mark.svg", type: "image/svg+xml" },
    ],
    apple: [{ url: "/brand/moataz-192.png", sizes: "192x192" }],
  },
  appleWebApp: {
    capable: true,
    statusBarStyle: "black-translucent",
    title: MOATAZ_AI_BRAND.name,
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#F7F8FC" },
    { media: "(prefers-color-scheme: dark)", color: "#080B14" },
  ],
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
          <ServiceWorkerRegistration />
        </ThemeProvider>
      </body>
    </html>
  );
}
