import { fetch } from "@/core/api/fetcher";

export type ProviderType =
  | "openai"
  | "openrouter"
  | "openai_compatible"
  | "ollama"
  | "vllm"
  | "lmstudio"
  | "litellm";

export type DiscoveredModel = {
  model: string;
  display_name?: string;
  description?: string;
  supports_thinking?: boolean;
  supports_reasoning_effort?: boolean;
  supports_vision?: boolean;
  supports_tools?: boolean;
  supports_streaming?: boolean;
  context_window?: number | null;
  registry_name?: string;
};

export type ProviderSummary = {
  id: string;
  name: string;
  provider_type: ProviderType;
  base_url: string;
  has_api_key: boolean;
  models: DiscoveredModel[];
  created_at: string;
  updated_at: string;
};

export type ProviderDraft = {
  provider_type: ProviderType;
  base_url?: string;
  api_key?: string;
};

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let detail = `Request failed (${response.status})`;
    try {
      const payload = (await response.json()) as { detail?: string };
      if (payload.detail) detail = payload.detail;
    } catch {
      // Keep status-only error: provider responses may not be JSON.
    }
    throw new Error(detail);
  }
  return (await response.json()) as T;
}

export async function listProviders(): Promise<ProviderSummary[]> {
  return readJson<ProviderSummary[]>(await fetch("/api/providers"));
}

export async function testProvider(draft: ProviderDraft) {
  return readJson<{ ok: boolean; model_count: number }>(
    await fetch("/api/providers/test", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(draft),
    }),
  );
}

export async function discoverProviderModels(draft: ProviderDraft) {
  const result = await readJson<{ models: DiscoveredModel[] }>(
    await fetch("/api/providers/discover", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(draft),
    }),
  );
  return result.models;
}

export async function probeProviderModel(draft: ProviderDraft, model: string) {
  return readJson<Record<string, unknown>>(
    await fetch("/api/providers/probe", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...draft, model }),
    }),
  );
}

export async function saveProvider(input: ProviderDraft & {
  name: string;
  models: DiscoveredModel[];
  provider_id?: string;
}): Promise<ProviderSummary> {
  return readJson<ProviderSummary>(
    await fetch("/api/providers", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    }),
  );
}

export async function setDefaultProviderModel(modelName: string | null) {
  return readJson<{ model_name: string | null }>(
    await fetch("/api/providers/default", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ model_name: modelName }),
    }),
  );
}

export async function deleteProvider(providerId: string): Promise<void> {
  const response = await fetch(`/api/providers/${encodeURIComponent(providerId)}`, { method: "DELETE" });
  if (!response.ok && response.status !== 204) {
    throw new Error(`Delete failed (${response.status})`);
  }
}
