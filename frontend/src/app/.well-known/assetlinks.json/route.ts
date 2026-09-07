import { NextResponse } from "next/server";

const FINGERPRINT = /^[0-9A-F]{2}(?::[0-9A-F]{2}){31}$/;

export function GET() {
  const packageName =
    process.env.MOATAZ_ANDROID_PACKAGE_NAME?.trim() || "ai.moataz.app";
  const fingerprints = (process.env.MOATAZ_ANDROID_CERT_SHA256 ?? "")
    .split(",")
    .map((value) => value.trim().toUpperCase())
    .filter((value) => FINGERPRINT.test(value));

  const statements = fingerprints.length
    ? [
        {
          relation: ["delegate_permission/common.handle_all_urls"],
          target: {
            namespace: "android_app",
            package_name: packageName,
            sha256_cert_fingerprints: fingerprints,
          },
        },
      ]
    : [];

  return NextResponse.json(statements, {
    headers: {
      "Cache-Control": fingerprints.length
        ? "public, max-age=3600, s-maxage=3600"
        : "no-store",
      "X-Content-Type-Options": "nosniff",
    },
  });
}
