"""Fail a build if any offline content is missing from source or the APK."""
import argparse
import json
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
expected = set(phrases) | {"music", "sound_cow", "sound_dog", "sound_cat", "sound_bird", "sound_duck"}
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
        assert "classes.dex" in paths
        assert not any(p.endswith((".html", ".js", ".onnx")) for p in paths)
        assert all(apk.getinfo("assets/audio/" + key + ".wav").compress_type == zipfile.ZIP_STORED for key in expected)
print(f"Verified {len(phrases)} Spanish recordings, 5 animal effects and original music; native app, no permissions.")
