import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { Modal, Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { useEffect, useState } from "react";

import { haptic } from "@/lib/haptics";

type AddTaskSheetProps = {
  visible: boolean;
  onClose: () => void;
  onSave: (title: string, note: string) => void;
};

export function AddTaskSheet({ visible, onClose, onSave }: AddTaskSheetProps) {
  const [title, setTitle] = useState("");
  const [note, setNote] = useState("");

  useEffect(() => {
    if (!visible) {
      setTitle("");
      setNote("");
    }
  }, [visible]);

  const save = () => {
    if (!title.trim()) return;
    haptic.light();
    onSave(title, note);
    onClose();
  };

  return (
    <Modal animationType="slide" transparent visible={visible} onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose} />
      <View style={styles.sheet}>
        <View style={styles.handle} />
        <View style={styles.topRow}>
          <Text style={styles.heading}>A tiny plan</Text>
          <Pressable accessibilityRole="button" accessibilityLabel="Close add task" onPress={onClose} style={({ pressed }) => [styles.close, pressed && styles.pressed]}>
            <MaterialIcons name="close" size={21} color="#7A5761" />
          </Pressable>
        </View>
        <Text style={styles.label}>WHAT IS ON YOUR MIND?</Text>
        <TextInput
          value={title}
          onChangeText={setTitle}
          placeholder="e.g. Finish assignment"
          placeholderTextColor="#BBA2A8"
          autoFocus
          returnKeyType="done"
          onSubmitEditing={save}
          style={styles.input}
          accessibilityLabel="Task title"
        />
        <Text style={styles.label}>OPTIONAL NOTE</Text>
        <TextInput
          value={note}
          onChangeText={setNote}
          placeholder="A gentle detail, if you need one"
          placeholderTextColor="#BBA2A8"
          returnKeyType="done"
          onSubmitEditing={save}
          style={[styles.input, styles.noteInput]}
          accessibilityLabel="Task note"
        />
        <Pressable accessibilityRole="button" accessibilityLabel="Add task" disabled={!title.trim()} onPress={save} style={({ pressed }) => [styles.save, !title.trim() && styles.saveDisabled, pressed && styles.savePressed]}>
          <Text style={styles.saveText}>Add to today</Text>
          <MaterialIcons name="arrow-forward" size={19} color="#FFFFFF" />
        </Pressable>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { ...StyleSheet.absoluteFillObject, backgroundColor: "rgba(57, 34, 42, 0.32)" },
  sheet: { position: "absolute", bottom: 0, left: 0, right: 0, borderTopLeftRadius: 30, borderTopRightRadius: 30, backgroundColor: "#FFF9F5", padding: 22, paddingBottom: 34 },
  handle: { alignSelf: "center", width: 42, height: 5, borderRadius: 3, backgroundColor: "#EACED3", marginBottom: 18 },
  topRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginBottom: 24 },
  heading: { color: "#4C2D38", fontSize: 23, lineHeight: 30, fontWeight: "700" },
  close: { width: 44, height: 44, borderRadius: 22, backgroundColor: "#FFF0F2", alignItems: "center", justifyContent: "center" },
  label: { color: "#AD7C88", fontSize: 11, letterSpacing: 1.1, fontWeight: "700", marginBottom: 7 },
  input: { minHeight: 52, borderWidth: 1, borderColor: "#EFD8DC", borderRadius: 15, backgroundColor: "#FFFFFF", paddingHorizontal: 15, color: "#4C2D38", fontSize: 16, marginBottom: 17 },
  noteInput: { marginBottom: 24 },
  save: { height: 54, borderRadius: 18, backgroundColor: "#F29AA8", flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 9 },
  saveDisabled: { backgroundColor: "#E8C2C8" },
  savePressed: { opacity: 0.84, transform: [{ scale: 0.98 }] },
  saveText: { color: "#FFFFFF", fontWeight: "700", fontSize: 16 },
  pressed: { opacity: 0.65 },
});
