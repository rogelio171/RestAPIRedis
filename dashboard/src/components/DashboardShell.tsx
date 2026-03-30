"use client";

import { useSyncExternalStore, useState } from "react";

import {
  clearStoredToken,
  getAuthSnapshot,
  getServerAuthSnapshot,
  subscribeAuth,
} from "@/lib/auth";
import { getApiBaseUrl } from "@/lib/api";

import { GeoPanel } from "./GeoPanel";
import { LoginForm } from "./LoginForm";
import { PerformancePanel } from "./PerformancePanel";
import { StoragePanel } from "./StoragePanel";
import { btnSecondary } from "./ui";

type Tab = "performance" | "storage" | "geo";

const tabs: { id: Tab; label: string }[] = [
  { id: "performance", label: "Performance" },
  { id: "storage", label: "Inspector" },
  { id: "geo", label: "Geolocation" },
];

export function DashboardShell() {
  const authed = useSyncExternalStore(subscribeAuth, getAuthSnapshot, getServerAuthSnapshot);
  const [tab, setTab] = useState<Tab>("performance");
  const apiBaseUrl = getApiBaseUrl();

  if (!authed) {
    return (
      <LoginForm
        apiBaseUrl={apiBaseUrl}
        onLoggedIn={() => {
          /* token stored in LoginForm; subscribeAuth updates authed */
        }}
      />
    );
  }

  return (
    <div className="min-h-full bg-stone-50 text-stone-900 dark:bg-stone-950 dark:text-stone-50">
      <main className="mx-auto max-w-7xl px-4 py-6 md:px-8 md:py-8">
        <header className="mb-6 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-[28px] font-semibold tracking-tight">RestAPIRedis Dashboard</h1>
            <p className="mt-1 text-sm text-stone-500 dark:text-stone-400">
              Cache performance, Redis inspection, and geospatial testing console
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-md border border-stone-200 bg-white px-3 py-2 font-mono text-[11px] text-stone-500 shadow-sm dark:border-stone-800 dark:bg-stone-900 dark:text-stone-400">
              {apiBaseUrl}
            </span>
            <button type="button" onClick={clearStoredToken} className={btnSecondary}>
              Sign out
            </button>
          </div>
        </header>

        <div className="mb-6 flex flex-wrap gap-2">
          {tabs.map((t) => (
            <button
              key={t.id}
              type="button"
              onClick={() => setTab(t.id)}
              className={
                tab === t.id
                  ? "rounded-md border border-stone-200 bg-white px-4 py-2 text-sm font-medium text-stone-900 shadow-sm dark:border-stone-700 dark:bg-stone-900 dark:text-stone-50"
                  : "rounded-md border border-transparent bg-stone-100 px-4 py-2 text-sm font-medium text-stone-600 transition hover:bg-stone-200 dark:bg-stone-900/50 dark:text-stone-400 dark:hover:bg-stone-900"
              }
            >
              {t.label}
            </button>
          ))}
        </div>

        {tab === "performance" && <PerformancePanel />}
        {tab === "storage" && <StoragePanel />}
        {tab === "geo" && <GeoPanel />}
      </main>
    </div>
  );
}
