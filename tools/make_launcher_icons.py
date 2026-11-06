import os
from PIL import Image, ImageDraw

SHEET = "assets/player/player_assets/playerassets.png"
FRAME = (78, 102, 156, 204)
BACKGROUND = (152, 173, 182, 255)
RES = "app/src/main/res"
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}

frame = Image.open(SHEET).convert("RGBA").crop(FRAME)

def sprite_fit(height):
    scale = height / frame.height
    return frame.resize((max(1, round(frame.width * scale)), round(height)), Image.NEAREST)

def centered(canvas_px, sprite_height, background=None, round_mask=False):
    canvas = Image.new("RGBA", (canvas_px, canvas_px), background or (0, 0, 0, 0))
    sprite = sprite_fit(sprite_height)
    canvas.alpha_composite(sprite, ((canvas_px - sprite.width) // 2, (canvas_px - sprite.height) // 2))
    if round_mask:
        mask = Image.new("L", canvas.size, 0)
        ImageDraw.Draw(mask).ellipse((0, 0, canvas_px - 1, canvas_px - 1), fill=255)
        canvas.putalpha(mask)
    return canvas

for name, factor in DENSITIES.items():
    out = os.path.join(RES, f"mipmap-{name}")
    os.makedirs(out, exist_ok=True)
    fg = round(108 * factor)
    centered(fg, fg * 0.58).save(os.path.join(out, "ic_launcher_foreground.png"))
    legacy = round(48 * factor)
    centered(legacy, legacy * 0.8, BACKGROUND).save(os.path.join(out, "ic_launcher.png"))
    centered(legacy, legacy * 0.7, BACKGROUND, round_mask=True).save(os.path.join(out, "ic_launcher_round.png"))
print("icons written")
