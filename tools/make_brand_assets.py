#!/usr/bin/env python3
"""Generates the Mehran Movie launcher icon and Android TV banner.

The mascot geometry mirrors drawMascot() in MehranMovieIntro.kt, so the animated
intro, the launcher icon and the TV banner all show the same cute clapperboard.

Usage:  python3 tools/make_brand_assets.py
"""
import os
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")

CREAM = (248, 244, 237, 255)
INK = (20, 21, 28, 255)
BLUSH = (240, 69, 74, 90)
BRAND = (240, 69, 74)
BRAND_LIGHT = (247, 106, 110)
BRAND_DARK = (182, 22, 31)
TEXT1 = (245, 242, 236)
TEXT2 = (185, 181, 173)
BG0 = (8, 8, 12)
BG1 = (23, 16, 23)

BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
ARABIC = "/usr/share/fonts/truetype/wine/tahoma.ttf"


def vertical_gradient(size, top, bottom):
    w, h = size
    grad = Image.new("RGB", (1, h))
    d = ImageDraw.Draw(grad)
    for y in range(h):
        t = y / max(1, h - 1)
        d.point((0, y), fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return grad.resize((w, h))


def radial_glow(size, color, max_alpha):
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(glow)
    steps = 120
    for i in range(steps, 0, -1):
        r = size / 2 * i / steps
        a = int(max_alpha * (1 - i / steps) ** 1.7)
        d.ellipse([size / 2 - r, size / 2 - r, size / 2 + r, size / 2 + r], fill=(*color, a))
    return glow.filter(ImageFilter.GaussianBlur(size * 0.02))


def mascot(size, clap_deg=0.0):
    """The clapperboard character on a transparent square of `size` px."""
    s = float(size)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Clapper bar: 0.17..0.83 wide, 0.225..0.37 tall, hinged at its top-left.
    bar = [s * 0.17, s * 0.225, s * 0.83, s * 0.37]
    bar_radius = (bar[3] - bar[1]) * 0.34

    if clap_deg:
        bar_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        ImageDraw.Draw(bar_layer).rounded_rectangle(bar, radius=bar_radius, fill=CREAM)
        pivot = (bar[0] + bar_radius, bar[3])
        img.alpha_composite(
            bar_layer.rotate(clap_deg, resample=Image.BICUBIC, center=(pivot[0], pivot[1]))
        )
    else:
        d.rounded_rectangle(bar, radius=bar_radius, fill=CREAM)

    # Board: 0.17..0.83 wide, 0.375..0.815 tall.
    d.rounded_rectangle(
        [s * 0.17, s * 0.375, s * 0.83, s * 0.815], radius=s * 0.085, fill=CREAM
    )

    # Slanted slate stripes, clipped to the bar's rounded shape.
    stripes = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sd = ImageDraw.Draw(stripes)
    bar_h = bar[3] - bar[1]
    thickness = bar_h * 0.28
    x = bar[0] + bar_h * 0.30
    while x < bar[2]:
        sd.polygon(
            [
                (x, bar[3]),
                (x + bar_h, bar[1]),
                (x + bar_h + thickness, bar[1]),
                (x + thickness, bar[3]),
            ],
            fill=INK,
        )
        x += bar_h * 0.92
    bar_mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(bar_mask).rounded_rectangle(bar, radius=bar_radius, fill=255)
    stripes.putalpha(ImageChops.multiply(stripes.getchannel("A"), bar_mask))
    img.alpha_composite(stripes)

    # Blush, eyes (with highlights) and smile.
    for fx in (0.30, 0.70):
        r = s * 0.036
        d.ellipse([s * fx - r, s * 0.700 - r, s * fx + r, s * 0.700 + r], fill=BLUSH)

    eye_r = s * 0.058
    for fx in (0.375, 0.625):
        cx, cy = s * fx, s * 0.535
        d.ellipse([cx - eye_r, cy - eye_r, cx + eye_r, cy + eye_r], fill=INK)
        hr = eye_r * 0.32
        hx, hy = cx - eye_r * 0.30, cy - eye_r * 0.34
        d.ellipse([hx - hr, hy - hr, hx + hr, hy + hr], fill=(255, 255, 255, 255))

    smile_r = s * 0.10
    d.arc(
        [s * 0.5 - smile_r, s * 0.590 - smile_r, s * 0.5 + smile_r, s * 0.590 + smile_r],
        start=25,
        end=155,
        fill=INK,
        width=int(max(1, s * 0.030)),
    )
    return img


def badge(size):
    """The rounded red badge with the mascot inside it."""
    img = vertical_gradient((size, size), BRAND_LIGHT, BRAND_DARK).convert("RGBA")
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=size * 0.24, fill=255)
    img.putalpha(mask)
    img.alpha_composite(mascot(size))
    return img


