import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useMemo, useState } from "react";
import { ActivityIndicator, Pressable, SectionList, StyleSheet, Text, View } from "react-native";

import { AddTaskSheet } from "@/components/add-task-sheet";
import { TaskRow } from "@/components/task-row";
import { ScreenContainer } from "@/components/screen-container";
import { taskSummary, useTasks } from "@/lib/task-context";
import type { Task } from "@/lib/models";

type TaskSection = { title: string; data: Task[] };

function todayLabel() {
  return new Intl.DateTimeFormat("en", { weekday: "long", month: "short", day: "numeric" }).format(new Date());
}

export default function HomeScreen() {
  const [addSheetOpen, setAddSheetOpen] = useState(false);
  const { tasks, isReady, addTask, deleteTask, toggleTask } = useTasks();
  const summary = taskSummary(tasks);
  const sections = useMemo<TaskSection[]>(() => {
    const active = tasks.filter((task) => !task.completed);
    const completed = tasks.filter((task) => task.completed);
    return [
      ...(active.length ? [{ title: "TODAY", data: active }] : []),
      ...(completed.length ? [{ title: "COMPLETED", data: completed }] : []),
    ];
  }, [tasks]);

  if (!isReady) {
    return <ScreenContainer containerClassName="bg-[#FFF9F5]" className="items-center justify-center"><ActivityIndicator color="#F29AA8" /></ScreenContainer>;
  }

  return (
    <ScreenContainer containerClassName="bg-[#FFF9F5]" className="flex-1">
      <SectionList
        sections={sections}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => <TaskRow task={item} onToggle={() => toggleTask(item.id)} onDelete={() => deleteTask(item.id)} />}
        renderSectionHeader={({ section }) => <Text style={styles.sectionTitle}>{section.title}</Text>}
        showsVerticalScrollIndicator={false}
        stickySectionHeadersEnabled={false}
        contentContainerStyle={[styles.listContent, tasks.length === 0 && styles.emptyContent]}
        ListHeaderComponent={
          <View>
            <Text style={styles.eyebrow}>{todayLabel()}</Text>
            <View style={styles.titleRow}>
              <Text style={styles.title}>today <Text style={styles.heart}>♡</Text></Text>
              <Text style={styles.count}>{summary.completed} / {summary.total} done</Text>
            </View>
            <View style={styles.progressTrack} accessibilityLabel={`${summary.completed} of ${summary.total} tasks completed`}>
              <View style={[styles.progressFill, { width: `${summary.progress * 100}%` }]} />
            </View>
            <View style={styles.companionNote}>
              <View style={styles.sparkle}><Text style={styles.sparkleText}>✦</Text></View>
              <Text style={styles.companionText}>{tasks.length === 0 ? "A soft little place for your day." : "A little at a time is still enough."}</Text>
            </View>
          </View>
        }
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <View style={styles.emptyDoodle}><Text style={styles.emptyDoodleText}>♡</Text></View>
            <Text style={styles.emptyTitle}>Nothing to carry yet</Text>
            <Text style={styles.emptyBody}>Add one tiny thing, then let the day meet you there.</Text>
          </View>
        }
      />
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Add a task"
        onPress={() => setAddSheetOpen(true)}
        style={({ pressed }) => [styles.fab, pressed && styles.fabPressed]}
      >
        <MaterialIcons name="add" size={28} color="#FFFFFF" />
      </Pressable>
      <AddTaskSheet visible={addSheetOpen} onClose={() => setAddSheetOpen(false)} onSave={addTask} />
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  listContent: { paddingHorizontal: 20, paddingTop: 18, paddingBottom: 120 },
  emptyContent: { flexGrow: 1 },
  eyebrow: { color: "#AD7C88", fontWeight: "700", fontSize: 12, letterSpacing: 1.25, textTransform: "uppercase", marginBottom: 4 },
  titleRow: { flexDirection: "row", alignItems: "baseline", justifyContent: "space-between" },
  title: { color: "#4C2D38", fontSize: 35, lineHeight: 44, fontWeight: "700", letterSpacing: -1.1 },
  heart: { color: "#F29AA8" },
  count: { color: "#916A75", fontSize: 13, fontWeight: "600" },
  progressTrack: { height: 7, backgroundColor: "#F2DDE1", overflow: "hidden", borderRadius: 4, marginTop: 17 },
  progressFill: { height: "100%", borderRadius: 4, backgroundColor: "#F29AA8" },
  companionNote: { marginTop: 18, backgroundColor: "#FFF0F2", borderRadius: 18, minHeight: 54, paddingHorizontal: 14, flexDirection: "row", alignItems: "center", gap: 10 },
  sparkle: { width: 27, height: 27, borderRadius: 14, backgroundColor: "#F4A6AF", alignItems: "center", justifyContent: "center" },
  sparkleText: { color: "#FFFFFF", fontSize: 14 },
  companionText: { color: "#78535E", fontSize: 13, lineHeight: 19, fontWeight: "600", flex: 1 },
  sectionTitle: { color: "#AD7C88", fontSize: 11, letterSpacing: 1.25, fontWeight: "700", marginTop: 28, marginBottom: 10 },
  emptyState: { flex: 1, alignItems: "center", justifyContent: "center", paddingBottom: 40 },
  emptyDoodle: { width: 74, height: 74, borderRadius: 37, backgroundColor: "#FFF0F2", alignItems: "center", justifyContent: "center", marginBottom: 15 },
  emptyDoodleText: { color: "#F29AA8", fontSize: 33 },
  emptyTitle: { color: "#4C2D38", fontSize: 19, lineHeight: 26, fontWeight: "700" },
  emptyBody: { color: "#96727B", textAlign: "center", fontSize: 14, lineHeight: 21, marginTop: 5, maxWidth: 230 },
  fab: { position: "absolute", right: 22, bottom: 26, width: 58, height: 58, borderRadius: 29, backgroundColor: "#F29AA8", justifyContent: "center", alignItems: "center", shadowColor: "#A54759", shadowOpacity: 0.24, shadowRadius: 13, shadowOffset: { width: 0, height: 7 }, elevation: 5 },
  fabPressed: { transform: [{ scale: 0.97 }], opacity: 0.9 },
});
