import { MoatazLanding } from "@/components/landing/moataz-landing";
import { I18nProvider } from "@/core/i18n/context";
import { detectLocaleServer } from "@/core/i18n/server";

export const dynamic = "force-dynamic";

export default async function LandingPage() {
  const locale = await detectLocaleServer();
  return (
    <I18nProvider initialLocale={locale}>
      <MoatazLanding />
    </I18nProvider>
  );
}
