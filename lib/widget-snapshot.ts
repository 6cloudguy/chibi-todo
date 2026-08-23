import type { AppSettings, ChibiMood, Task, WidgetSnapshot } from "@/lib/models";

export type WidgetSyncInput = {
  tasks: Task[];
  settings: AppSettings;
  mood: ChibiMood;
  message: string;
};

export function buildWidgetSnapshot(state: WidgetSyncInput): WidgetSnapshot {
  const activeTasks = state.tasks.filter((task) => !task.completed);
  const completedCount = state.tasks.filter((task) => task.completed).length;

  return {
    mood: state.mood,
    message: state.message,
    nextTask: activeTasks[0]?.title,
    completedCount,
    totalCount: state.tasks.length,
    showTasks: state.settings.showTasksOnWidget,
    companionName: state.settings.chibiName,
    updatedAt: new Date().toISOString(),
  };
}
