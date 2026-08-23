import { NativeModules, Platform } from "react-native";

import type { WidgetSnapshot } from "@/lib/models";

type ChibiWidgetNativeModule = {
  saveWidgetSnapshot(snapshot: string): Promise<void>;
  getWidgetSnapshot(): Promise<string | null>;
  refreshWidgets(): Promise<void>;
  isDesktopPetPermissionGranted(): Promise<boolean>;
  isLauncherUsageAccessGranted(): Promise<boolean>;
  isDesktopPetEnabled(): Promise<boolean>;
  openDesktopPetPermissionSettings(): Promise<void>;
  openLauncherUsageAccessSettings(): Promise<void>;
  startDesktopPet(): Promise<void>;
  stopDesktopPet(): Promise<void>;
};

const nativeWidget = NativeModules.ChibiWidgetBridge as ChibiWidgetNativeModule | undefined;

export async function syncWidgetSnapshot(snapshot: WidgetSnapshot) {
  if (Platform.OS !== "android" || !nativeWidget) return;
  await nativeWidget.saveWidgetSnapshot(JSON.stringify(snapshot));
  await nativeWidget.refreshWidgets();
}

export async function getNativeWidgetSnapshot(): Promise<Partial<WidgetSnapshot> | null> {
  if (Platform.OS !== "android" || !nativeWidget) return null;
  const serialized = await nativeWidget.getWidgetSnapshot();
  if (!serialized) return null;
  try {
    return JSON.parse(serialized) as Partial<WidgetSnapshot>;
  } catch {
    return null;
  }
}

export async function desktopPetPermissionGranted() {
  return Platform.OS === "android" && nativeWidget ? nativeWidget.isDesktopPetPermissionGranted() : false;
}

export async function launcherUsageAccessGranted() {
  return Platform.OS === "android" && nativeWidget ? nativeWidget.isLauncherUsageAccessGranted() : false;
}

export async function desktopPetEnabled() {
  return Platform.OS === "android" && nativeWidget ? nativeWidget.isDesktopPetEnabled() : false;
}

export async function openDesktopPetPermissionSettings() {
  if (Platform.OS === "android" && nativeWidget) await nativeWidget.openDesktopPetPermissionSettings();
}

export async function openLauncherUsageAccessSettings() {
  if (Platform.OS === "android" && nativeWidget) await nativeWidget.openLauncherUsageAccessSettings();
}

export async function startDesktopPet() {
  if (Platform.OS === "android" && nativeWidget) await nativeWidget.startDesktopPet();
}

export async function stopDesktopPet() {
  if (Platform.OS === "android" && nativeWidget) await nativeWidget.stopDesktopPet();
}
