export type ChibiMood = "idle" | "happy" | "love" | "sleepy" | "excited" | "shy" | "sad";

export type Task = {
  id: string;
  title: string;
  note?: string;
  completed: boolean;
  createdAt: string;
  completedAt?: string;
};

export type AppSettings = {
  favoriteColor: "strawberry" | "lavender" | "peach";
  showTasksOnWidget: boolean;
  personality: "gentle" | "playful" | "supportive";
  customMessages: string[];
};

export type WidgetSnapshot = {
  mood: ChibiMood;
  message: string;
  nextTask?: string;
  completedCount: number;
  totalCount: number;
  showTasks: boolean;
  companionName: string;
  updatedAt: string;
};

export const DEFAULT_SETTINGS: AppSettings = {
  favoriteColor: "strawberry",
  showTasksOnWidget: true,
  personality: "gentle",
  customMessages: [],
};
