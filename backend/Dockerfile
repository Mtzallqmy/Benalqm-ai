# Backend Dockerfile — multi-stage build
# Stage 1 (builder): compiles native Python extensions with build-essential
# Stage 2 (dev):     retains toolchain for dev containers (uv sync at startup)
# Stage 3 (runtime): clean image without compiler toolchain for production

ARG UV_IMAGE=ghcr.io/astral-sh/uv:0.11.1
FROM ${UV_IMAGE} AS uv-source

FROM python:3.12-slim-bookworm AS builder

ARG NODE_MAJOR=22
ARG APT_MIRROR
ARG UV_INDEX_URL
ARG NPM_REGISTRY
ARG LARK_CLI_NPM_VERSION=1.0.65
ARG UV_EXTRAS
# Product-layer dependency used only for encrypted BYOK credentials. It is
# installed after the upstream locked sync so DeerFlow's uv.lock stays intact.
ARG MOATAZ_CRYPTOGRAPHY_VERSION=50.0.1

RUN if [ -n "${APT_MIRROR}" ]; then \
      sed -i "s|deb.debian.org|${APT_MIRROR}|g" /etc/apt/sources.list.d/debian.sources 2>/dev/null || true; \
      sed -i "s|deb.debian.org|${APT_MIRROR}|g" /etc/apt/sources.list 2>/dev/null || true; \
    fi

RUN apt-get update && apt-get install -y \
    curl git build-essential gnupg ca-certificates \
    && mkdir -p /etc/apt/keyrings \
    && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_${NODE_MAJOR}.x nodistro main" > /etc/apt/sources.list.d/nodesource.list \
    && apt-get update \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

RUN if [ -n "${NPM_REGISTRY}" ]; then npm config set registry "${NPM_REGISTRY}"; fi \
    && npm install -g @larksuite/cli@${LARK_CLI_NPM_VERSION} \
    && lark-cli --version

COPY --from=uv-source /uv /uvx /usr/local/bin/
WORKDIR /app
COPY backend ./backend

RUN --mount=type=cache,target=/root/.cache/uv \
    sh -c 'cd backend && \
    set -f; \
    extras_flags=""; \
    for extra in $(printf "%s" "$UV_EXTRAS" | tr "," " "); do \
        case "$extra" in \
            [!A-Za-z]* | *[!A-Za-z0-9_-]*) \
                echo "UV_EXTRAS entry $extra is invalid (must match [A-Za-z][A-Za-z0-9_-]*)" >&2; \
                exit 1; \
                ;; \
        esac; \
        extras_flags="$extras_flags --extra $extra"; \
    done; \
    UV_INDEX_URL=${UV_INDEX_URL:-https://pypi.org/simple} uv sync --locked --extra redis $extras_flags && \
    UV_INDEX_URL=${UV_INDEX_URL:-https://pypi.org/simple} uv pip install --python .venv/bin/python "cryptography=='"$MOATAZ_CRYPTOGRAPHY_VERSION"'"'

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV PYTHONIOENCODING=utf-8

FROM builder AS dev
COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker
EXPOSE 8001
CMD ["sh", "-c", "cd backend && PYTHONPATH=. uv run --no-sync uvicorn app.gateway.app:app --host 0.0.0.0 --port 8001"]

FROM python:3.12-slim-bookworm
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV PYTHONIOENCODING=utf-8

COPY --from=builder /usr/bin/node /usr/bin/node
COPY --from=builder /usr/lib/node_modules /usr/lib/node_modules
RUN ln -s ../lib/node_modules/npm/bin/npm-cli.js /usr/bin/npm \
    && ln -s ../lib/node_modules/npm/bin/npx-cli.js /usr/bin/npx \
    && LARK_CLI_BIN="$(node -p 'const bin=require("/usr/lib/node_modules/@larksuite/cli/package.json").bin; typeof bin === "string" ? bin : (bin["lark-cli"] || Object.values(bin)[0])')" \
    && ln -s "../lib/node_modules/@larksuite/cli/${LARK_CLI_BIN#./}" /usr/bin/lark-cli \
    && lark-cli --version

COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=uv-source /uv /uvx /usr/local/bin/
WORKDIR /app
COPY --from=builder /app/backend ./backend
EXPOSE 8001
CMD ["sh", "-c", "cd backend && PYTHONPATH=. uv run --no-sync uvicorn app.gateway.app:app --host 0.0.0.0 --port 8001"]
