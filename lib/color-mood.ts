import type { AppSettings } from "@/lib/models";

export type ColorMood = {
  background: string;
  surface: string;
  accent: string;
  accentDeep: string;
  border: string;
  ink: string;
  muted: string;
  completed: string;
  completedSurface: string;
};

export const COLOR_MOODS: Record<AppSettings["favoriteColor"], ColorMood> = {
  strawberry: {
    background: "#FFF9F5", surface: "#FFF0F2", accent: "#F29AA8", accentDeep: "#C85F70", border: "#EFD8DC", ink: "#4C2D38", muted: "#916A75", completed: "#9ACBB7", completedSurface: "#FFF6F7",
  },
  lavender: {
    background: "#FAF8FF", surface: "#F0ECFF", accent: "#A89AE8", accentDeep: "#7160BB", border: "#DFD8F4", ink: "#40375D", muted: "#776D98", completed: "#9BBFAF", completedSurface: "#F5F2FF",
  },
  peach: {
    background: "#FFF9F2", surface: "#FFF0E2", accent: "#EDA86F", accentDeep: "#B96D3A", border: "#F3DAC2", ink: "#573C2D", muted: "#8D6851", completed: "#9BBE9A", completedSurface: "#FFF6ED",
  },
};

export function getColorMood(color: AppSettings["favoriteColor"]) {
  return COLOR_MOODS[color];
}
