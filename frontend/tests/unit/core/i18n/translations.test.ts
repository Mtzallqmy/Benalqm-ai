import { describe, expect, it } from "@rstest/core";

import { getLocaleDirection, normalizeLocale } from "@/core/i18n/locale";
import { loadTranslations } from "@/core/i18n/translations";

describe("moataz ai localization", () => {
  it("loads Arabic and English product copy without losing upstream Chinese", async () => {
    const [arabic, english, chinese] = await Promise.all([
      loadTranslations("ar-SA"),
      loadTranslations("en-US"),
      loadTranslations("zh-CN"),
    ]);
    expect(arabic.sidebar.newChat).toBe("محادثة جديدة");
    expect(arabic.pages.appName).toBe("moataz ai");
    expect(english.inputBox.disclaimer).toContain("moataz ai");
    expect(chinese.inputBox.disclaimer).toBe("内容由AI生成，重要信息请务必核查");
  });

  it("normalizes Arabic locales and returns the correct writing direction", () => {
    expect(normalizeLocale("ar-YE")).toBe("ar-SA");
    expect(getLocaleDirection("ar-SA")).toBe("rtl");
    expect(getLocaleDirection("en-US")).toBe("ltr");
  });
});
