import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        bg: {
          primary:   "#0A0E1A",
          secondary: "#0F1729",
          tertiary:  "#1A2238",
          elevated:  "#111827",
        },
        gold: {
          DEFAULT: "#D4A574",
          light:   "#F5C77D",
          dark:    "#B8894F",
          subtle:  "#3D2F1F",
        },
        success: { DEFAULT: "#10B981", bg: "#064E3B" },
        danger:  { DEFAULT: "#EF4444", bg: "#7F1D1D" },
        warning: { DEFAULT: "#F59E0B" },
        info:    { DEFAULT: "#3B82F6" },
        border: {
          subtle:  "#1F2937",
          default: "#374151",
          focus:   "#D4A574",
        },
        text: {
          primary:   "#FFFFFF",
          secondary: "#E5E7EB",
          muted:     "#9CA3AF",
          subtle:    "#6B7280",
        },
      },
      fontFamily: {
        sans:    ["Lexend", "system-ui", "sans-serif"],
        display: ["Lexend", "system-ui", "sans-serif"],
      },
      borderRadius: {
        xl:    "12px",
        "2xl": "16px",
      },
      backgroundImage: {
        "grid-subtle":
          "linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px)",
      },
      backgroundSize: {
        grid: "32px 32px",
      },
    },
  },
  plugins: [],
} satisfies Config;