def tracked(draw, xy, text, font, fill, tracking=0):
    """Draws text letter by letter so it can carry tracking."""
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=font, fill=fill)
        x += draw.textlength(ch, font=font) + tracking
    return x - tracking


def icon(path, size, supersample=4):
    img = badge(size * supersample).resize((size, size), Image.LANCZOS)
    img.save(path)
    print("wrote %s (%dx%d)" % (path, size, size))


def banner(path, width, height, supersample=4):
    k = width / 320.0
    scale = k * supersample
    W, H = width * supersample, height * supersample
    canvas = vertical_gradient((W, H), BG0, BG1).convert("RGBA")

    badge_px = int(96 * scale)
    badge_x = int(22 * scale)
    badge_y = int(round((H - badge_px) / 2))
    badge_img = badge(badge_px)
    glow = radial_glow(int(badge_px * 3.1), BRAND, 90)
    canvas.alpha_composite(
        glow,
        (badge_x + badge_px // 2 - glow.width // 2, badge_y + badge_px // 2 - glow.height // 2),
    )
    canvas.alpha_composite(badge_img, (badge_x, badge_y))

    d = ImageDraw.Draw(canvas)
    name_font = ImageFont.truetype(BOLD, int(30 * scale))
    sub_font = ImageFont.truetype(BOLD, int(19 * scale))
    fa_font = ImageFont.truetype(ARABIC, int(15 * scale))

    name, sub, fa = "MEHRAN", "MOVIE", "مهران مووی"
    name_track = int(2 * scale)
    sub_track = int(7 * scale)

    gap1 = int(8 * scale)
    gap2 = int(10 * scale)
    h_name = d.textbbox((0, 0), "MEHRAN", font=name_font)[3]
    h_sub = d.textbbox((0, 0), "MOVIE", font=sub_font)[3]
    h_fa = d.textbbox((0, 0), fa, font=fa_font)[3]
    block_h = h_name + gap1 + h_sub + gap2 + h_fa

    x = int(132 * scale)
    y = int(round((H - block_h) / 2))
    tracked(d, (x, y), name, name_font, TEXT1, name_track)
    tracked(d, (x, y + h_name + gap1), sub, sub_font, BRAND_LIGHT, sub_track)
    d.text((x, y + h_name + gap1 + h_sub + gap2), fa, font=fa_font, fill=TEXT2)

    canvas.resize((width, height), Image.LANCZOS).convert("RGB").save(path)
    print("wrote %s (%dx%d)" % (path, width, height))


def main():
    for folder, size in (
        ("drawable-mdpi", 108),
        ("drawable-hdpi", 162),
        ("drawable-xhdpi", 216),
        ("drawable-xxhdpi", 324),
    ):
        out = os.path.join(RES, folder)
        os.makedirs(out, exist_ok=True)
        icon(os.path.join(out, "ic_launcher.png"), size)

    for folder, w, h in (("drawable-mdpi", 320, 180), ("drawable-xhdpi", 640, 360)):
        out = os.path.join(RES, folder)
        os.makedirs(out, exist_ok=True)
        banner(os.path.join(out, "tv_banner.png"), w, h)


if __name__ == "__main__":
    main()
