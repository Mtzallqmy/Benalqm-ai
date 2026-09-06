import { enUS } from "./en-US";
import type { Translations } from "./types";

/**
 * Product-level English copy. We intentionally derive from upstream en-US so
 * new DeerFlow keys remain available when upstream adds capabilities, while
 * visible product branding stays moataz ai.
 */
export const enMoataz: Translations = {
  ...enUS,
  locale: { localName: "English" },
  welcome: {
    ...enUS.welcome,
    greeting: "Welcome back",
    description:
      "Welcome to moataz ai — a bilingual agent workspace for research, execution, files, skills, memory and complex multi-step work.",
    createYourOwnSkillDescription:
      "Create a custom skill to extend what moataz ai can research, analyze, automate and produce.",
  },
  inputBox: {
    ...enUS.inputBox,
    placeholder: "What would you like moataz ai to work on?",
    disclaimer: "moataz ai uses AI and can make mistakes — verify important results",
    voiceInputStart:
      "Dictate with voice. moataz ai receives only transcribed text; audio is handled by your browser or system speech service.",
  },
  backgroundTasks: {
    ...enUS.backgroundTasks,
    cancellationRetrying: (attempt) =>
      `Cancellation attempt ${attempt} failed; moataz ai will keep retrying.`,
    notificationRetrying: (attempt) =>
      `Chat notification attempt ${attempt} failed; moataz ai will retry with backoff.`,
    trackingDegraded: "Status checks are delayed; moataz ai is still retrying.",
  },
  agents: {
    ...enUS.agents,
    saveRequested:
      "Save requested. moataz ai is generating and saving an initial version now.",
    agentCreatedPendingRefresh:
      "The agent was created, but moataz ai could not load it yet. Refresh this page in a moment.",
  },
  workspace: {
    ...enUS.workspace,
    officialWebsite: "moataz ai home",
    githubTooltip: "moataz ai on GitHub",
    visitGithub: "Open project on GitHub",
    about: "About moataz ai",
  },
  channels: {
    ...enUS.channels,
    descriptions: {
      ...enUS.channels.descriptions,
      buzz: "Buzz channels and direct messages through your moataz ai agent.",
      telegram: "Telegram direct messages through your moataz ai bot.",
      discord: "Discord server messages through your moataz ai bot.",
      feishu: "Feishu and Lark messages through your moataz ai app.",
      dingtalk: "DingTalk Stream Push messages through your moataz ai bot.",
      wechat: "WeChat iLink messages through your moataz ai bot.",
      wecom: "WeCom messages through your moataz ai bot.",
    },
  },
  pages: {
    ...enUS.pages,
    appName: "moataz ai",
  },
  toolCalls: {
    ...enUS.toolCalls,
    skillInstallTooltip: "Install skill and make it available to moataz ai",
  },
  shortcuts: {
    ...enUS.shortcuts,
    keyboardShortcutsDescription:
      "Navigate moataz ai faster with keyboard shortcuts.",
  },
  settings: {
    ...enUS.settings,
    description: "Adjust how moataz ai looks and behaves for you.",
    memory: {
      ...enUS.settings.memory,
      description:
        "moataz ai can learn useful context from conversations in the background. Review and control remembered information here.",
    },
    channels: {
      ...enUS.settings.channels,
      description:
        "Connect messaging accounts that can send messages to moataz ai from outside the browser.",
    },
    skills: {
      ...enUS.settings.skills,
      emptyDescription:
        "Put custom skill folders under `/skills/custom` in the moataz ai repository root.",
    },
    notification: {
      ...enUS.settings.notification,
      description:
        "moataz ai sends completion notifications when the window is inactive, which is useful for long-running tasks.",
      testTitle: "moataz ai",
    },
    account: {
      ...enUS.settings.account,
      ssoPasswordMessage:
        "This account signs in with {provider}, so moataz ai cannot manage its password here. Use your SSO provider settings instead.",
    },
  },
  login: {
    ...enUS.login,
    rememberMeDescription:
      "Keep this browser session when possible. moataz ai stores only your email, never your password.",
    adminSetupRequiredDescription:
      "moataz ai needs an administrator account before regular accounts can be created.",
  },
};
