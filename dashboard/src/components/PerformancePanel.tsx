"use client";

import { useCallback, useMemo, useState } from "react";
import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  LinearScale,
  Title,
  Tooltip,
} from "chart.js";
import type { AxiosResponse } from "axios";
import { Bar } from "react-chartjs-2";

import { api, getResponseHeader } from "@/lib/api";

import { btnPrimary, btnSecondary, card, codeInline, input } from "./ui";

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip);

function readResponseTimeMs(headers: AxiosResponse["headers"]): number | null {
  const raw = getResponseHeader(headers, "X-Response-Time-Ms");
  if (raw == null) return null;
  const n = Number.parseFloat(raw);
  return Number.isFinite(n) ? n : null;
}

function formatMs(value: number | null): string {
  if (value == null) return "--";
  return `${value.toFixed(value >= 10 ? 0 : 2)} ms`;
}

export function PerformancePanel() {
  const [labels, setLabels] = useState<string[]>([]);
  const [values, setValues] = useState<number[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastTotal, setLastTotal] = useState<number | null>(null);
  const [period, setPeriod] = useState("This Session");
  const [endpoint] = useState("/api/v1/countries/all");

  const fetchCountries = useCallback(async () => {
    setError(null);
    setLoading(true);
    try {
      const res = await api.get(endpoint);
      const headers = res.headers;
      const ms = readResponseTimeMs(headers);
      setLabels((prev) => [...prev, `Request ${prev.length + 1}`]);
      setValues((prev) => [...prev, ms ?? 0]);
      const totalHeader = getResponseHeader(headers, "X-Total-Count");
      if (totalHeader != null) {
        const n = Number.parseInt(totalHeader, 10);
        setLastTotal(Number.isFinite(n) ? n : null);
      }
      if (ms == null) {
        setError("The API response did not expose X-Response-Time-Ms.");
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Request failed");
    } finally {
      setLoading(false);
    }
  }, [endpoint]);

  const clearCache = async () => {
    setError(null);
    setLoading(true);
    try {
      await api.post("/api/v1/countries/refresh");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Refresh failed");
    } finally {
      setLoading(false);
    }
  };

  const resetChart = () => {
    setLabels([]);
    setValues([]);
    setLastTotal(null);
    setError(null);
  };

  const latest = values.at(-1) ?? null;
  const best = values.length ? Math.min(...values) : null;
  const average = values.length ? values.reduce((sum, v) => sum + v, 0) / values.length : null;

  const metrics = useMemo(
    () => [
      { label: "Latest response", value: formatMs(latest), note: "Last authenticated call" },
      { label: "Best response", value: formatMs(best), note: "Fastest observed hit" },
      { label: "Average response", value: formatMs(average), note: "Across current session" },
      { label: "Records returned", value: lastTotal == null ? "--" : `${lastTotal}`, note: "X-Total-Count" },
    ],
    [latest, best, average, lastTotal]
  );

  const data = {
    labels,
    datasets: [
      {
        label: "Response time",
        data: values,
        backgroundColor: "#cbd5e1",
        borderColor: "#94a3b8",
        borderWidth: 1,
        borderRadius: 8,
        borderSkipped: false,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      title: { display: false },
      tooltip: {
        backgroundColor: "rgba(15, 23, 42, 0.95)",
        padding: 10,
        cornerRadius: 8,
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: "#78716c", font: { size: 11 } },
      },
      y: {
        beginAtZero: true,
        grid: { color: "rgba(148, 163, 184, 0.18)" },
        ticks: { color: "#78716c", font: { size: 11 } },
      },
    },
  };

  return (
    <div className="space-y-6">
      <div className="grid gap-3 md:grid-cols-3">
        <div className={`${card} p-4`}>
          <label className="mb-1 block text-xs font-medium text-stone-500">Range</label>
          <select value={period} onChange={(e) => setPeriod(e.target.value)} className={`w-full ${input}`}>
            <option>This Session</option>
            <option>Last Hour</option>
            <option>Today</option>
          </select>
        </div>
        <div className={`${card} p-4`}>
          <label className="mb-1 block text-xs font-medium text-stone-500">Endpoint</label>
          <div className={`w-full ${input} flex items-center`}>{endpoint}</div>
        </div>
        <div className={`${card} p-4`}>
          <label className="mb-1 block text-xs font-medium text-stone-500">Actions</label>
          <div className="flex gap-2">
            <button type="button" onClick={() => void fetchCountries()} disabled={loading} className={btnPrimary}>
              {loading ? "Loading…" : "Run test"}
            </button>
            <button type="button" onClick={() => void clearCache()} disabled={loading} className={btnSecondary}>
              Clear cache
            </button>
          </div>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {metrics.map((metric) => (
          <section key={metric.label} className={`${card} p-5`}>
            <p className="text-sm font-medium text-stone-500">{metric.label}</p>
            <p className="mt-5 text-4xl font-semibold tracking-tight text-stone-900 dark:text-stone-50">
              {metric.value}
            </p>
            <p className="mt-3 text-xs text-stone-400">{metric.note}</p>
          </section>
        ))}
      </div>

      {error && (
        <p className="text-sm text-red-600 dark:text-red-400" role="alert">
          {error}
        </p>
      )}

      <div className="grid gap-6 xl:grid-cols-[1.4fr_0.6fr]">
        <section className={`${card} p-6`}>
          <div className="mb-4 flex items-center justify-between gap-4">
            <div>
              <h2 className="text-lg font-semibold tracking-tight">Response time trend</h2>
              <p className="mt-1 text-sm text-stone-500">
                Measured from <code className={codeInline}>X-Response-Time-Ms</code>
              </p>
            </div>
            <button type="button" onClick={resetChart} className={btnSecondary}>
              Reset
            </button>
          </div>
          {labels.length === 0 ? (
            <div className="flex h-[360px] items-center justify-center rounded-xl border border-dashed border-stone-200 bg-stone-50 text-sm text-stone-500 dark:border-stone-700 dark:bg-stone-900/30 dark:text-stone-400">
              Run a test to visualize cache misses and hits.
            </div>
          ) : (
            <div className="h-[360px]">
              <Bar data={data} options={options} />
            </div>
          )}
        </section>

        <section className={`${card} p-6`}>
          <h2 className="text-lg font-semibold tracking-tight">How to read this</h2>
          <ul className="mt-4 space-y-4 text-sm leading-relaxed text-stone-600 dark:text-stone-400">
            <li>The first request usually represents a cache miss and is slower.</li>
            <li>Subsequent requests should drop when Redis serves the same result.</li>
            <li>Use <code className={codeInline}>POST /api/v1/countries/refresh</code> to force a miss again.</li>
            <li>Current namespace: <code className={codeInline}>restapi-json-v3::</code></li>
          </ul>
        </section>
      </div>
    </div>
  );
}
