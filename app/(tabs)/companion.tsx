import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { ChibiPortrait } from "@/components/chibi-portrait";
import { ScreenContainer } from "@/components/screen-container";
import { CHIBI_MOODS, messageForMood, nextMood } from "@/lib/chibi-content";
import { haptic } from "@/lib/haptics";
import type { ChibiMood } from "@/lib/models";
import { useTasks } from "@/lib/task-context";

const moodLabels: Record<ChibiMood, string> = {
  idle: "quietly here",
  happy: "so happy for you",
  love: "sending love",
  sleepy: "ready to rest",
  excited: "ready to cheer",
  shy: "a little bashful",
  sad: "being gentle",
};

export default function CompanionScreen() {
  const { mood, message, settings, updateCompanion } = useTasks();

  const chooseMood = (newMood: ChibiMood) => {
    haptic.light();
    updateCompanion(newMood, messageForMood(newMood, settings.customMessages, message));
  };

  const cycleMood = () => chooseMood(nextMood(mood));

  return (
    <ScreenContainer containerClassName="bg-[#FFF9F5]" className="flex-1">
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <Text style={styles.eyebrow}>YOUR LITTLE COMPANION</Text>
        <Text style={styles.title}>{settings.chibiName}</Text>
        <Text style={styles.subtitle}>{moodLabels[mood]}</Text>
        <View style={styles.portraitWrap}>
          <View style={styles.floatHeartOne}><Text style={styles.heart}>♡</Text></View>
          <View style={styles.floatHeartTwo}><Text style={styles.heartSmall}>✦</Text></View>
          <ChibiPortrait mood={mood} />
        </View>
        <View style={styles.messageCard}>
          <MaterialIcons name="format-quote" size={21} color="#D78390" />
          <Text style={styles.message}>{message}</Text>
        </View>
        <Pressable accessibilityRole="button" accessibilityLabel="Change companion mood" onPress={cycleMood} style={({ pressed }) => [styles.primaryButton, pressed && styles.primaryButtonPressed]}>
          <MaterialIcons name="auto-awesome" size={18} color="#FFFFFF" />
          <Text style={styles.primaryButtonText}>A new little mood</Text>
        </Pressable>
        <Text style={styles.pickerLabel}>CHOOSE A FEELING</Text>
        <View style={styles.moodGrid}>
          {CHIBI_MOODS.map((option) => (
            <Pressable
              key={option}
              accessibilityRole="button"
              accessibilityState={{ selected: mood === option }}
              accessibilityLabel={`Set mood to ${option}`}
              onPress={() => chooseMood(option)}
              style={({ pressed }) => [styles.moodChip, mood === option && styles.moodChipActive, pressed && styles.chipPressed]}
            >
              <Text style={[styles.moodChipText, mood === option && styles.moodChipTextActive]}>{option}</Text>
            </Pressable>
          ))}
        </View>
        <View style={styles.widgetHint}>
          <View style={styles.widgetIcon}><MaterialIcons name="widgets" size={20} color="#D78390" /></View>
          <View style={styles.widgetCopy}>
            <Text style={styles.widgetTitle}>Made for the home screen</Text>
            <Text style={styles.widgetBody}>Place Momo directly from Widgets, long-press to move her, then tap “pet Momo” for tiny play moments.</Text>
          </View>
        </View>
      </ScrollView>
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  content: { padding: 20, paddingBottom: 112, alignItems: "center" },
  eyebrow: { color: "#AD7C88", fontWeight: "700", fontSize: 11, letterSpacing: 1.15, marginTop: 3 },
  title: { color: "#4C2D38", fontWeight: "700", fontSize: 34, lineHeight: 43, marginTop: 3 },
  subtitle: { color: "#96727B", fontSize: 14, lineHeight: 21 },
  portraitWrap: { width: 265, height: 255, marginTop: 8, alignItems: "center", justifyContent: "center" },
  floatHeartOne: { position: "absolute", top: 25, left: 8, width: 36, height: 36, borderRadius: 18, backgroundColor: "#FAD6DC", alignItems: "center", justifyContent: "center" },
  floatHeartTwo: { position: "absolute", right: 8, bottom: 38, width: 30, height: 30, borderRadius: 15, backgroundColor: "#EDE7FA", alignItems: "center", justifyContent: "center" },
  heart: { color: "#E98B9A", fontSize: 22 },
  heartSmall: { color: "#9A86BC", fontSize: 15 },
  messageCard: { width: "100%", minHeight: 69, flexDirection: "row", alignItems: "center", gap: 8, backgroundColor: "#FFFFFF", borderRadius: 19, paddingHorizontal: 17, shadowColor: "#704553", shadowOpacity: 0.05, shadowRadius: 12, shadowOffset: { width: 0, height: 4 }, elevation: 1 },
  message: { flex: 1, color: "#5D3A45", fontSize: 16, fontWeight: "600", lineHeight: 22 },
  primaryButton: { width: "100%", height: 52, marginTop: 14, borderRadius: 17, flexDirection: "row", justifyContent: "center", alignItems: "center", gap: 8, backgroundColor: "#F29AA8" },
  primaryButtonPressed: { opacity: 0.86, transform: [{ scale: 0.98 }] },
  primaryButtonText: { color: "#FFFFFF", fontSize: 15, fontWeight: "700" },
  pickerLabel: { alignSelf: "flex-start", color: "#AD7C88", fontSize: 11, fontWeight: "700", letterSpacing: 1.1, marginTop: 27, marginBottom: 10 },
  moodGrid: { flexDirection: "row", flexWrap: "wrap", gap: 8, width: "100%" },
  moodChip: { height: 37, borderRadius: 19, paddingHorizontal: 14, backgroundColor: "#FFFFFF", justifyContent: "center", borderWidth: 1, borderColor: "#F0DDE0" },
  moodChipActive: { backgroundColor: "#FFF0F2", borderColor: "#F29AA8" },
  moodChipText: { color: "#8F6B75", fontSize: 13, fontWeight: "600", textTransform: "capitalize" },
  moodChipTextActive: { color: "#C85F70" },
  chipPressed: { opacity: 0.65 },
  widgetHint: { width: "100%", flexDirection: "row", alignItems: "center", gap: 12, marginTop: 28, padding: 14, backgroundColor: "#F6F0FF", borderRadius: 18 },
  widgetIcon: { width: 38, height: 38, borderRadius: 19, backgroundColor: "#FFFFFF", alignItems: "center", justifyContent: "center" },
  widgetCopy: { flex: 1 },
  widgetTitle: { color: "#604A70", fontSize: 13, fontWeight: "700", lineHeight: 18 },
  widgetBody: { color: "#836F91", fontSize: 12, lineHeight: 17, marginTop: 2 },
});
