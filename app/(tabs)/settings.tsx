import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useState } from "react";
import { Pressable, ScrollView, StyleSheet, Switch, Text, TextInput, View } from "react-native";

import { ScreenContainer } from "@/components/screen-container";
import { haptic } from "@/lib/haptics";
import type { AppSettings } from "@/lib/models";
import { useTasks } from "@/lib/task-context";

const themes: { value: AppSettings["favoriteColor"]; label: string; color: string }[] = [
  { value: "strawberry", label: "Strawberry", color: "#F4A6AF" },
  { value: "lavender", label: "Lavender", color: "#BFAEE6" },
  { value: "peach", label: "Peach", color: "#F2BA8C" },
];

const personalities: AppSettings["personality"][] = ["gentle", "playful", "supportive"];

export default function SettingsScreen() {
  const { settings, updateSettings } = useTasks();
  const [messageDraft, setMessageDraft] = useState("");

  const updateText = (key: "girlfriendName" | "chibiName", value: string) => updateSettings({ [key]: value });
  const addMessage = () => {
    const cleaned = messageDraft.trim();
    if (!cleaned || settings.customMessages.includes(cleaned)) return;
    haptic.light();
    updateSettings({ customMessages: [...settings.customMessages, cleaned] });
    setMessageDraft("");
  };

  return (
    <ScreenContainer containerClassName="bg-[#FFF9F5]" className="flex-1">
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
        <Text style={styles.eyebrow}>MAKE IT YOURS</Text>
        <Text style={styles.title}>settings</Text>
        <SectionTitle label="NAMES" />
        <View style={styles.card}>
          <Field label="GIRLFRIEND'S NICKNAME" value={settings.girlfriendName} placeholder="A sweet name, if you want" onChangeText={(value) => updateText("girlfriendName", value)} />
          <Divider />
          <Field label="CHIBI NAME" value={settings.chibiName} placeholder="Momo" onChangeText={(value) => updateText("chibiName", value)} />
        </View>
        <SectionTitle label="COLOR MOOD" />
        <View style={styles.themeRow}>
          {themes.map((theme) => (
            <Pressable key={theme.value} accessibilityRole="button" accessibilityState={{ selected: settings.favoriteColor === theme.value }} onPress={() => { haptic.selection(); updateSettings({ favoriteColor: theme.value }); }} style={({ pressed }) => [styles.themeChoice, settings.favoriteColor === theme.value && styles.themeChoiceActive, pressed && styles.pressed]}>
              <View style={[styles.themeDot, { backgroundColor: theme.color }]} />
              <Text style={[styles.themeLabel, settings.favoriteColor === theme.value && styles.themeLabelActive]}>{theme.label}</Text>
            </Pressable>
          ))}
        </View>
        <SectionTitle label="WIDGET" />
        <View style={styles.card}>
          <View style={styles.settingRow}>
            <View style={styles.settingCopy}><Text style={styles.settingTitle}>Show my next task</Text><Text style={styles.settingBody}>Keep a tiny reminder below the companion.</Text></View>
            <Switch value={settings.showTasksOnWidget} onValueChange={(value) => { haptic.medium(); updateSettings({ showTasksOnWidget: value }); }} trackColor={{ false: "#E7D6DA", true: "#F2A8B3" }} thumbColor="#FFFFFF" accessibilityLabel="Show next task on widget" />
          </View>
        </View>
        <SectionTitle label="PERSONALITY" />
        <View style={styles.personalityRow}>
          {personalities.map((personality) => (
            <Pressable key={personality} accessibilityRole="button" accessibilityState={{ selected: settings.personality === personality }} onPress={() => { haptic.selection(); updateSettings({ personality }); }} style={({ pressed }) => [styles.personalityChip, settings.personality === personality && styles.personalityChipActive, pressed && styles.pressed]}>
              <Text style={[styles.personalityText, settings.personality === personality && styles.personalityTextActive]}>{personality}</Text>
            </Pressable>
          ))}
        </View>
        <SectionTitle label="YOUR OWN LITTLE MESSAGES" />
        <View style={styles.messageInputRow}>
          <TextInput value={messageDraft} onChangeText={setMessageDraft} placeholder="Something sweet to hear" placeholderTextColor="#BBA2A8" returnKeyType="done" onSubmitEditing={addMessage} style={styles.messageInput} accessibilityLabel="Custom chibi message" />
          <Pressable accessibilityRole="button" accessibilityLabel="Add custom message" onPress={addMessage} style={({ pressed }) => [styles.addMessageButton, pressed && styles.pressed]}><MaterialIcons name="add" size={21} color="#FFFFFF" /></Pressable>
        </View>
        {settings.customMessages.length > 0 ? <View style={styles.customList}>{settings.customMessages.map((message) => <View style={styles.customMessage} key={message}><Text style={styles.customMessageText}>“{message}”</Text><Pressable accessibilityRole="button" accessibilityLabel={`Remove message: ${message}`} onPress={() => updateSettings({ customMessages: settings.customMessages.filter((item) => item !== message) })} hitSlop={10}><MaterialIcons name="close" size={19} color="#AD7C88" /></Pressable></View>)}</View> : <Text style={styles.emptyMessages}>Add your own message for a more personal widget.</Text>}
      </ScrollView>
    </ScreenContainer>
  );
}

