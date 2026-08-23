import { NativeModules, Platform } from "react-native";

import type { WidgetSnapshot } from "@/lib/models";

type ChibiWidgetNativeModule = {
  saveWidgetSnapshot(snapshot: string): Promise<void>;
  getWidgetSnapshot(): Promise<string | null>;
  refreshWidgets(): Promise<void>;
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
