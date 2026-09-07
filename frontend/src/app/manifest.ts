import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "moataz ai",
    short_name: "moataz ai",
    description:
      "وكيل ذكاء اصطناعي للأبحاث والبرمجة والملفات والمهام طويلة الأفق.",
    start_url: "/workspace",
    scope: "/",
    display: "standalone",
    background_color: "#080B14",
    theme_color: "#7857FF",
    orientation: "any",
    lang: "ar",
    dir: "auto",
    categories: ["productivity", "utilities"],
    icons: [
      {
        src: "/brand/moataz-192.png",
        sizes: "192x192",
        type: "image/png",
        purpose: "any maskable",
      },
      {
        src: "/brand/moataz-512.png",
        sizes: "512x512",
        type: "image/png",
        purpose: "any maskable",
      },
      {
        src: "/brand/moataz-mark.svg",
        sizes: "any",
        type: "image/svg+xml",
        purpose: "any",
      },
    ],
    shortcuts: [
      {
        name: "محادثة جديدة",
        short_name: "محادثة",
        url: "/workspace/chats/new",
        icons: [{ src: "/brand/moataz-192.png", sizes: "192x192" }],
      },
      {
        name: "الوكلاء",
        short_name: "الوكلاء",
        url: "/workspace/agents",
        icons: [{ src: "/brand/moataz-192.png", sizes: "192x192" }],
      },
    ],
  };
}
