#!/usr/bin/env python3
"""
Generador de assets de Amatis Upgrade (sprites, armadura, modelos, recetas, tags, traducciones).

    python3 tools/generate_assets.py

Requiere: numpy y Pillow.

IMPORTANTE SOBRE EL ARTE
  Los sprites son dibujos ORIGINALES hechos por script (estilo morado/negro con cristales rosas),
  inspirados en la paleta de la imagen de referencia, pero NO son esa obra. Si tienes permiso del
  artista (o tus propios sprites), basta con reemplazar los PNG de
      src/main/resources/assets/amatis_upgrade/textures/item/
      src/main/resources/assets/amatis_upgrade/textures/entity/equipment/
  conservando el nombre del archivo. No hace falta tocar el codigo.
"""
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / "amatis_upgrade"
DATA = RES / "data"
NS = "amatis_upgrade"


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


# =====================================================================================
#  PALETA
# =====================================================================================
PAL = {
    "K": (20, 14, 28),     # contorno
    "D": (44, 38, 54),     # metal oscuro
    "G": (72, 64, 88),     # gris violeta
    "L": (112, 102, 132),  # gris claro
    "V": (130, 40, 170),   # violeta
    "M": (208, 60, 222),   # magenta
    "R": (244, 128, 248),  # rosa
    "W": (255, 222, 255),  # brillo
}


class Sprite:
    def __init__(self):
        self.px = {}

    def put(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[(x, y)] = PAL[c]

    def ascii(self, rows):
        assert len(rows) == 16, len(rows)
        for y, row in enumerate(rows):
            assert len(row) == 16, (y, row, len(row))
            for x, ch in enumerate(row):
                if ch != ".":
                    self.put(x, y, ch)
        return self

    def outline(self):
        extra = {}
        for (x, y) in self.px:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, y + dy)
                if n not in self.px and 0 <= n[0] < 16 and 0 <= n[1] < 16:
                    extra[n] = PAL["K"]
        self.px.update(extra)
        return self

    def image(self):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for (x, y), c in self.px.items():
            img.putpixel((x, y), c + (255,))
        return img


# ---- geometria diagonal: a = x - y (a lo largo del arma), d = x + y - 15 (ancho) ----
def ad(x, y):
    return x - y, x + y - 15


