import AsyncStorage from "@react-native-async-storage/async-storage";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type PropsWithChildren } from "react";

import { messageForMood } from "@/lib/chibi-content";
import { DEFAULT_SETTINGS, type AppSettings, type ChibiMood, type Task } from "@/lib/models";
import { getNativeWidgetSnapshot, syncWidgetSnapshot } from "@/lib/widget-bridge";
import { buildWidgetSnapshot } from "@/lib/widget-snapshot";

const STORAGE_KEY = "@chibi-todo-widget/state/v1";

type PersistedState = {
  tasks: Task[];
  settings: AppSettings;
  mood: ChibiMood;
  message: string;
};

type TaskContextValue = PersistedState & {
  isReady: boolean;
  addTask: (title: string, note?: string) => void;
  toggleTask: (taskId: string) => void;
  deleteTask: (taskId: string) => void;
  updateSettings: (settings: Partial<AppSettings>) => void;
  updateCompanion: (mood: ChibiMood, message: string) => void;
};

const TaskContext = createContext<TaskContextValue | undefined>(undefined);

function normalizeSettings(restored?: Partial<AppSettings>): AppSettings {
  const favoriteColor = restored?.favoriteColor;
  const personality = restored?.personality;
  return {
    favoriteColor: favoriteColor === "lavender" || favoriteColor === "peach" || favoriteColor === "strawberry" ? favoriteColor : DEFAULT_SETTINGS.favoriteColor,
    showTasksOnWidget: typeof restored?.showTasksOnWidget === "boolean" ? restored.showTasksOnWidget : DEFAULT_SETTINGS.showTasksOnWidget,
    personality: personality === "playful" || personality === "supportive" || personality === "gentle" ? personality : DEFAULT_SETTINGS.personality,
    customMessages: Array.isArray(restored?.customMessages) ? restored.customMessages.filter((message): message is string => typeof message === "string") : [],
  };
}

export function TaskProvider({ children }: PropsWithChildren) {
  const [isReady, setIsReady] = useState(false);
  const [state, setState] = useState<PersistedState>({
    tasks: [],
    settings: DEFAULT_SETTINGS,
    mood: "idle",
    message: "you got this!",
  });

  useEffect(() => {
    let mounted = true;
    Promise.all([AsyncStorage.getItem(STORAGE_KEY), getNativeWidgetSnapshot()])
      .then(([value, nativeSnapshot]) => {
        if (!mounted) return;
        const restored = value ? (JSON.parse(value) as Partial<PersistedState>) : {};
        setState((current) => ({
          tasks: restored.tasks ?? current.tasks,
          settings: normalizeSettings(restored.settings),
          mood: (nativeSnapshot?.mood as ChibiMood | undefined) ?? restored.mood ?? current.mood,
          message: nativeSnapshot?.message ?? restored.message ?? current.message,
        }));
      })
      .catch(() => undefined)
      .finally(() => {
        if (mounted) setIsReady(true);
      });
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (!isReady) return;
    void AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    void syncWidgetSnapshot(buildWidgetSnapshot(state));
  }, [isReady, state]);

  const addTask = useCallback((title: string, note?: string) => {
    const cleanTitle = title.trim();
    if (!cleanTitle) return;
    setState((current) => ({
      ...current,
      tasks: [
        {
          id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
          title: cleanTitle,
          note: note?.trim() || undefined,
          completed: false,
          createdAt: new Date().toISOString(),
        },
        ...current.tasks,
      ],
    }));
  }, []);

  const toggleTask = useCallback((taskId: string) => {
    setState((current) => ({
      ...current,
      tasks: current.tasks.map((task) =>
        task.id === taskId
          ? { ...task, completed: !task.completed, completedAt: task.completed ? undefined : new Date().toISOString() }
          : task,
      ),
    }));
  }, []);

  const deleteTask = useCallback((taskId: string) => {
    setState((current) => ({ ...current, tasks: current.tasks.filter((task) => task.id !== taskId) }));
  }, []);

  const updateSettings = useCallback((settings: Partial<AppSettings>) => {
    setState((current) => ({ ...current, settings: { ...current.settings, ...settings } }));
  }, []);

  const updateCompanion = useCallback((mood: ChibiMood, message: string) => {
    setState((current) => ({ ...current, mood, message: message || messageForMood(mood, current.settings.customMessages, current.message) }));
  }, []);

  const value = useMemo<TaskContextValue>(
    () => ({ ...state, isReady, addTask, toggleTask, deleteTask, updateSettings, updateCompanion }),
    [addTask, deleteTask, isReady, state, toggleTask, updateCompanion, updateSettings],
  );

  return <TaskContext.Provider value={value}>{children}</TaskContext.Provider>;
}

export function useTasks() {
  const context = useContext(TaskContext);
  if (!context) throw new Error("useTasks must be used within TaskProvider");
  return context;
}

export function taskSummary(tasks: Task[]) {
  const completed = tasks.filter((task) => task.completed).length;
  return { completed, total: tasks.length, progress: tasks.length === 0 ? 0 : completed / tasks.length };
}
