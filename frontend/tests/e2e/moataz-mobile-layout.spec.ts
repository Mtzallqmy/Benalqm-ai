import { expect, test } from "@playwright/test";

const androidViewports = [
  { name: "compact", width: 360, height: 800 },
  { name: "standard", width: 412, height: 915 },
];

for (const viewport of androidViewports) {
  test(`landing has no horizontal overflow on Android ${viewport.name}`, async ({ page }) => {
    await page.setViewportSize({ width: viewport.width, height: viewport.height });
    await page.goto("/");
    await expect(page.getByLabel(/English|العربية/)).toBeVisible();
    const dimensions = await page.evaluate(() => ({
      clientWidth: document.documentElement.clientWidth,
      scrollWidth: document.documentElement.scrollWidth,
    }));
    expect(dimensions.scrollWidth).toBeLessThanOrEqual(dimensions.clientWidth + 1);
  });
}
