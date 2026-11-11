"""Deterministic, subject-aware ink flourishes for 200 offline coloring sheets.

Non-region open paths leave the paintable region order, template IDs and legacy
painting data untouched. The original silhouette remains editable by the child.
"""
from __future__ import annotations

INK = "#243D59"
STYLE = "storybook_illustrated_v2"


def stroke(d: str, width: float = 2.5) -> dict:
    return {"d": d, "color": INK, "region": False, "width": width, "ink": False}


def flourishes(page: dict) -> list[dict]:
    seed = sum((i + 1) * ord(c) for i, c in enumerate(page["id"]))
    n = seed % 24
    m = (seed // 7) % 20
    v = seed % 4
    category = page["category"]
    result = [
        # Paper-storybook star, tiny dots, playful curling ribbon. None are closed paths.
        stroke(f"M {47+n} 65 L {71+n} 65 M {59+n} 53 L {59+n} 77", 2.7),
        stroke(f"M {78+n} 93 Q {92+n} 77 {106+n} 96", 2.3),
        stroke(f"M {475-m} 65 Q {499-m} 38 {519-m} 65 Q {541-m} 48 {551-m} 80", 2.4),
        stroke(f"M {486-m} 92 Q {509-m} 103 {528-m} 91", 2.2),
        stroke(f"M {46+n} 351 Q {55+n} 333 {64+n} 348 Q {78+n} 363 {86+n} 338", 2.3),
        stroke(f"M {503-m} 357 Q {520-m} 347 {539-m} 365", 2.3),
    ]
    if category in {"animals", "dinosaurs", "nature"}:
        result.extend([
            stroke(f"M {69+n} 395 Q {63+n} 369 {49+n} 364 M {69+n} 395 Q {75+n} 371 {88+n} 367", 2.9),
            stroke(f"M {513-m} 392 Q {505-m} 371 {495-m} 369 M {513-m} 392 Q {529-m} 372 {542-m} 369", 2.8),
            stroke(f"M {103+n} 406 Q {110+n} 391 {120+n} 398", 2.0),
        ])
    elif category == "vehicles":
        result.extend([
            stroke(f"M {42+n} {202+m} L {96+n} {202+m} M {56+n} {221+m} L {88+n} {221+m}", 2.7),
            stroke(f"M {504-m} {224+m} L {557-m} {224+m} M {519-m} {244+m} L {545-m} {244+m}", 2.6),
            stroke(f"M {110+n} 401 Q {190+n} 387 {241+n} 403", 2.1),
        ])
    elif category == "food":
        result.extend([
            stroke(f"M {89+n} 373 Q {120+n} 390 {158+n} 375", 2.2),
            stroke(f"M {440-m} 375 Q {475-m} 390 {503-m} 374", 2.2),
            stroke(f"M {482-m} 113 Q {468-m} 95 {481-m} 85 Q {492-m} 79 {498-m} 65", 2.4),
        ])
    else:
        result.extend([
            stroke(f"M {77+n} 393 Q {107+n} 369 {135+n} 393", 2.4),
            stroke(f"M {458-m} 390 Q {484-m} 366 {519-m} 388", 2.4),
            stroke(f"M {450-m} 123 Q {470-m} 105 {490-m} 127", 2.2),
        ])
    if v == 0:
        result.append(stroke(f"M {456-m} 307 Q {472-m} 282 {491-m} 308 Q {507-m} 325 {521-m} 306", 1.9))
    elif v == 1:
        result.append(stroke(f"M {91+n} 281 Q {112+n} 266 {128+n} 285 Q {138+n} 296 {154+n} 280", 1.9))
    elif v == 2:
        result.append(stroke(f"M {461-m} 164 L {477-m} 177 M {475-m} 161 L {461-m} 179", 1.8))
    else:
        result.append(stroke(f"M {115+n} 157 Q {124+n} 144 {133+n} 157", 1.8))
    return result


def enrich_pages(pages: list[dict]) -> list[dict]:
    for page in pages:
        # All added strokes are open, non-region art. Never insert a region
        # before a stored region index: older coloring drafts depend on it.
        page["layers"].extend(flourishes(page))
    return pages
