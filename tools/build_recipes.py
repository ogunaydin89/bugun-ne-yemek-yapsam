#!/usr/bin/env python3
"""Check the recipe text files in data/recipes/ and build the app tables from them.

Reads   data/recipes/*.txt, data/pantry.tsv, data/dishes.tsv
Writes  data/recipe_ingredients.tsv, data/recipe_steps.tsv
Updates data/dishes.tsv: ana_malzemeler, sure_dk, kcal_porsiyon

Run from the project root:  python3 tools/build_recipes.py
Nothing is written if any recipe has an error.
"""
import csv
import glob
import os
import re
import sys
from fractions import Fraction

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "data")

# Kitchen measures in ml (see README, "Recipe format")
VOLUME_ML = {"çay kaşığı": 5, "tatlı kaşığı": 10, "yemek kaşığı": 15, "çay bardağı": 100, "su bardağı": 200}
# Units that must be whole numbers, except for the items listed in HALF_OK
COUNT_UNITS = {"adet", "diş", "sap", "dilim", "kare", "kutu", "tablet", "küp", "paket", "demet"}
HALF_OK = {"Limon", "Roka", "Maydanoz", "Dereotu", "Taze nane", "Kalıp margarin", "Spagetti", "Penne / burgu"}
# Not counted in ana_malzemeler (see README, dishes.tsv columns)
NOT_MAIN = {"Tuz", "Karabiber", "Ayçiçek yağı", "Su"}
WATER = "Su"  # assumed at home, not in the pantry; 0 kcal
# Share of deep-frying oil that stays in the food (lines marked "| kızartma")
FRYING_ABSORBED = Fraction(3, 10)


def read_tsv(path):
    with open(path, encoding="utf-8") as f:
        return list(csv.DictReader(f, delimiter="\t"))


def write_tsv(path, cols, rows):
    with open(path, "w", encoding="utf-8") as f:
        f.write("\t".join(cols) + "\n")
        for r in rows:
            f.write("\t".join(str(r.get(c, "")) for c in cols) + "\n")


def num(s):
    """'1', '1,5', '1/2' -> Fraction"""
    return Fraction(s.replace(",", "."))


def parse(path):
    recipes, cur, section = [], None, None
    for n, raw in enumerate(open(path, encoding="utf-8"), 1):
        line = raw.rstrip()
        where = f"{os.path.basename(path)}:{n}"
        if not line:
            continue
        if line.startswith("# "):
            m = re.match(r"# (\d+) (.+)$", line)
            cur = {"id": m.group(1), "ad": m.group(2), "sure": None, "ing": [], "steps": [], "where": where}
            recipes.append(cur)
            section = None
        elif line.startswith("süre:"):
            cur["sure"] = int(line.split(":", 1)[1])
        elif line == "malzemeler:":
            section = "ing"
        elif line == "yapılışı:":
            section = "steps"
        elif section == "ing" and line.startswith("- "):
            parts = [x.strip() for x in line[2:].split("|")]
            if len(parts) == 3 and parts[2] != "kızartma":
                raise SystemExit(f"{where}: unknown mark '{parts[2]}' (only 'kızartma')")
            cur["ing"].append((parts[0], parts[1], len(parts) == 3, where))
        elif section == "steps" and re.match(r"\d+\. ", line):
            cur["steps"].append(line.split(". ", 1)[1])
        else:
            raise SystemExit(f"{where}: cannot read line: {line}")
    return recipes


def grams(item, qty, unit):
    """Return grams for one ingredient line, or raise ValueError."""
    if unit == "g":
        return qty
    if item is None:  # water
        if unit == "ml":
            return qty
        if unit in VOLUME_ML:
            return qty * VOLUME_ML[unit]
        raise ValueError(f"su cannot be measured in '{unit}'")
    sb, yk, ck = (item[k] and Fraction(item[k]) for k in ("g_su_bardagi", "g_yemek_kasigi", "g_cay_kasigi"))
    if unit in VOLUME_ML:
        ml = VOLUME_ML[unit]
        if ml >= 100 and sb:
            return qty * sb * ml / 200
        if ml < 100 and yk:
            return qty * yk * ml / 15
        if ml < 100 and ck:
            return qty * ck * ml / 5
        if sb:
            return qty * sb * ml / 200
        raise ValueError(f"no conversion for '{unit}'")
    if unit == "ml":
        return qty * (sb / 200 if sb else 1)
    if unit == item["birim"]:
        return qty * Fraction(item["g_birim"])
    raise ValueError(f"unit '{unit}' does not match the pantry unit '{item['birim'] or '-'}'")


