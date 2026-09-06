export const MOATAZ_AI_THEME = {
  concept: "midnight intelligence with luminous orbital accents",
  color: {
    ink: "#080B14",
    surface: "#0E1322",
    surfaceElevated: "#151B2E",
    paper: "#F7F8FC",
    paperElevated: "#FFFFFF",
    violet: "#7857FF",
    indigo: "#4E6BFF",
    cyan: "#25D0E8",
    mint: "#55E6B5",
    amber: "#FFBE55",
    danger: "#FF5D73",
  },
  gradient: {
    signature:
      "linear-gradient(135deg, #7857FF 0%, #4E6BFF 48%, #25D0E8 100%)",
    ambientDark:
      "radial-gradient(circle at 50% 0%, rgba(120, 87, 255, 0.24), transparent 54%)",
    ambientLight:
      "radial-gradient(circle at 50% 0%, rgba(78, 107, 255, 0.14), transparent 56%)",
  },
  radius: {
    control: "0.875rem",
    card: "1.25rem",
    panel: "1.5rem",
    pill: "999px",
  },
  motion: {
    instantMs: 120,
    fastMs: 180,
    normalMs: 260,
    expressiveMs: 420,
    easing: "cubic-bezier(0.22, 1, 0.36, 1)",
  },
} as const;

export type MoatazAiTheme = typeof MOATAZ_AI_THEME;
