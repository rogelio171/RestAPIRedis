"use client";

import { useEffect } from "react";
import L from "leaflet";
import {
  CircleMarker,
  MapContainer,
  Marker,
  Popup,
  TileLayer,
  useMap,
  useMapEvents,
} from "react-leaflet";

import type { GeoSearchResult } from "@/types/api";

export type GeoMapInnerProps = {
  origin: { lat: number; lng: number } | null;
  results: GeoSearchResult[];
  onMapClick: (lat: number, lng: number) => void;
};

function ClickHandler({ onClick }: { onClick: (lat: number, lng: number) => void }) {
  useMapEvents({
    click(e) {
      onClick(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

function Recenter({ lat, lng, zoom }: { lat: number; lng: number; zoom: number }) {
  const map = useMap();
  useEffect(() => {
    map.setView([lat, lng], zoom);
  }, [map, lat, lng, zoom]);
  return null;
}

export function GeoMapInner({ origin, results, onMapClick }: GeoMapInnerProps) {
  useEffect(() => {
    const proto = L.Icon.Default.prototype as unknown as { _getIconUrl?: unknown };
    delete proto._getIconUrl;
    L.Icon.Default.mergeOptions({
      iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
      iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
      shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
    });
  }, []);

  const center: [number, number] = origin
    ? [origin.lat, origin.lng]
    : [20, 0];
  const zoom = origin ? 4 : 2;
  const viewLat = origin?.lat ?? 20;
  const viewLng = origin?.lng ?? 0;

  return (
    <MapContainer
      center={center}
      zoom={zoom}
      className="h-[480px] w-full rounded-lg [&_.leaflet-tile-pane]:z-0"
      scrollWheelZoom
    >
      <Recenter lat={viewLat} lng={viewLng} zoom={zoom} />
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <ClickHandler onClick={onMapClick} />
      {origin && (
        <CircleMarker
          center={[origin.lat, origin.lng]}
          radius={9}
          pathOptions={{
            color: "#292524",
            fillColor: "#57534e",
            fillOpacity: 0.9,
            weight: 2,
          }}
        >
          <Popup>Search origin</Popup>
        </CircleMarker>
      )}
      {results.map((r) => (
        <Marker key={r.countryCode} position={[r.latitude, r.longitude]}>
          <Popup>
            <strong>{r.countryName}</strong>
            <br />
            {r.countryCode} — {r.distanceKm.toFixed(1)} km
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
