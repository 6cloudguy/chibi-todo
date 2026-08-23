from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
CHIBI_DIR = ROOT / "assets" / "chibi"
ICON_PATHS = [
    ROOT / "assets" / "images" / "icon.png",
    ROOT / "assets" / "images" / "splash-icon.png",
    ROOT / "assets" / "images" / "favicon.png",
    ROOT / "assets" / "images" / "android-icon-foreground.png",
]


def compact_png(path: Path, max_dimension: int) -> None:
    with Image.open(path) as image:
        source = image.convert("RGBA")
        source.thumbnail((max_dimension, max_dimension), Image.Resampling.LANCZOS)
        source.save(path, format="PNG", optimize=True, compress_level=9)


def main() -> None:
    for mood in ("idle", "happy", "love", "sleepy", "excited", "shy", "sad"):
        compact_png(CHIBI_DIR / f"{mood}.png", 256)
    for icon in ICON_PATHS:
        compact_png(icon, 384)


if __name__ == "__main__":
    main()