def main():
    pantry = {r["malzeme"]: r for r in read_tsv(os.path.join(DATA, "pantry.tsv"))}
    dishes = read_tsv(os.path.join(DATA, "dishes.tsv"))
    by_id = {d["id"]: d for d in dishes}

    errors, warnings, ing_rows, step_rows, done = [], [], [], [], {}
    for path in sorted(glob.glob(os.path.join(DATA, "recipes", "*.txt"))):
        for rc in parse(path):
            d = by_id.get(rc["id"])
            if d is None or d["ad"] != rc["ad"]:
                errors.append(f"{rc['where']}: '{rc['id']} {rc['ad']}' does not match dishes.tsv")
                continue
            if rc["id"] in done:
                errors.append(f"{rc['where']}: dish {rc['id']} is written twice")
            if not rc["sure"]:
                errors.append(f"{rc['where']}: missing 'süre:'")
            if not 3 <= len(rc["steps"]) <= 8:
                warnings.append(f"{rc['where']}: {len(rc['steps'])} steps (aim for 5-8)")
            total_kcal, main_ids = Fraction(0), []
            for i, (name, amount, frying, where) in enumerate(rc["ing"], 1):
                item = None if name == WATER else pantry.get(name)
                if name != WATER and item is None:
                    errors.append(f"{where}: '{name}' is not in pantry.tsv")
                    continue
                m = re.match(r"([\d/,]+) (.+)$", amount)
                if not m:
                    errors.append(f"{where}: cannot read amount '{amount}'")
                    continue
                qty, unit = num(m.group(1)), m.group(2)
                if unit in COUNT_UNITS and qty.denominator != 1 and name not in HALF_OK:
                    errors.append(f"{where}: '{name}' must be a whole number of {unit}")
                try:
                    g = grams(item, qty, unit)
                except ValueError as e:
                    errors.append(f"{where}: {name}: {e}")
                    continue
                if item is not None and item["kcal_100g"]:
                    eaten = g * Fraction(item["yenen_oran"] or 1)
                    if frying:
                        eaten *= FRYING_ABSORBED
                    total_kcal += eaten * Fraction(item["kcal_100g"]) / 100
                if name not in NOT_MAIN and item["id"] not in main_ids:
                    main_ids.append(item["id"])
                ing_rows.append({"dish_id": rc["id"], "sira": i, "pantry_id": item["id"] if item else "",
                                 "malzeme": name, "miktar": m.group(1), "olcu": unit, "gram": round(g),
                                 "kizartma": "evet" if frying else ""})
            for i, s in enumerate(rc["steps"], 1):
                step_rows.append({"dish_id": rc["id"], "sira": i, "metin": s})
            kcal = round(total_kcal / int(d["taban_kisi"]) / 10) * 10
            flags = [f for f in d["uyari"].split(",") if f and f != "bal"]
            if pantry["Bal"]["id"] in main_ids:
                flags.insert(0, "bal")
            done[rc["id"]] = {"ana_malzemeler": ",".join(main_ids), "sure_dk": rc["sure"], "kcal_porsiyon": kcal,
                              "uyari": ",".join(flags)}
            band = "kısa" if rc["sure"] <= 30 else "orta" if rc["sure"] <= 60 else "uzun"
            if band != d["sure"] and d["sure"] != "uzun":
                warnings.append(f"{rc['where']}: süre {rc['sure']} dk is '{band}', dishes.tsv says '{d['sure']}'")

    for w in warnings:
        print("warning:", w)
    if errors:
        for e in errors:
            print("ERROR:", e)
        sys.exit(f"{len(errors)} error(s); nothing written.")

    cols = list(dishes[0].keys())
    for c in ("sure_dk", "kcal_porsiyon"):
        if c not in cols:
            cols.insert(cols.index("aciklama"), c)
    for d in dishes:
        d.update(done.get(d["id"], {}))
    write_tsv(os.path.join(DATA, "dishes.tsv"), cols, dishes)
    write_tsv(os.path.join(DATA, "recipe_ingredients.tsv"),
              ["dish_id", "sira", "pantry_id", "malzeme", "miktar", "olcu", "gram", "kizartma"], ing_rows)
    write_tsv(os.path.join(DATA, "recipe_steps.tsv"), ["dish_id", "sira", "metin"], step_rows)
    print(f"{len(done)} recipes built, {len(dishes) - len(done)} dishes still without a recipe.")
    for rid, v in done.items():
        print(f"  {rid:>3} {by_id[rid]['ad']}: {v['sure_dk']} dk, ~{v['kcal_porsiyon']} kcal/porsiyon")


if __name__ == "__main__":
    main()
