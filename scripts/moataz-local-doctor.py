#!/usr/bin/env python3
"""Small dependency-free local deployment diagnostic for moataz ai."""

from __future__ import annotations

import argparse
import json
import socket
import sys
import urllib.error
import urllib.request


def http_check(name: str, url: str, timeout: float = 5.0) -> bool:
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            ok = 200 <= response.status < 400
            print(f"{'OK' if ok else 'FAIL':4} {name:12} HTTP {response.status} {url}")
            return ok
    except Exception as exc:
        print(f"FAIL {name:12} {type(exc).__name__}: {exc}")
        return False


def tcp_check(name: str, host: str, port: int, timeout: float = 3.0) -> bool:
    try:
        with socket.create_connection((host, port), timeout=timeout):
            print(f"OK   {name:12} TCP {host}:{port}")
            return True
    except OSError as exc:
        print(f"FAIL {name:12} {host}:{port} {exc}")
        return False


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://127.0.0.1:2026")
    parser.add_argument("--provider", help="Optional provider URL to test, e.g. http://127.0.0.1:11434")
    args = parser.parse_args()
    base = args.base.rstrip("/")
    checks = [
        http_check("nginx/ui", f"{base}/"),
        http_check("gateway", f"{base}/health"),
        http_check("readiness", f"{base}/health/ready"),
    ]
    if args.provider:
        provider = args.provider.rstrip("/")
        if provider.endswith(":11434"):
            checks.append(http_check("provider", f"{provider}/api/tags"))
        else:
            checks.append(http_check("provider", f"{provider}/v1/models"))
    print(json.dumps({"ok": all(checks), "checks": len(checks)}))
    return 0 if all(checks) else 1


if __name__ == "__main__":
    raise SystemExit(main())
