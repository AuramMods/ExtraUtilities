#!/usr/bin/env python3
"""Check this project's Patchouli references and report its item and recipe coverage."""

import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
NAMESPACE = "extrautilitiesrebirth"
MAIN = ROOT / "src/main"
RESOURCES = MAIN / "resources"
ASSETS = RESOURCES / "assets" / NAMESPACE
BOOK = ASSETS / "patchouli_books/field_guide/en_us"
DATA = RESOURCES / "data" / NAMESPACE
REGISTRIES = MAIN / "java/inzhefop/extrautilitiesrebirth/init"
ITEM_PREFIX = NAMESPACE + ":"
LINK = re.compile(r"\$\(l:([^)]*)\)")
RECIPE_PAGE_TYPES = {
    "patchouli:crafting": {"minecraft:crafting_shaped", "minecraft:crafting_shapeless",
                          "patchouli:shapeless_book_recipe"},
    "patchouli:smelting": {"minecraft:smelting"},
}
MECHANIC_ENTRIES = (
    ("Generator burn rates, remainders, and tag fuels", "power/first_power", "power/frosty_generator_1",
     "power/pink_generator", "power/death_generator"),
    ("Panel output, wireless links, and Rainbow requirements", "power/sky_panels", "power/wireless",
     "power/rainbow_generator"),
    ("Machine recipes and automation", "machines/resonator", "machines/enchanter", "machines/furnace",
     "machines/mechanical_miner", "machines/mechanical_user", "machines/sorter"),
    ("Item storage, fluid capacities, and disposal", "storage/gold_chest", "storage/golden_bag", "storage/drums",
     "storage/inventory_interface", "storage/trash_cans"),
    ("Quarry operation, upgrades, pump, and WIP assembly", "mining/ender_markers", "mining/ender_quarry",
     "mining/quarry_upgrades", "mining/ender_thermic_pump", "mining/quantum_quarry"),
    ("End ore placement and lily growth", "mining/ender_lilies"),
    ("Redstone ore loot and Demon Metal conversion", "materials/redstone_parts", "materials/demon_metal"),
    ("Boss drops and sigil crafting wear", "materials/unstable_metals"),
    ("Light, collision, and enchanting blocks", "building/selective_glass", "building/utility_glass",
     "building/timber", "materials/ender_parts"),
    ("Mob capture, wing effects, and optional flight", "equipment/golden_lasso", "equipment/wing_rings",
     "equipment/legacy_rings"),
    ("Mob spawning, spike loot, and light controls", "utilities/cursed_earth", "utilities/spikes",
     "utilities/chandelier"),
    ("Return travel, redstone pulses, and chunk tickets", "utilities/return_travel", "utilities/redstone_clock",
     "utilities/chunk_loading"),
    ("Creative and legacy content", "storage/creative_chest", "storage/drums", "power/creative_energy",
     "utilities/creative_blocks", "materials/opinium_core"),
)


def namespaced(name):
    return name if ":" in name else ITEM_PREFIX + name


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def json_tree(directory):
    return {namespaced(str(path.relative_to(directory).with_suffix(""))): read_json(path)
            for path in sorted(directory.rglob("*.json"))}


def registered_content():
    blocks_source = (REGISTRIES / "ExtrautilitiesrebirthModBlocks.java").read_text()
    blocks = dict(re.findall(r'RegistryObject<Block>\s+(\w+)\s*=\s*REGISTRY\.register\("([^"]+)"',
                             blocks_source))
    items_source = (REGISTRIES / "ExtrautilitiesrebirthModItems.java").read_text()
    items = set(re.findall(r'\bREGISTRY\.register\("([^"]+)"', items_source))
    items.update(blocks[name] for name in re.findall(
        r'=\s*block\(\s*ExtrautilitiesrebirthModBlocks\.(\w+)\s*\)', items_source))
    return {namespaced(block) for block in blocks.values()}, {namespaced(item) for item in items}


