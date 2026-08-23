import { Image, StyleSheet, View } from "react-native";

import type { ChibiMood } from "@/lib/models";

type ChibiPortraitProps = {
  mood: ChibiMood;
  size?: number;
};

const moodAsset = {
  idle: require("@/assets/chibi/idle.png"),
  happy: require("@/assets/chibi/happy.png"),
  love: require("@/assets/chibi/love.png"),
  sleepy: require("@/assets/chibi/sleepy.png"),
  excited: require("@/assets/chibi/excited.png"),
  shy: require("@/assets/chibi/shy.png"),
  sad: require("@/assets/chibi/sad.png"),
} satisfies Record<ChibiMood, number>;

export function ChibiPortrait({ mood, size = 230 }: ChibiPortraitProps) {
  return (
    <View style={[styles.halo, { width: size, height: size, borderRadius: size / 2 }]}>
      <Image source={moodAsset[mood]} resizeMode="contain" style={{ width: size * 0.92, height: size * 0.92 }} accessibilityLabel={`${mood} chibi companion`} />
    </View>
  );
}

const styles = StyleSheet.create({
  halo: { backgroundColor: "#FFF0F2", alignItems: "center", justifyContent: "center", overflow: "hidden" },
});
