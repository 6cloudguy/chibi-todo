import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { haptic } from "@/lib/haptics";
import type { ColorMood } from "@/lib/color-mood";
import type { Task } from "@/lib/models";

type TaskRowProps = {
  task: Task;
  palette: ColorMood;
  onToggle: () => void;
  onDelete: () => void;
};

export function TaskRow({ task, palette, onToggle, onDelete }: TaskRowProps) {
  const handleToggle = () => {
    if (task.completed) {
      haptic.light();
    } else {
      haptic.success();
    }
    onToggle();
  };

  return (
    <View style={[styles.row, { borderColor: palette.border }, task.completed && { backgroundColor: palette.completedSurface }]}>
      <Pressable
        accessibilityRole="checkbox"
        accessibilityState={{ checked: task.completed }}
        accessibilityLabel={`${task.completed ? "Completed" : "Incomplete"} task: ${task.title}`}
        onPress={handleToggle}
        style={({ pressed }) => [styles.check, { borderColor: palette.border }, task.completed && { backgroundColor: palette.completed, borderColor: palette.completed }, pressed && styles.pressed]}
      >
        {task.completed ? <MaterialIcons name="check" size={17} color="#FFFFFF" /> : null}
      </Pressable>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`Toggle task: ${task.title}`}
        onPress={handleToggle}
        style={({ pressed }) => [styles.textArea, pressed && styles.pressed]}
      >
        <Text style={[styles.title, { color: palette.ink }, task.completed && styles.titleDone]} numberOfLines={1}>{task.title}</Text>
        {task.note ? <Text style={[styles.note, { color: palette.muted }, task.completed && styles.noteDone]} numberOfLines={1}>{task.note}</Text> : null}
      </Pressable>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`Delete task: ${task.title}`}
        hitSlop={10}
        onPress={() => {
          haptic.medium();
          onDelete();
        }}
        style={({ pressed }) => [styles.delete, pressed && styles.pressed]}
      >
        <MaterialIcons name="close" size={20} color={palette.muted} />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { minHeight: 68, flexDirection: "row", alignItems: "center", borderRadius: 20, backgroundColor: "#FFFFFF", borderWidth: 1, paddingHorizontal: 14, marginBottom: 10, shadowColor: "#704553", shadowOpacity: 0.05, shadowRadius: 12, shadowOffset: { width: 0, height: 4 }, elevation: 1 },
  check: { width: 28, height: 28, borderRadius: 14, borderWidth: 1.5, alignItems: "center", justifyContent: "center", marginRight: 12 },
  textArea: { flex: 1, paddingVertical: 11 },
  title: { fontSize: 16, lineHeight: 21, fontWeight: "600" },
  titleDone: { color: "#9E7982", textDecorationLine: "line-through" },
  note: { fontSize: 12, lineHeight: 17, marginTop: 2 },
  noteDone: { color: "#B8A4A9" },
  delete: { width: 36, height: 44, alignItems: "center", justifyContent: "center" },
  pressed: { opacity: 0.62 },
});
