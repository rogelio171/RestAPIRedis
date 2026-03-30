"use client";

import { useState } from "react";

import { loginRequest } from "@/lib/api";
import { setStoredToken } from "@/lib/auth";

import { btnPrimary, card, codeInline, input } from "./ui";

type Props = {
  onLoggedIn: () => void;
  apiBaseUrl: string;
};

export function LoginForm({ onLoggedIn, apiBaseUrl }: Props) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const { token } = await loginRequest({ username, password });
      setStoredToken(token);
      onLoggedIn();
    } catch {
      setError("Sign-in failed. Confirm the API is running and credentials are valid.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-full flex-col items-center justify-center px-4 py-16">
      <div className="w-full max-w-[380px]">
        <div className="mb-10 text-center">
          <h1 className="text-2xl font-semibold tracking-tight text-stone-900 dark:text-stone-50">
            RestAPIRedis
          </h1>
          <p className="mt-2 text-sm text-stone-500 dark:text-stone-400">
            Dashboard · cache &amp; geospatial tools
          </p>
        </div>
        <div className={`${card} p-8`}>
          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label htmlFor="username" className="mb-1.5 block text-xs font-medium text-stone-600 dark:text-stone-400">
                Username
              </label>
              <input
                id="username"
                type="text"
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className={`w-full ${input}`}
                required
              />
            </div>
            <div>
              <label htmlFor="password" className="mb-1.5 block text-xs font-medium text-stone-600 dark:text-stone-400">
                Password
              </label>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={`w-full ${input}`}
                required
              />
            </div>
            {error && (
              <p className="text-sm text-red-600 dark:text-red-400" role="alert">
                {error}
              </p>
            )}
            <button type="submit" disabled={loading} className={`w-full ${btnPrimary}`}>
              {loading ? "Signing in…" : "Continue"}
            </button>
          </form>
        </div>
        <p className="mt-8 text-center text-xs leading-relaxed text-stone-500 dark:text-stone-500">
          Register first with{" "}
          <code className={codeInline}>POST /api/v1/auth/register</code>
          <br />
          <span className="mt-2 inline-block font-mono text-[11px] text-stone-400">{apiBaseUrl}</span>
        </p>
      </div>
    </div>
  );
}
