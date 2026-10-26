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
expected = set(phrases) | {"music", "reward_chime", "sound_cow", "sound_dog", "sound_cat", "sound_bird", "sound_duck"}
assert "Noah" in phrases["welcome"] and "Noah" in phrases["praise_3"] and "Noah" in phrases["break_time"]
profile = json.loads((ASSETS / "voice_profile.json").read_text())
assert profile["kind"] == "synthetic_child_character" and profile["duration_preserved"]
assert set(profile["clips"]) == set(phrases)
assert all(200 <= p["target_median_hz"] <= 400 for p in profile["clips"].values())
animals = json.loads((ASSETS / "animal_sources.json").read_text())
assert set(animals) == {"cow", "dog", "cat", "bird", "duck"}
for animal, info in animals.items():
    assert info["kind"] == "real_field_recording" and info["license"] == "CC0-1.0"
    assert hashlib.sha256((ASSETS / "audio" / ("sound_" + animal + ".wav")).read_bytes()).hexdigest() == info["processed_sha256"]
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
        assert "classes.dex" in paths
        assert not any(p.endswith((".html", ".js", ".onnx")) for p in paths)
        assert all(apk.getinfo("assets/audio/" + key + ".wav").compress_type == zipfile.ZIP_STORED for key in expected)
print(f"Verified {len(phrases)} child-character recordings for Noah, 5 real CC0 animal recordings, celebration chime and music; native app, no permissions.")
