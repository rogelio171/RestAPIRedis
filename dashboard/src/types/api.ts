export type CountryDTO = {
  cca3: string;
  commonName: string;
  officialName: string;
  capital: string;
  region: string;
  subregion: string;
  population: number;
  area: number;
  latitude: number;
  longitude: number;
  capitalLat: number;
  capitalLng: number;
  flagUrl: string;
  flagSvg: string;
};

export type GeoSearchResult = {
  countryCode: string;
  countryName: string;
  latitude: number;
  longitude: number;
  distanceKm: number;
};

export type DistanceResponse = {
  from: string;
  to: string;
  distanceKm: number;
  unit: string;
};