function SectionTitle({ label }: { label: string }) { return <Text style={styles.sectionTitle}>{label}</Text>; }
function Divider() { return <View style={styles.divider} />; }
function Field({ label, value, placeholder, onChangeText }: { label: string; value: string; placeholder: string; onChangeText: (value: string) => void }) {
  return <View><Text style={styles.fieldLabel}>{label}</Text><TextInput value={value} placeholder={placeholder} placeholderTextColor="#BBA2A8" onChangeText={onChangeText} style={styles.fieldInput} accessibilityLabel={label} /></View>;
}

const styles = StyleSheet.create({
  content: { padding: 20, paddingBottom: 112 },
  eyebrow: { color: "#AD7C88", fontWeight: "700", fontSize: 11, letterSpacing: 1.15, marginTop: 3 },
  title: { color: "#4C2D38", fontWeight: "700", fontSize: 34, lineHeight: 43, letterSpacing: -1.1 },
  sectionTitle: { color: "#AD7C88", fontSize: 11, letterSpacing: 1.1, fontWeight: "700", marginTop: 26, marginBottom: 9 },
  card: { backgroundColor: "#FFFFFF", borderRadius: 19, paddingHorizontal: 16, shadowColor: "#704553", shadowOpacity: 0.05, shadowRadius: 12, shadowOffset: { width: 0, height: 4 }, elevation: 1 },
  fieldLabel: { color: "#AD7C88", fontSize: 10, letterSpacing: 1, fontWeight: "700", marginTop: 14, marginBottom: 2 },
  fieldInput: { color: "#4C2D38", fontSize: 16, fontWeight: "600", minHeight: 42, paddingVertical: 4 },
  divider: { height: StyleSheet.hairlineWidth, backgroundColor: "#F0DFE2", marginVertical: 3 },
  themeRow: { flexDirection: "row", gap: 8 },
  themeChoice: { flex: 1, backgroundColor: "#FFFFFF", minHeight: 74, borderRadius: 17, justifyContent: "center", alignItems: "center", borderWidth: 1, borderColor: "#F0DFE2", gap: 6 },
  themeChoiceActive: { borderColor: "#F29AA8", backgroundColor: "#FFF4F5" },
  themeDot: { width: 21, height: 21, borderRadius: 11 },
  themeLabel: { color: "#916A75", fontSize: 11, fontWeight: "700" },
  themeLabelActive: { color: "#C85F70" },
  settingRow: { flexDirection: "row", alignItems: "center", minHeight: 82 },
  settingCopy: { flex: 1, paddingRight: 10 },
  settingTitle: { color: "#4C2D38", fontSize: 15, lineHeight: 21, fontWeight: "700" },
  settingBody: { color: "#96727B", fontSize: 12, lineHeight: 17, marginTop: 2 },
  personalityRow: { flexDirection: "row", gap: 8, flexWrap: "wrap" },
  personalityChip: { minHeight: 38, borderRadius: 19, paddingHorizontal: 15, justifyContent: "center", backgroundColor: "#FFFFFF", borderWidth: 1, borderColor: "#F0DFE2" },
  personalityChipActive: { backgroundColor: "#FFF0F2", borderColor: "#F29AA8" },
  personalityText: { color: "#916A75", fontSize: 13, fontWeight: "600", textTransform: "capitalize" },
  personalityTextActive: { color: "#C85F70" },
  messageInputRow: { flexDirection: "row", alignItems: "center", gap: 9 },
  messageInput: { flex: 1, minHeight: 50, borderRadius: 16, borderColor: "#EFD8DC", borderWidth: 1, backgroundColor: "#FFFFFF", paddingHorizontal: 14, color: "#4C2D38", fontSize: 14 },
  addMessageButton: { width: 50, height: 50, borderRadius: 16, alignItems: "center", justifyContent: "center", backgroundColor: "#F29AA8" },
  customList: { marginTop: 11, gap: 7 },
  customMessage: { minHeight: 45, paddingHorizontal: 13, borderRadius: 14, backgroundColor: "#FFF0F2", flexDirection: "row", alignItems: "center", gap: 8 },
  customMessageText: { flex: 1, color: "#79525D", fontSize: 13, lineHeight: 18, fontWeight: "600" },
  emptyMessages: { color: "#96727B", fontSize: 13, lineHeight: 19, marginTop: 10 },
  pressed: { opacity: 0.65 },
});
