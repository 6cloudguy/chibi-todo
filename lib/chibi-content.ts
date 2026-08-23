import type { ChibiMood } from "@/lib/models";

export const CHIBI_MOODS: ChibiMood[] = ["idle", "happy", "love", "sleepy", "excited", "shy", "sad"];

const messagesByMood: Record<ChibiMood, string[]> = {
  idle: ["hehe hi", "one task at a time", "I am right here"],
  happy: ["you got this!", "proud of you", "look at you go"],
  love: ["miss you ♡", "sending you a tiny hug", "you make my day softer"],
  sleepy: ["remember to take a break", "a little rest counts too", "slow is still progress"],
  excited: ["let's do this!", "tiny win incoming", "today feels promising"],
  shy: ["I saved you a smile", "quietly cheering for you", "you are doing enough"],
  sad: ["be gentle with yourself", "I am still proud of you", "tomorrow is a fresh page"],
};

export function messageForMood(mood: ChibiMood, customMessages: string[] = [], lastMessage?: string) {
  const collection = [...customMessages, ...messagesByMood[mood]];
  const options = collection.filter((message) => message !== lastMessage);
  const safeOptions = options.length > 0 ? options : collection;
  return safeOptions[Math.floor(Math.random() * safeOptions.length)] ?? "you got this!";
}

export function nextMood(currentMood: ChibiMood): ChibiMood {
  const index = CHIBI_MOODS.indexOf(currentMood);
  return CHIBI_MOODS[(index + 1) % CHIBI_MOODS.length];
}