def write_checklist(path, items, recipes, entries, item_pages, recipe_pages, lang):
    def locations(targets):
        return ", ".join(f"{entries[entry]['name']} (p. {page + 1})" for entry, page in targets)

    lines = [
        "# Extra Utilities Guide coverage",
        "",
        "Run `python3 scripts/check_field_guide.py --report docs/FIELD_GUIDE_COVERAGE.md`",
        "to validate references and regenerate the checklist from the registries,",
        "live recipe pages, and Patchouli item lookup mappings.",
        "",
        "Block items use the same row as the placeable block. Machine recipes,",
        "fuel rules, drops, world generation, and rituals are described in their",
        "respective entries. Those acquisition paths are separate from the",
        "datapack recipe IDs listed below.",
        "",
        "## Gameplay and acquisition",
        "",
        "Use these entries when checking the guide against machine definitions,",
        "fuel tags, loot modifiers, world generation, and player interactions.",
        "",
        "| Mechanic | Guide entries |",
        "| --- | --- |",
    ]
    for mechanic, *references in MECHANIC_ENTRIES:
        titles = [entries.get(namespaced(entry), {}).get("name", f"Open ({entry})") for entry in references]
        lines.append(f"| {mechanic} | {', '.join(titles)} |")
    lines.extend([
        "",
        f"## Registered content ({len(item_pages)}/{len(items)})",
        "",
        "| Status | Item or block | Registry ID | Guide |",
        "| --- | --- | --- | --- |",
    ])
    for item in sorted(items):
        key = item.split(":")[1]
        name = lang.get(f"block.{NAMESPACE}.{key}", lang.get(f"item.{NAMESPACE}.{key}", key))
        covered = item_pages.get(item, ())
        status = "✓" if covered else "Open"
        lines.append(f"| {status} | {name} | `{key}` | {locations(covered)} |")
    lines.extend([
        "",
        f"## Datapack recipes ({len(recipe_pages)}/{len(recipes)})",
        "",
        "| Status | Recipe ID | Guide |",
        "| --- | --- | --- |",
    ])
    for recipe in sorted(recipes):
        covered = recipe_pages.get(recipe, ())
        status = "✓" if covered else "Open"
        lines.append(f"| {status} | `{recipe.split(':')[1]}` | {locations(covered)} |")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--allow-incomplete", action="store_true", help="Check references while drafting chapters")
    parser.add_argument("--report", type=Path, help="Write the full Markdown coverage checklist")
    args = parser.parse_args()

    blocks, items = registered_content()
    recipes = json_tree(DATA / "recipes")
    categories = json_tree(BOOK / "categories")
    entries = json_tree(BOOK / "entries")
    lang = read_json(ASSETS / "lang/en_us.json")
    book = read_json(DATA / "patchouli_books/field_guide/book.json")
    errors = []
    item_pages = defaultdict(list)
    recipe_pages = defaultdict(list)
    used_categories = Counter()

    def require(test, context, detail):
        if not test:
            errors.append(f"{context}: {detail}")

    def check_link(target, source):
        entry, _, anchor = target.partition("#")
        entry = namespaced(entry)
        require(entry in entries, source, f"unknown entry {target}")
        if entry in entries and anchor:
            require(any(page.get("anchor") == anchor for page in entries[entry]["pages"]),
                    source, f"unknown anchor {target}")

    def check_text(value, source):
        for target in LINK.findall(value):
            check_link(target, source)

    def check_asset(resource, source):
        namespace, path = namespaced(resource).split(":", 1)
        if namespace == NAMESPACE:
            require((ASSETS / path).is_file(), source, f"missing asset {resource}")

    def check_stack(stack, source):
        for part in stack.split(","):
            key = part.split("{", 1)[0].split("#", 1)[0].strip()
            if key.startswith(ITEM_PREFIX):
                require(key in items, source, f"unknown item {key}")
            if key.endswith(".png"):
                check_asset(key, source)

    for key in ("book_texture", "crafting_texture", "filler_texture"):
        check_asset(book[key], "book.json")
    namespace, model = namespaced(book["model"]).split(":", 1)
    if namespace == NAMESPACE:
        model_path = ASSETS / f"models/item/{model}.json"
        require(model_path.is_file(), "book.json", f"missing book model {book['model']}")
        if model_path.is_file():
            for texture in read_json(model_path)["textures"].values():
                texture_namespace, texture_path = namespaced(texture).split(":", 1)
                check_asset(f"{texture_namespace}:textures/{texture_path}.png", "book item model")
    for key in ("name", "landing_text"):
        require(book[key] in lang, "book.json", f"missing translation {book[key]}")
        check_text(lang.get(book[key], ""), "book.json")

    for category, content in categories.items():
        check_stack(content["icon"], category)
        check_text(content["description"], category)

    for entry, content in entries.items():
        category = namespaced(content["category"])
        require(category in categories, entry, f"unknown category {category}")
        used_categories[category] += 1
        check_stack(content["icon"], entry)
        pages = content["pages"]
        require(bool(pages), entry, "no pages")
        anchors = [page["anchor"] for page in pages if "anchor" in page]
        require(len(anchors) == len(set(anchors)), entry, "duplicate page anchor")

        for number, page in enumerate(pages):
            source = f"{entry} page {number + 1}"
            check_text(page.get("text", ""), source)
            for target in page.get("entries", []):
                check_link(target, source)
            for image in page.get("images", []):
                check_asset(image, source)
            if isinstance(page.get("item"), str):
                check_stack(page["item"], source)
            if "multiblock" in page:
                multiblock = page["multiblock"]
                mapping = multiblock["mapping"]
                for predicate in mapping.values():
                    block = predicate.split("[", 1)[0]
                    if block.startswith(ITEM_PREFIX):
                        require(block in blocks, source, f"unknown multiblock block {block}")
                layers = multiblock["pattern"]
                rows = [row for layer in layers for row in layer]
                require(bool(rows) and len({len(row) for row in rows}) == 1
                        and len({len(layer) for layer in layers}) == 1, source, "uneven multiblock dimensions")
                cells = Counter("".join(rows))
                require(cells["0"] == 1, source, "multiblock needs one origin")
                require(not cells.keys() - mapping.keys() - {"0", " ", "_"}, source, "unmapped multiblock cell")

            page_type = page["type"]
            if page_type in RECIPE_PAGE_TYPES:
                require("recipe" in page, source, "recipe page has no recipe")
            for field in ("recipe", "recipe2"):
                if field not in page:
                    continue
                recipe_id = namespaced(page[field])
                require(recipe_id in recipes, source, f"unknown recipe {recipe_id}")
                if recipe_id in recipes:
                    recipe = recipes[recipe_id]
                    if recipe["type"] == "forge:conditional":
                        recipe = recipe["recipes"][0]["recipe"]
                    require(recipe["type"] in RECIPE_PAGE_TYPES.get(page_type, set()),
                            source, f"{recipe_id} has incompatible type {recipe['type']}")
                    recipe_pages[recipe_id].append((entry, number))

        for item, page in content.get("extra_recipe_mappings", {}).items():
            valid_item = item in blocks | items
            valid_page = isinstance(page, int) and 0 <= page < len(pages)
            require(valid_item, entry, f"lookup references unknown item {item}")
            require(valid_page, entry, f"invalid page for {item}")
            if valid_item and valid_page:
                item_pages[item].append((entry, page))

    for category in categories.keys() - used_categories.keys():
        errors.append(f"{category}: empty category")
    for item, locations in item_pages.items():
        require(len(locations) == 1, item, "multiple canonical lookup pages")

    missing_items = (blocks | items) - item_pages.keys()
    missing_recipes = recipes.keys() - recipe_pages.keys()
    if args.report:
        write_checklist(args.report, blocks | items, recipes, entries, item_pages, recipe_pages, lang)
    if not args.allow_incomplete:
        for mechanic, *references in MECHANIC_ENTRIES:
            for reference in references:
                check_link(reference, mechanic)
        errors.extend(f"Uncovered item or block: {item}" for item in sorted(missing_items))
        errors.extend(f"Uncovered recipe: {recipe}" for recipe in sorted(missing_recipes))
    for error in errors:
        print(error)
    print(f"{len(categories)} chapters, {len(entries)} entries, "
          f"{len(item_pages)}/{len(blocks | items)} item and block lookups, "
          f"{len(recipe_pages)}/{len(recipes)} live recipe references")
    return bool(errors)


if __name__ == "__main__":
    raise SystemExit(main())
