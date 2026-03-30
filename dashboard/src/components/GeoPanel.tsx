"use client";

import { useCallback, useEffect, useState } from "react";

import { api, getResponseHeader } from "@/lib/api";
import type { CountryDTO, GeoSearchResult } from "@/types/api";

import { GeoMap } from "./GeoMap";
import { btnPrimary, card, codeInline, input } from "./ui";

export function GeoPanel() {
  const [radiusKm, setRadiusKm] = useState(1000);
  const [limit, setLimit] = useState(10);
  const [origin, setOrigin] = useState<{ lat: number; lng: number } | null>(null);
  const [nearby, setNearby] = useState<GeoSearchResult[]>([]);
  const [loadingNearby, setLoadingNearby] = useState(false);
  const [nearbyError, setNearbyError] = useState<string | null>(null);
  const [lastNearbyMs, setLastNearbyMs] = useState<number | null>(null);

  const [countries, setCountries] = useState<CountryDTO[]>([]);
  const [fromCode, setFromCode] = useState("");
  const [toCode, setToCode] = useState("");
  const [distance, setDistance] = useState<{
    from: string;
    to: string;
    distanceKm: number;
    unit: string;
  } | null>(null);
  const [distanceError, setDistanceError] = useState<string | null>(null);
  const [loadingDistance, setLoadingDistance] = useState(false);

  const [geoStats, setGeoStats] = useState<{ countriesIndexed?: number } | null>(null);

  useEffect(() => {
    void (async () => {
      try {
        const res = await api.get("/api/v1/geo/stats");
        setGeoStats(res.data);
      } catch {
        setGeoStats(null);
      }
    })();
  }, []);

  useEffect(() => {
    void (async () => {
      try {
        const res = await api.get<CountryDTO[]>("/api/v1/countries/all");
        const list = [...res.data].sort((a, b) => a.commonName.localeCompare(b.commonName));
        setCountries(list);
        setFromCode((prev) => prev || list[0]?.cca3 || "");
        setToCode((prev) => prev || list[1]?.cca3 || list[0]?.cca3 || "");
      } catch {
        /* optional */
      }
    })();
  }, []);

  const runNearby = useCallback(
    async (lat: number, lng: number) => {
      setOrigin({ lat, lng });
      setNearbyError(null);
      setLoadingNearby(true);
      try {
        const res = await api.get<GeoSearchResult[]>("/api/v1/geo/nearby", {
          params: { lat, lng, radiusKm, limit },
        });
        setNearby(res.data);
        const raw = getResponseHeader(res.headers, "X-Response-Time-Ms");
        const ms = raw != null ? Number.parseFloat(raw) : NaN;
        setLastNearbyMs(Number.isFinite(ms) ? ms : null);
      } catch (e) {
        setNearbyError(e instanceof Error ? e.message : "Nearby search failed");
        setNearby([]);
      } finally {
        setLoadingNearby(false);
      }
    },
    [limit, radiusKm]
  );

  const handleMapClick = (lat: number, lng: number) => {
    void runNearby(lat, lng);
  };

  const fetchDistance = async () => {
    if (!fromCode || !toCode) return;
    setDistanceError(null);
    setLoadingDistance(true);
    setDistance(null);
    try {
      const res = await api.get<{
        from: string;
        to: string;
        distanceKm: number;
        unit: string;
      }>("/api/v1/geo/distance", {
        params: { from: fromCode, to: toCode },
      });
      setDistance(res.data);
    } catch (e) {
      setDistanceError(
        e instanceof Error ? e.message : "Distance lookup failed (e.g. code missing in geo index)"
      );
    } finally {
      setLoadingDistance(false);
    }
  };

  return (
    <div className="space-y-12">
      <header className="border-b border-[var(--border)] pb-8">
        <h1 className="text-xl font-semibold tracking-tight text-stone-900 dark:text-stone-50">
          Geolocation
        </h1>
        <p className="mt-2 max-w-2xl text-sm leading-relaxed text-stone-500 dark:text-stone-400">
          Redis <code className={codeInline}>GEORADIUS</code> and <code className={codeInline}>GEODIST</code>.
          Click the map to run a nearby search from that point.
        </p>
        {geoStats?.countriesIndexed != null && (
          <p className="mt-3 text-sm text-stone-600 dark:text-stone-400">
            Index size <span className="font-medium text-stone-900 dark:text-stone-100">{geoStats.countriesIndexed}</span>{" "}
            <span className="text-stone-400">(ZCARD)</span>
          </p>
        )}
      </header>

      <section className="space-y-4">
        <div className="flex flex-wrap items-end gap-4">
          <div>
            <label className="mb-1.5 block text-xs font-medium text-stone-600 dark:text-stone-400">
              Radius (km)
            </label>
            <input
              type="number"
              min={1}
              step={1}
              value={radiusKm}
              onChange={(e) => setRadiusKm(Number(e.target.value) || 1000)}
              className={`w-28 ${input}`}
            />
          </div>
          <div>
            <label className="mb-1.5 block text-xs font-medium text-stone-600 dark:text-stone-400">
              Max results
            </label>
            <input
              type="number"
              min={1}
              max={250}
              value={limit}
              onChange={(e) => setLimit(Number(e.target.value) || 10)}
              className={`w-24 ${input}`}
            />
          </div>
        </div>
        {loadingNearby && <p className="text-sm text-stone-500">Searching…</p>}
        {lastNearbyMs != null && !loadingNearby && (
          <p className="text-sm text-stone-600 dark:text-stone-400">
            Last request: <span className="font-medium">{lastNearbyMs} ms</span>
          </p>
        )}
        {nearbyError && (
          <p className="text-sm text-red-600 dark:text-red-400" role="alert">
            {nearbyError}
          </p>
        )}
        <div className={`${card} overflow-hidden p-0`}>
          <GeoMap origin={origin} results={nearby} onMapClick={handleMapClick} />
        </div>
        {nearby.length > 0 && (
          <div className={`${card} overflow-x-auto p-0`}>
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-stone-200 bg-stone-50 dark:border-stone-800 dark:bg-stone-900/50">
                <tr>
                  <th className="px-4 py-2.5 text-xs font-semibold uppercase tracking-wider text-stone-500">
                    Code
                  </th>
                  <th className="px-4 py-2.5 text-xs font-semibold uppercase tracking-wider text-stone-500">
                    Name
                  </th>
                  <th className="px-4 py-2.5 text-xs font-semibold uppercase tracking-wider text-stone-500">
                    Distance
                  </th>
                </tr>
              </thead>
              <tbody>
                {nearby.map((r) => (
                  <tr
                    key={r.countryCode}
                    className="border-t border-stone-100 dark:border-stone-800"
                  >
                    <td className="px-4 py-2 font-mono text-xs">{r.countryCode}</td>
                    <td className="px-4 py-2">{r.countryName}</td>
                    <td className="px-4 py-2 tabular-nums text-stone-600 dark:text-stone-400">
                      {r.distanceKm.toFixed(2)} km
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section>
        <h2 className="text-sm font-semibold text-stone-900 dark:text-stone-100">Distance</h2>
        <p className="mt-1 text-sm text-stone-500">
          <code className={codeInline}>GET /api/v1/geo/distance</code>
        </p>
        <div className="mt-4 flex flex-wrap items-end gap-4">
          <div>
            <label className="mb-1.5 block text-xs font-medium text-stone-600 dark:text-stone-400">From</label>
            <select
              value={fromCode}
              onChange={(e) => setFromCode(e.target.value)}
              className={`min-w-[220px] ${input}`}
            >
              {countries.map((c) => (
                <option key={c.cca3} value={c.cca3}>
                  {c.commonName} ({c.cca3})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1.5 block text-xs font-medium text-stone-600 dark:text-stone-400">To</label>
            <select
              value={toCode}
              onChange={(e) => setToCode(e.target.value)}
              className={`min-w-[220px] ${input}`}
            >
              {countries.map((c) => (
                <option key={c.cca3} value={c.cca3}>
                  {c.commonName} ({c.cca3})
                </option>
              ))}
            </select>
          </div>
          <button
            type="button"
            onClick={() => void fetchDistance()}
            disabled={loadingDistance || !fromCode || !toCode}
            className={btnPrimary}
          >
            {loadingDistance ? "Loading…" : "Calculate"}
          </button>
        </div>
        {distanceError && (
          <p className="mt-3 text-sm text-red-600 dark:text-red-400" role="alert">
            {distanceError}
          </p>
        )}
        {distance && (
          <p
            className={`${card} mt-4 border-stone-200 bg-stone-50 px-4 py-3 text-sm text-stone-800 dark:border-stone-800 dark:bg-stone-900/40 dark:text-stone-200`}
          >
            <span className="font-medium">
              {distance.from} → {distance.to}
            </span>
            <span className="mx-2 text-stone-400">·</span>
            {distance.distanceKm.toFixed(2)} {distance.unit}
          </p>
        )}
      </section>
    </div>
  );
}
