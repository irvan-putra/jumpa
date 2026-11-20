#!/usr/bin/env python3
"""jumpa launcher icon: two people meeting, because jumpa means "meet".

Same shared rules as the other vanipy apps: numbers-only version strip across
the bottom 24%, picture inside the round-mask safe circle, deterministic
output (pinned Pillow + pinned DejaVu font vendored in tools/fonts/, fail
instead of substituting).
    pip install pillow==12.3.0
"""
import os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

PILLOW_PIN = "12.3.0"
import PIL

if PIL.__version__ != PILLOW_PIN:
    sys.exit("Pillow %s required, found %s" % (PILLOW_PIN, PIL.__version__))

import importlib.util

HELPERS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "badge_common.py")
spec = importlib.util.spec_from_file_location("badge_common", HELPERS)
common = importlib.util.module_from_spec(spec)
spec.loader.exec_module(common)

S = common.S


def person(d, cx, cy, scale, fill):
    """Head circle above a shoulder half-disc, centered on (cx, cy)."""
    head_r = 0.055 * scale
    head_cy = cy - 0.045 * scale
    d.ellipse([(cx - head_r) * S, (head_cy - head_r) * S,
               (cx + head_r) * S, (head_cy + head_r) * S], fill=fill)
    body_w = 0.19 * scale
    body_h = 0.11 * scale
    d.pieslice([(cx - body_w / 2) * S, (cy - 0.005 * scale) * S,
                (cx + body_w / 2) * S, (cy - 0.005 * scale + body_h) * S],
               start=180, end=360, fill=fill)


def meet(version):
    BG = (14, 92, 75, 255)        # jumpa green
    STRIP = (9, 59, 48, 255)      # darker green
    WHITE = (250, 250, 247, 255)
    AMBER = (255, 179, 0, 255)
    art, d = common.new_layer()
    # two people leaning toward each other above the strip
    person(d, 0.40, 0.44, 1.15, WHITE)
    person(d, 0.60, 0.44, 1.15, AMBER)
    # a small dot where they meet
    d.ellipse([0.485 * S, 0.335 * S, 0.515 * S, 0.365 * S], fill=WHITE)
    return common.compose(BG, art, version, STRIP, WHITE, rounded=.2)


def main():
    version = common.parse_version(sys.argv)
    root = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    common.write_densities(meet(version), os.path.join(root, "app/src/main/res"))
    print("wrote jumpa icons for", version)


if __name__ == "__main__":
    main()
