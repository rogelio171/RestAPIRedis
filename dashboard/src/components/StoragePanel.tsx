"use client";

import { useCallback, useState } from "react";

import { api } from "@/lib/api";

import { btnPrimary, card, codeInline } from "./ui";

function normalizeCacheKeys(data: Record<string, unknown>): Record<string, string[]> {
  const out: Record<string, string[]> = {};
  for (const [name, v] of Object.entries(data)) {
    if (Array.isArray(v)) {
      out[name] = v.map(String);
    } else if (v != null && typeof v === "object") {
      out[name] = Object.values(v as Record<string, string>).map(String);
    } else {
      out[name] = [];
    }
  }
  return out;
}

export function StoragePanel() {
  const [keys, setKeys] = useState<Record<string, string[]> | null>(null);
  const [stats, setStats] = useState<Record<string, unknown> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    setLoading(true);
    try {
      const [keysRes, statsRes] = await Promise.all([
        api.get<Record<string, unknown>>("/api/v1/cache/keys"),
        api.get<Record<string, unknown>>("/api/v1/cache/stats"),
      ]);
      setKeys(normalizeCacheKeys(keysRes.data));
      setStats(statsRes.data);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load cache data");
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <div>
      <header className="mb-10 border-b border-[var(--border)] pb-8">
        <h1 className="text-xl font-semibold tracking-tight text-stone-900 dark:text-stone-50">
          Data inspector
        </h1>
        <p className="mt-2 max-w-2xl text-sm leading-relaxed text-stone-500 dark:text-stone-400">
          Spring cache keys via Redis <code className={codeInline}>SCAN</code> and memory stats from{" "}
          <code className={codeInline}>/api/v1/cache/keys</code> and{" "}
          <code className={codeInline}>/api/v1/cache/stats</code>.
        </p>
      </header>

      <button type="button" onClick={() => void load()} disabled={loading} className={btnPrimary}>
        {loading ? "Loading…" : "Refresh"}
      </button>

      {error && (
        <p className="mt-4 text-sm text-red-600 dark:text-red-400" role="alert">
          {error}
        </p>
      )}

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <section className={`${card} p-5`}>
          <h2 className="text-xs font-semibold uppercase tracking-wider text-stone-500">Keys</h2>
          {!keys ? (
            <p className="mt-4 text-sm text-stone-500">Refresh to load keys.</p>
          ) : (
            <ul className="mt-4 max-h-[min(420px,50vh)] space-y-5 overflow-auto text-sm">
              {Object.entries(keys).map(([name, keyList]) => (
                <li key={name}>
                  <p className="font-mono text-xs font-medium text-stone-800 dark:text-stone-200">{name}</p>
                  <ul className="mt-2 space-y-1 border-l border-stone-200 pl-3 dark:border-stone-700">
                    {keyList.length === 0 ? (
                      <li className="font-mono text-xs text-stone-400">(empty)</li>
                    ) : (
                      keyList.map((k) => (
                        <li key={k} className="break-all font-mono text-[11px] text-stone-600 dark:text-stone-400">
                          {k}
                        </li>
                      ))
                    )}
                  </ul>
                </li>
              ))}
            </ul>
          )}
        </section>
        <section className={`${card} p-5`}>
          <h2 className="text-xs font-semibold uppercase tracking-wider text-stone-500">Statistics</h2>
          {!stats ? (
            <p className="mt-4 text-sm text-stone-500">Refresh to load stats.</p>
          ) : (
            <pre className="mt-4 max-h-[min(420px,50vh)] overflow-auto rounded-md border border-stone-100 bg-stone-50 p-4 font-mono text-[11px] leading-relaxed text-stone-800 dark:border-stone-800 dark:bg-stone-950 dark:text-stone-300">
              {JSON.stringify(stats, null, 2)}
            </pre>
          )}
        </section>
      </div>
    </div>
  );
}