def shaft(s, a_lo=-13, a_hi=6):
    """Mango: escalera de 1 pixel de ancho, con bandas de agarre."""
    for y in range(16):
        for x in range(16):
            a, d = ad(x, y)
            if a_lo <= a <= a_hi and d in (0, 1):
                s.put(x, y, "G" if ((a - a_lo) // 3) % 2 == 0 else "D")
    # contera
    s.put(0, 15, "V")
    s.put(1, 15, "D")
    s.put(0, 14, "D")


def blade(s, a_lo, a_hi, d_lo, d_hi, flat=False):
    """Hoja con degradado violeta -> magenta -> rosa y filo brillante."""
    width = d_hi - d_lo + 1
    for y in range(16):
        for x in range(16):
            a, d = ad(x, y)
            if not (a_lo <= a <= a_hi and d_lo <= d <= d_hi):
                continue
            if not flat:  # punta: se afila los ultimos pasos
                left = a_hi - a
                if left <= 1 and not (d_lo + width // 2 - 1 <= d <= d_lo + width // 2):
                    continue
                if left == 2 and (d == d_lo or d == d_hi) and width > 3:
                    continue
            t = (d - d_lo) / max(1, d_hi - d_lo)
            col = "V" if t < 0.25 else "M" if t < 0.6 else "R" if t < 0.9 else "W"
            s.put(x, y, col)


def guard(s, a, half, wide=2):
    for y in range(16):
        for x in range(16):
            aa, d = ad(x, y)
            if a <= aa < a + wide and -half <= d <= half + 1:
                s.put(x, y, "L" if abs(d - 0.5) <= 1 else "G")
    # gema central
    for y in range(16):
        for x in range(16):
            aa, d = ad(x, y)
            if aa in (a, a + 1) and d in (0, 1):
                s.put(x, y, "M")


def sparkle(s, pts):
    for (x, y, c) in pts:
        s.put(x, y, c)


def disc(s, cx, cy, r_lo, r_hi, ang_lo, ang_hi, colorfn):
    for y in range(16):
        for x in range(16):
            dx = x + 0.5 - cx
            dy = cy - (y + 0.5)
            r = math.hypot(dx, dy)
            ang = math.degrees(math.atan2(dy, dx))
            if r_lo <= r <= r_hi and ang_lo <= ang <= ang_hi:
                s.put(x, y, colorfn(r, ang))


# =====================================================================================
#  SPRITES DE ITEMS
# =====================================================================================
def sprite_sword():
    s = Sprite()
    shaft(s, -13, -5)
    blade(s, -3, 14, -1, 2)
    guard(s, -5, 3)
    sparkle(s, [(12, 3, "W"), (9, 6, "W")])
    return s.outline().image()


def sprite_dagger():
    s = Sprite()
    shaft(s, -13, -3)
    blade(s, -1, 11, 0, 1)
    guard(s, -3, 2, 1)
    return s.outline().image()


def sprite_heavy():
    s = Sprite()
    shaft(s, -13, -4)
    blade(s, -2, 13, -3, 4, flat=True)   # punta plana
    # nervadura central y runas
    for y in range(16):
        for x in range(16):
            a, d = ad(x, y)
            if -1 <= a <= 11 and d in (0, 1) and a % 3 == 0:
                s.put(x, y, "W")
            elif 0 <= a <= 12 and d == -2 and a % 4 == 1:
                s.put(x, y, "D")
    guard(s, -4, 4)
    return s.outline().image()


def sprite_scythe():
    s = Sprite()
    shaft(s, -13, 7)
    # hoja en media luna: nace en la punta del mango y se curva hacia abajo-izquierda, afinandose
    for y in range(16):
        for x in range(16):
            dx = x + 0.5 - 11.0
            dy = 11.0 - (y + 0.5)
            r = math.hypot(dx, dy)
            ang = math.degrees(math.atan2(dy, dx))
            if 88 <= ang <= 178:
                t = (ang - 88) / 90.0
                r_hi = 8.9
                r_lo = 6.9 + t * 1.9
                if r_lo <= r <= r_hi:
                    s.put(x, y, "R" if r > r_hi - 0.9 else ("M" if r > r_lo + 0.5 else "V"))
    sparkle(s, [(10, 2, "W"), (6, 3, "W")])
    return s.outline().image()


def sprite_pickaxe():
    s = Sprite()
    shaft(s, -13, 3)
    disc(s, 6.0, 9.0, 4.4, 6.6, -30, 120,
         lambda r, ang: "R" if r > 6.0 else ("M" if r > 5.2 else "V"))
    return s.outline().image()


def sprite_axe():
    s = Sprite()
    shaft(s, -13, 5)
    rows = {2: (7, 10), 3: (5, 11), 4: (4, 11), 5: (3, 10), 6: (3, 8), 7: (3, 7), 8: (4, 6)}
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            if x <= x0 + 1:
                c = "R"
            elif x >= x1 - 1:
                c = "V"
            else:
                c = "M"
            s.put(x, y, c)
    s.put(4, 6, "W")
    s.put(4, 5, "W")
    return s.outline().image()


def sprite_shovel():
    s = Sprite()
    shaft(s, -13, 2)
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 11.0, 4.0 - (y + 0.5)
            if (dx / 2.6) ** 2 + (dy / 3.6) ** 2 <= 1.0:
                r = math.hypot(dx, dy)
                s.put(x, y, "R" if r > 2.6 else ("M" if r > 1.4 else "V"))
    return s.outline().image()


def sprite_hoe():
    s = Sprite()
    shaft(s, -13, 5)
    for x in range(5, 12):
        s.put(x, 3, "R" if x < 10 else "M")
    for x in (5, 6, 7, 10, 11):
        s.put(x, 4, "M")
    s.put(5, 5, "V")
    s.put(6, 5, "V")
    s.put(5, 6, "V")
    return s.outline().image()


ART = {
    "template": [
        "................",
        ".KKKKKKKKKKKKKK.",
        ".KDGGGGGGGGGGDK.",
        ".KGDDDDDDDDDDGK.",
        ".KGDDDDMMDDDDGK.",
        ".KGDDDMRRMDDDGK.",
        ".KGDDMRWWRMDDGK.",
        ".KGDDMRWWRMDDGK.",
        ".KGDDDMRRMDDDGK.",
        ".KGDDDDMMDDDDGK.",
        ".KGDDDDDDDDDDGK.",
        ".KGDVDDDDDDVDGK.",
        ".KDGGGGGGGGGGDK.",
        ".KKKKKKKKKKKKKK.",
        "................",
        "................",
    ],
    "discharged": [
        "................",
        ".......KK.......",
        "......KLLK......",
        ".....KLGVLK.....",
        "....KLGVVVLK....",
        "...KLGVVMVVLK...",
        "...KGVVMGMVVK...",
        "...KGVMGGGMVK...",
        "...KVVVMGMVVK...",
        "...KDVVVMVVDK...",
        "....KDVVVVDK....",
        ".....KDVVDK.....",
        "......KDDK......",
        ".......KK.......",
        "................",
        "................",
    ],
    "overcharged": [
        "................",
        "..........KK....",
        ".........KRWK...",
        "........KRWMK...",
        ".......KRWMVK...",
        "......KRWMVK....",
        ".....KRWMVK.....",
        "....KRWMVK......",
        "...KRWMVK.......",
        "..KRWMVK........",
        "..KRMVK.........",
        "...KVVK.....W...",
        "....KK.....WRW..",
        "............W...",
        "................",
        "................",
    ],
    "shield": [
        "................",
        "..KKKKKKKKKKKK..",
        ".KDGGGGGGGGGGDK.",
        ".KGDDDVVVVDDDGK.",
        ".KGDDVMRRMVDDGK.",
        ".KGDVMRWWRMVDGK.",
        ".KGDVMRWWRMVDGK.",
        ".KGDDVMRRMVDDGK.",
        ".KGDDDVMMVDDDGK.",
        ".KGDDDDVVDDDDGK.",
        "..KGDDDVVDDDGK..",
        "...KGDDDDDDGK...",
        "....KGDDDDGK....",
        ".....KGDDGK.....",
        "......KGGK......",
        ".......KK.......",
    ],
    "helmet": [
        "................",
        "..R..........R..",
        "..RM........MR..",
        ".KRMK......KMRK.",
        ".KKKKKKKKKKKKKK.",
        ".KDGGGGGGGGGGDK.",
        ".KDGLLLLLLLLGDK.",
        ".KDGLDDDDDDLGDK.",
        ".KDGLDDDDDDLGDK.",
        ".KDGLDMDDMDLGDK.",
        ".KDGLDDDDDDLGDK.",
        ".KDGK......KGDK.",
        ".KDDK......KDDK.",
        ".KKKK......KKKK.",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "..KKK......KKK..",
        ".KGLLK....KLLGK.",
        ".KDGLKKKKKKLGDK.",
        ".KDGGLLLLLLGGDK.",
        ".KKDGLDDDDLGDKK.",
        "..KKDLDMMDLDKK..",
        "...KDLDMMDLDK...",
        "...KDLDDDDLDK...",
        "...KGLDVVDLGK...",
        "...KGLDDDDLGK...",
        "...KDGLLLLGDK...",
        "...KDDGGGGDDK...",
        "....KKKKKKKK....",
        "................",
        "................",
    ],
    "leggings": [
        "................",
        "..KKKKKKKKKKKK..",
        "..KDGGGGGGGGDK..",
        "..KDGLLLLLLGDK..",
        "..KDGMDDDDMGDK..",
        "..KDGDDKKDDGDK..",
        "..KDGDK..KDGDK..",
        "..KDGDK..KDGDK..",
        "..KDGDK..KDGDK..",
        "..KDGDK..KDGDK..",
        "..KDLDK..KDLDK..",
        "..KDLVK..KVLDK..",
        "..KDDDK..KDDDK..",
        "..KKKKK..KKKKK..",
        "................",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "...KKK....KKK...",
        "..KDGDK..KDGDK..",
        "..KDGDK..KDGDK..",
        "..KDLDK..KDLDK..",
        "..KDLDK..KDLDK..",
        ".KDGLDK..KDLGDK.",
        ".KDLLLDKKDLLLDK.",
        ".KVVVVDKKDVVVVK.",
        ".KKKKKKKKKKKKKK.",
        "................",
    ],
}

ITEM_SPRITES = {
    "amatis_upgrade_template": lambda: Sprite().ascii(ART["template"]).image(),
    "discharged_resonant_amethyst": lambda: Sprite().ascii(ART["discharged"]).image(),
    "overcharged_resonant_fragment": lambda: Sprite().ascii(ART["overcharged"]).image(),
    "amatis_sword": sprite_sword,
    "amatis_dagger": sprite_dagger,
    "amatis_heavy_sword": sprite_heavy,
    "amatis_scythe": sprite_scythe,
    "amatis_pickaxe": sprite_pickaxe,
    "amatis_axe": sprite_axe,
    "amatis_shovel": sprite_shovel,
    "amatis_hoe": sprite_hoe,
    "amatis_shield": lambda: Sprite().ascii(ART["shield"]).image(),
    "amatis_helmet": lambda: Sprite().ascii(ART["helmet"]).image(),
    "amatis_chestplate": lambda: Sprite().ascii(ART["chestplate"]).image(),
    "amatis_leggings": lambda: Sprite().ascii(ART["leggings"]).image(),
    "amatis_boots": lambda: Sprite().ascii(ART["boots"]).image(),
}

# modelos "handheld" (se sostienen en diagonal como una espada) y escala extra para las armas grandes
HANDHELD = {"amatis_sword", "amatis_dagger", "amatis_heavy_sword", "amatis_scythe",
            "amatis_pickaxe", "amatis_axe", "amatis_shovel", "amatis_hoe"}
BIG = {"amatis_heavy_sword": 1.45, "amatis_scythe": 1.35}


def handheld_display(scale):
    k = 0.85 * scale
    f = 0.68 * scale
    return {
        "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [k, k, k]},
        "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [k, k, k]},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [f, f, f]},
        "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [f, f, f]},
    }


def build_items():
    (ASSETS / "textures" / "item").mkdir(parents=True, exist_ok=True)
    for name, fn in ITEM_SPRITES.items():
        fn().save(ASSETS / "textures" / "item" / f"{name}.png")
        if name in HANDHELD:
            model = {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{NS}:item/{name}"}}
            if name in BIG:
                model["display"] = handheld_display(BIG[name])
        else:
            model = {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}}
        write_json(ASSETS / "models" / "item" / f"{name}.json", model)
        write_json(ASSETS / "items" / f"{name}.json",
                   {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})


# =====================================================================================
#  CAPAS DE ARMADURA (64x32)
# =====================================================================================
def armor_layers():
    rng = np.random.default_rng(29)

    def fill(img, x0, y0, w, h, base, accent_bottom=True):
        for yy in range(h):
            for xx in range(w):
                n = int(rng.integers(-6, 7))
                c = tuple(max(0, min(255, v + n)) for v in base)
                if (xx + yy) % 5 == 0:
                    c = tuple(min(255, v + 12) for v in c)
                img.putpixel((x0 + xx, y0 + yy), c + (255,))
        # borde magenta inferior y vetas
        if accent_bottom and h >= 3:
            for xx in range(w):
                img.putpixel((x0 + xx, y0 + h - 1), PAL["V"] + (255,))
        if w >= 6 and h >= 6:
            img.putpixel((x0 + w // 2, y0 + h // 2), PAL["M"] + (255,))
            img.putpixel((x0 + w // 2, y0 + h // 2 + 1), PAL["R"] + (255,))

    dark = (46, 40, 58)
    mid = (70, 62, 88)

    # --- capa 1: casco, peto, mangas y botas ---
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    fill(l1, 8, 0, 16, 8, dark, False)          # cabeza: arriba y abajo
    fill(l1, 0, 8, 32, 8, mid)                  # cabeza: 4 lados
    # cristales rosas en la parte superior del casco
    for (x, y) in ((10, 3), (11, 3), (10, 4), (12, 4), (20, 3), (21, 3), (21, 4), (19, 4)):
        l1.putpixel((x, y), PAL["R"] + (255,))
    for (x, y) in ((10, 4), (21, 3)):
        l1.putpixel((x, y), PAL["W"] + (255,))
    # visor en la cara frontal (x 8..16, y 8..16)
    for xx in range(9, 15):
        l1.putpixel((xx, 12), PAL["M"] + (255,))
    fill(l1, 16, 16, 24, 16, dark)              # cuerpo (los 4 lados)
    fill(l1, 40, 16, 16, 16, mid)               # brazos
    fill(l1, 0, 26, 16, 6, dark)                # botas (parte baja de las piernas)
    for xx in range(0, 16):
        l1.putpixel((xx, 26), PAL["M"] + (255,))
    # pecho: gema
    for (x, y) in ((23, 24), (24, 24), (23, 25), (24, 25)):
        l1.putpixel((x, y), PAL["R"] + (255,))

    # --- capa 2: pantalones ---
    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    fill(l2, 0, 16, 16, 16, dark)               # piernas
    fill(l2, 16, 28, 24, 4, mid, False)         # cinturon
    for xx in range(16, 40):
        l2.putpixel((xx, 28), PAL["M"] + (255,))
    for yy in range(16, 32, 4):
        for xx in range(0, 16):
            if xx % 4 == 0:
                l2.putpixel((xx, yy), PAL["V"] + (255,))

    out1 = ASSETS / "textures" / "entity" / "equipment" / "humanoid"
    out2 = ASSETS / "textures" / "entity" / "equipment" / "humanoid_leggings"
    out1.mkdir(parents=True, exist_ok=True)
    out2.mkdir(parents=True, exist_ok=True)
    l1.save(out1 / "amatis.png")
    l2.save(out2 / "amatis.png")

    write_json(ASSETS / "equipment" / "amatis.json", {
        "layers": {
            "humanoid": [{"texture": f"{NS}:amatis"}],
            "humanoid_leggings": [{"texture": f"{NS}:amatis"}],
        }
    })


# =====================================================================================
#  RECETAS, TAGS, LANG
# =====================================================================================
TEMPLATE = f"{NS}:amatis_upgrade_template"
FRAG = f"{NS}:overcharged_resonant_fragment"
DISCHARGED = f"{NS}:discharged_resonant_amethyst"

UPGRADES = {  # resultado: item base de netherita (o escudo vanilla)
    "amatis_sword": "minecraft:netherite_sword",
    "amatis_axe": "minecraft:netherite_axe",
    "amatis_pickaxe": "minecraft:netherite_pickaxe",
    "amatis_shovel": "minecraft:netherite_shovel",
    "amatis_hoe": "minecraft:netherite_hoe",
    "amatis_helmet": "minecraft:netherite_helmet",
    "amatis_chestplate": "minecraft:netherite_chestplate",
    "amatis_leggings": "minecraft:netherite_leggings",
    "amatis_boots": "minecraft:netherite_boots",
    "amatis_shield": "minecraft:shield",
}


def build_recipes():
    for result, base in UPGRADES.items():
        write_json(DATA / NS / "recipe" / f"{result}_smithing.json", {
            "type": "minecraft:smithing_transform",
            "template": TEMPLATE,
            "base": base,
            "addition": FRAG,
            "result": {"id": f"{NS}:{result}", "count": 1},
        })

    # amatista resonante descargada: 8 fragmentos de amatista rodeando un fragmento resonante (echo shard)
    write_json(DATA / NS / "recipe" / "discharged_resonant_amethyst.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["AAA", "ASA", "AAA"],
        "key": {"A": "minecraft:amethyst_shard", "S": "minecraft:echo_shard"},
        "result": {"id": DISCHARGED, "count": 1},
    })
    # daga: dos columnas paralelas  fragmento / lingote de netherita / palo
    write_json(DATA / NS / "recipe" / "amatis_dagger.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": ["FF", "NN", "SS"],
        "key": {"F": FRAG, "N": "minecraft:netherite_ingot", "S": "minecraft:stick"},
        "result": {"id": f"{NS}:amatis_dagger", "count": 1},
    })
    # guadana: receta de la guadana anterior (MMM / .S. / S..) con 1 lingote de netherita + 2 fragmentos
    write_json(DATA / NS / "recipe" / "amatis_scythe.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": ["FFN", " S ", "S  "],
        "key": {"F": FRAG, "N": "minecraft:netherite_ingot", "S": "minecraft:stick"},
        "result": {"id": f"{NS}:amatis_scythe", "count": 1},
    })


def tag(path, values):
    write_json(path, {"replace": False, "values": values})


def build_tags():
    n = lambda *names: [f"{NS}:{x}" for x in names]
    tools = n("amatis_pickaxe", "amatis_axe", "amatis_shovel", "amatis_hoe")
    swords = n("amatis_sword", "amatis_heavy_sword", "amatis_dagger", "amatis_scythe")
    armor = n("amatis_helmet", "amatis_chestplate", "amatis_leggings", "amatis_boots")
    everything = tools + swords + armor + n("amatis_shield")

    tag(DATA / NS / "tags" / "item" / "repair_materials.json", n("overcharged_resonant_fragment"))

    mc = DATA / "minecraft" / "tags" / "item"
    tag(mc / "swords.json", swords)
    tag(mc / "pickaxes.json", n("amatis_pickaxe"))
    tag(mc / "axes.json", n("amatis_axe"))
    tag(mc / "shovels.json", n("amatis_shovel"))
    tag(mc / "hoes.json", n("amatis_hoe"))
    tag(mc / "head_armor.json", n("amatis_helmet"))
    tag(mc / "chest_armor.json", n("amatis_chestplate"))
    tag(mc / "leg_armor.json", n("amatis_leggings"))
    tag(mc / "foot_armor.json", n("amatis_boots"))

    ench = mc / "enchantable"
    tag(ench / "durability.json", everything)
    tag(ench / "vanishing.json", everything)
    tag(ench / "sword.json", swords)
    tag(ench / "weapon.json", swords + n("amatis_axe"))
    tag(ench / "sharp_weapon.json", swords + n("amatis_axe"))
    tag(ench / "fire_aspect.json", swords)
    tag(ench / "mining.json", tools)
    tag(ench / "mining_loot.json", tools)
    tag(ench / "armor.json", armor)
    tag(ench / "head_armor.json", n("amatis_helmet"))
    tag(ench / "chest_armor.json", n("amatis_chestplate"))
    tag(ench / "leg_armor.json", n("amatis_leggings"))
    tag(ench / "foot_armor.json", n("amatis_boots"))


NAMES = {
    "amatis_upgrade_template": ("Amethyst Upgrade Smithing Template", "Plantilla de mejora de amatista"),
    "discharged_resonant_amethyst": ("Discharged Resonant Amethyst", "Amatista resonante descargada"),
    "overcharged_resonant_fragment": ("Overcharged Resonant Amethyst Fragment",
                                      "Fragmento de amatista resonante sobrecargada"),
    "amatis_sword": ("Amethyst Sword", "Espada de amatista"),
    "amatis_axe": ("Amethyst Axe", "Hacha de amatista"),
    "amatis_pickaxe": ("Amethyst Pickaxe", "Pico de amatista"),
    "amatis_shovel": ("Amethyst Shovel", "Pala de amatista"),
    "amatis_hoe": ("Amethyst Hoe", "Azada de amatista"),
    "amatis_helmet": ("Amethyst Helmet", "Casco de amatista"),
    "amatis_chestplate": ("Amethyst Chestplate", "Peto de amatista"),
    "amatis_leggings": ("Amethyst Leggings", "Pantalones de amatista"),
    "amatis_boots": ("Amethyst Boots", "Botas de amatista"),
    "amatis_heavy_sword": ("Amethyst Heavy Sword", "Espada pesada de amatista"),
    "amatis_dagger": ("Amethyst Dagger", "Daga de amatista"),
    "amatis_scythe": ("Amethyst Scythe", "Guadaña de amatista"),
    "amatis_shield": ("Amethyst Shield", "Escudo de amatista"),
}
TIPS = {
    "template": ("Netherite Upgrade Template + Amethyst Shard (anvil)",
                 "Plantilla de netherita + fragmento de amatista (yunque)"),
    "heavy": ("Every 3rd hit: heavy strike with a small shockwave",
              "Cada 3 golpes: golpe pesado con pequeña onda de choque"),
    "dagger": ("Fast attacks", "Ataques rápidos"),
    "scythe": ("Every 3rd hit: heavy strike with a small shockwave",
               "Cada 3 golpes: golpe pesado con pequeña onda de choque"),
    "shield": ("Hold to raise a 3x3 barrier that blocks all damage",
               "Mantén para alzar una barrera de 3x3 que bloquea todo el daño"),
}


def build_lang():
    en, es = {}, {}
    for key, (e, s) in NAMES.items():
        en[f"item.{NS}.{key}"] = e
        es[f"item.{NS}.{key}"] = s
    for key, (e, s) in TIPS.items():
        en[f"tooltip.{NS}.{key}"] = e
        es[f"tooltip.{NS}.{key}"] = s
    en["creativeTab.amatis_upgrade"] = "Amatis Upgrade"
    es["creativeTab.amatis_upgrade"] = "Amatis Upgrade"
    write_json(ASSETS / "lang" / "en_us.json", en)
    for code in ("es_es", "es_mx", "es_ar", "es_uy"):
        write_json(ASSETS / "lang" / f"{code}.json", es)


def main():
    build_items()
    armor_layers()
    build_recipes()
    build_tags()
    build_lang()
    print("Listo.")


if __name__ == "__main__":
    main()
