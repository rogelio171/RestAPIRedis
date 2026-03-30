import axios, {
  type AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";

import { clearStoredToken, getStoredToken } from "./auth";

const baseURL =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ||
  "http://localhost:8080";

export const api = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

function attachAuth(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  const token = getStoredToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}

api.interceptors.request.use(attachAuth, (error) => Promise.reject(error));

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      clearStoredToken();
    }
    return Promise.reject(error);
  }
);

export function getApiBaseUrl(): string {
  return baseURL;
}

/**
 * Reads a response header in a way that works with AxiosHeaders (case-insensitive {@code get})
 * and with plain objects. Required for custom headers after CORS
 * {@code Access-Control-Expose-Headers} is set on the API.
 */
export function getResponseHeader(
  headers: AxiosResponse["headers"],
  name: string
): string | undefined {
  if (headers && typeof headers === "object" && "get" in headers && typeof headers.get === "function") {
    const v = headers.get(name);
    if (v != null) return String(v);
  }
  const h = headers as Record<string, string>;
  const lower = name.toLowerCase();
  for (const [k, v] of Object.entries(h)) {
    if (k.toLowerCase() === lower) return String(v);
  }
  return undefined;
}

export type LoginPayload = {
  username: string;
  password: string;
};

export type AuthResponse = {
  token: string;
  expiresIn: number;
};

export async function loginRequest(payload: LoginPayload): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>("/api/v1/auth/login", payload);
  return data;
}
