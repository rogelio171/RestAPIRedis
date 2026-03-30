"use client";

import dynamic from "next/dynamic";

import type { GeoMapInnerProps } from "./GeoMapInner";

const GeoMapInner = dynamic(
  () => import("./GeoMapInner").then((m) => m.GeoMapInner),
  {
    ssr: false,
    loading: () => (
      <div className="flex h-[480px] items-center justify-center rounded-lg border border-zinc-200 bg-zinc-50 text-sm text-zinc-500 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-400">
        Loading map…
      </div>
    ),
  }
);

export function GeoMap(props: GeoMapInnerProps) {
  return <GeoMapInner {...props} />;
}
