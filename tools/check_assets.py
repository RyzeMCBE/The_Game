"""Fail a build if any offline content is missing from source or the APK."""
import argparse
import json
import hashlib
from pathlib import Path
import wave
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
parser = argparse.ArgumentParser()
parser.add_argument("--apk", type=Path)
args = parser.parse_args()
phrases = json.loads((ASSETS / "narration.json").read_text(encoding="utf-8"))
animal_ids = {"cow", "dog", "cat", "bird", "duck", "pig", "sheep", "goat", "horse", "hen", "lion", "elephant", "monkey", "frog", "owl"}
expected = set(phrases) | {"music", "reward_chime"} | {"sound_"+a for a in animal_ids}
assert "Noah" in phrases["welcome"] and "Noah" in phrases["praise_3"] and "Noah" in phrases["break_time"]
profile = json.loads((ASSETS / "voice_profile.json").read_text())
assert profile["kind"] == "synthetic_child_character" and profile["duration_preserved"]
assert set(profile["clips"]) == set(phrases)
assert all(200 <= p["target_median_hz"] <= 400 for p in profile["clips"].values())
animals = json.loads((ASSETS / "animal_sources.json").read_text())
assert set(animals) == animal_ids
for animal, info in animals.items():
    assert info["kind"] == "real_field_recording" and info["license"] in {"CC0-1.0", "Public-Domain", "CC-BY-2.5", "CC-BY-3.0"}
    if info["license"].startswith("CC-BY"):
        assert info["author"] and info["license_url"] and info["page_url"]
    assert hashlib.sha256((ASSETS / "audio" / ("sound_" + animal + ".wav")).read_bytes()).hexdigest() == info["processed_sha256"]
for n in range(1,11):
    assert all(f"{prefix}_{n}" in phrases for prefix in ["count","give","total"])
catalog = json.loads((ASSETS / "drawings/catalog.json").read_text())
pages = catalog["pages"]
assert catalog["width"]==600 and catalog["height"]==450
assert len(pages)==200 and len({p["id"] for p in pages})==200
assert len({p["name"] for p in pages})==200
assert {c:sum(p["category"]==c for p in pages) for c in {p["category"] for p in pages}} == {"animals":40,"dinosaurs":30,"vehicles":30,"nature":30,"food":30,"objects":40}
assert len({json.dumps([(s["d"],s["region"],s["ink"]) for s in p["layers"]]) for p in pages})==200, "Distinct geometry, not recolored copies"
for page in pages:
    assert sum(layer["region"] for layer in page["layers"])>=1
    assert all(layer["d"].startswith("M ") and layer["width"]>0 for layer in page["layers"])
for key in expected:
    with wave.open(str(ASSETS / "audio" / (key + ".wav"))) as audio:
        assert audio.getnchannels() == 1, key
        assert audio.getsampwidth() == 2, key
        assert audio.getnframes() > 1000, key
manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
assert not manifest.findall("uses-permission"), "No network/storage/microphone permission is needed"
for source in (ROOT / "app/src/main/java").rglob("*.kt"):
    content = source.read_text()
    assert "android.webkit" not in content, source
    assert "android.speech.tts" not in content, source
if args.apk:
    with zipfile.ZipFile(args.apk) as apk:
        paths = set(apk.namelist())
        assert all("assets/audio/" + key + ".wav" in paths for key in expected)
        assert "assets/narration.json" in paths
        assert "assets/voice_profile.json" in paths and "assets/animal_sources.json" in paths
        assert "assets/drawings/catalog.json" in paths
        assert len(json.loads(apk.read("assets/drawings/catalog.json"))["pages"])==200
        assert "classes.dex" in paths
        assert not any(p.endswith((".html", ".js", ".onnx")) for p in paths)
        assert all(apk.getinfo("assets/audio/" + key + ".wav").compress_type == zipfile.ZIP_STORED for key in expected)
print(f"Verified 200 distinct coloring sheets, {len(phrases)} child-character recordings for Noah, 15 licensed real animal recordings, celebration chime and music; native app, no permissions.")
