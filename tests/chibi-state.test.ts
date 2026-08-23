import { afterEach, describe, expect, it, vi } from "vitest";

import { messageForMood, nextMood } from "../lib/chibi-content";
import { DEFAULT_SETTINGS, type Task } from "../lib/models";
import { buildWidgetSnapshot } from "../lib/widget-snapshot";

const activeTask: Task = {
  id: "active",
  title: "Finish assignment",
  completed: false,
  createdAt: "2026-08-23T00:00:00.000Z",
};

const completedTask: Task = {
  id: "completed",
  title: "Drink some water",
  completed: true,
  createdAt: "2026-08-23T00:00:00.000Z",
  completedAt: "2026-08-23T01:00:00.000Z",
};

afterEach(() => vi.restoreAllMocks());

describe("chibi state", () => {
  it("cycles through the mood collection and returns to idle", () => {
    expect(nextMood("idle")).toBe("happy");
    expect(nextMood("sad")).toBe("idle");
  });

  it("avoids immediately repeating the last message when another message exists", () => {
    vi.spyOn(Math, "random").mockReturnValue(0);
    expect(messageForMood("happy", ["a custom hello"], "a custom hello")).not.toBe("a custom hello");
  });

  it("serializes the next incomplete task and completion counts for the widget", () => {
    const snapshot = buildWidgetSnapshot({
      tasks: [activeTask, completedTask],
      settings: { ...DEFAULT_SETTINGS, chibiName: "Momo", showTasksOnWidget: true },
      mood: "love",
      message: "miss you ♡",
    });

    expect(snapshot).toMatchObject({
      mood: "love",
      message: "miss you ♡",
      nextTask: "Finish assignment",
      completedCount: 1,
      totalCount: 2,
      showTasks: true,
      companionName: "Momo",
    });
    expect(snapshot.updatedAt).toBeTruthy();
  });
});
