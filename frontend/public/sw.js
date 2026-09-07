/* moataz ai service worker: cache static shell only; never cache authenticated HTML or API data. */
const CACHE = "moataz-ai-static-v1";
const STATIC_ASSETS = [
  "/brand/moataz-mark.svg",
  "/brand/moataz-192.png",
  "/brand/moataz-512.png",
];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(STATIC_ASSETS)));
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener("fetch", (event) => {
  const request = event.request;
  if (request.method !== "GET") return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  if (
    url.pathname.startsWith("/api/") ||
    url.pathname.startsWith("/workspace") ||
    url.pathname.startsWith("/login") ||
    url.pathname.startsWith("/setup")
  ) {
    return;
  }

  if (request.mode === "navigate") {
    event.respondWith(
      fetch(request).catch(
        () =>
          new Response(
            "<!doctype html><html lang=\"ar\" dir=\"rtl\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><body style=\"font-family:system-ui;background:#080B14;color:#fff;padding:32px\"><h1>moataz ai</h1><p>لا يوجد اتصال بالخادم حاليًا. أعد المحاولة بعد استعادة الشبكة.</p></body></html>",
            { headers: { "Content-Type": "text/html; charset=utf-8" } },
          ),
      ),
    );
    return;
  }

  if (/\.(?:js|css|png|svg|ico|woff2?)$/i.test(url.pathname)) {
    event.respondWith(
      caches.open(CACHE).then(async (cache) => {
        const cached = await cache.match(request);
        const network = fetch(request)
          .then((response) => {
            if (response.ok) cache.put(request, response.clone());
            return response;
          })
          .catch(() => cached);
        return cached || network;
      }),
    );
  }
});
