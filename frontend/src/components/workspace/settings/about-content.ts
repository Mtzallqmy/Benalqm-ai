/**
 * moataz ai About content.
 *
 * Keep upstream attribution visible here as part of the product UI. Full
 * locale-specific About content will move into i18n during the localization
 * phase.
 */
import { MOATAZ_AI_BRAND } from "@/brand";
import { APP_VERSION } from "@/version";

const { upstream } = MOATAZ_AI_BRAND;

export const aboutMarkdown = `# moataz ai ${APP_VERSION}

> **${MOATAZ_AI_BRAND.tagline.en}**  
> **${MOATAZ_AI_BRAND.tagline.ar}**

**moataz ai** is an independent product experience built on top of the open-source DeerFlow agent harness while preserving its agent runtime, extensible skills, tools, memory, sandbox, and sub-agent architecture.

---

## What moataz ai adds

- A distinct moataz ai visual identity and design system.
- Arabic and English product experiences with RTL/LTR support.
- Mobile-first navigation and an Android application experience.
- Dynamic AI provider management for local and hosted models.
- A simplified local deployment experience without removing DeerFlow capabilities.

---

## Open-source foundation / المصدر المفتوح

A substantial part of the agent foundation in this application comes from **${upstream.name}** by **${upstream.organization}** and the DeerFlow Authors.

جزء جوهري من نواة الوكلاء في **moataz ai** مأخوذ ومطوّر مباشرة من مشروع **DeerFlow** مفتوح المصدر، مع الحفاظ على إشعارات المصدر والترخيص.

- **Source repository:** [github.com/bytedance/deer-flow](${upstream.repository})
- **Baseline version:** ${upstream.baselineVersion}
- **Pinned source commit:** [${upstream.baselineCommit}](${upstream.repository}/commit/${upstream.baselineCommit})
- **Upstream license:** ${upstream.license}
- **moataz ai repository:** [github.com/Mtzallqmy/Benalqm-ai](${MOATAZ_AI_BRAND.repository})

**moataz ai is not an official ByteDance product and this attribution does not imply endorsement or sponsorship by ByteDance or DeerFlow Authors.**

The original MIT license notice remains included in the repository. Some bundled skills include additional license files in their own directories; see \`THIRD_PARTY_NOTICES.md\` for the project-level notice.

---

## Core inherited capabilities

- **Skills & Tools** — extensible skills and tool execution.
- **Sub-Agents** — delegated and parallel agent work.
- **Sandbox & File System** — isolated execution and artifact/file workflows.
- **Context Engineering** — context isolation and compaction for long-running work.
- **Long-Term Memory** — supported memory backends and persisted user context.
- **MCP** — extensible external tool integrations.

---

## Product repository

[${MOATAZ_AI_BRAND.repository}](${MOATAZ_AI_BRAND.repository})

## License & notices

See the repository \`LICENSE\`, \`THIRD_PARTY_NOTICES.md\`, and \`docs/UPSTREAM_PROVENANCE.md\` files for source and licensing details.
`;
