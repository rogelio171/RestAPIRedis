import type { Metadata } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";

import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  display: "swap",
});

const jetbrains = JetBrains_Mono({
  variable: "--font-mono",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "RestAPIRedis · Dashboard",
  description: "Redis cache performance, key inspection, and geospatial API tools",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${jetbrains.variable} h-full antialiased`}>
      <body
        className={`${inter.className} min-h-full bg-[var(--background)] text-[var(--foreground)]`}
      >
        {children}
      </body>
    </html>
  );
}
