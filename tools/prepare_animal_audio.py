"""Prepare licensed actual animal recordings at authoring time, never on Android."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import tempfile
import urllib.request
import urllib.parse
import wave

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
SOURCES = {
    "cow": ("https://bigsoundbank.com/cow-moos-s0546.html", 3.0),
    "dog": ("https://bigsoundbank.com/barking-dog-3-s2955.html", 2.2),
    "cat": ("https://bigsoundbank.com/little-meow-of-a-cat-6-s1476.html", 2.0),
    "bird": ("https://bigsoundbank.com/robin-1-s1667.html", 3.0),
    "duck": ("https://bigsoundbank.com/ducks-s0276.html", 3.0),
    "sheep": ("https://bigsoundbank.com/sheep-1-s2343.html", 2.0),
    "goat": ("https://bigsoundbank.com/bleating-goat-1-s0279.html", 2.0),
    "horse": ("https://bigsoundbank.com/horse-neighing-3-s0863.html", 3.0),
    "hen": ("https://bigsoundbank.com/hens-lays-s0978.html", 3.0),
    "frog": ("https://bigsoundbank.com/one-frog-s0819.html", 3.0),
    "owl": ("https://bigsoundbank.com/long-eared-owl-1-s0936.html", 3.0),
}
COMMONS = {
    "pig": ("Pig grunt - Erdie.ogg", "erdie", "CC-BY-3.0", "https://creativecommons.org/licenses/by/3.0/", 2.0),
    "lion": ("Lion raring-sound1TamilNadu178.ogg", "த*உழவன் (Uzhavan / Info-farmer)", "Public-Domain", "https://commons.wikimedia.org/wiki/Template:PD-self", 3.0),
    "elephant": ("Elephant voice - trumpeting.ogg", "தகவலுழவன் (Info-farmer)", "CC0-1.0", "https://creativecommons.org/publicdomain/zero/1.0/", 2.0),
    "monkey": ("Campbell's-Monkeys-Use-Affixation-to-Alter-Call-Meaning-pone.0007808.s001.ogg", "Ouattara K, Lemasson A, Zuberbühler K (2009), PLOS ONE, doi:10.1371/journal.pone.0007808, Audio S1: Campbell's monkey hok call", "CC-BY-2.5", "https://creativecommons.org/licenses/by/2.5/", 2.0),
}


def prepare():
    import numpy as np
    (ASSETS / "audio").mkdir(parents=True, exist_ok=True)
    meta_file = ASSETS / "animal_sources.json"
    metadata = json.loads(meta_file.read_text()) if meta_file.exists() else {}
    with tempfile.TemporaryDirectory(prefix="noah-animals-") as tmp:
        for animal in list(SOURCES) + list(COMMONS):
            target = ASSETS / "audio" / ("sound_" + animal + ".wav")
            if animal in metadata and target.exists() and hashlib.sha256(target.read_bytes()).hexdigest()==metadata[animal]["processed_sha256"]: continue
            if animal in SOURCES:
                page, seconds = SOURCES[animal]
                html = urllib.request.urlopen(page, timeout=30).read().decode()
                assert "CC0" in html, "Source license must be checked"
                data = next(json.loads(s) for s in re.findall(r'<script[^>]*type=[\"\']application/ld\+json[\"\'][^>]*>(.*?)</script>', html, re.S)
                            if json.loads(s).get("@type") == "AudioObject")
                original_url = next(e["contentUrl"] for e in data["encoding"] if e["encodingFormat"] == "audio/wav")
                assert original_url.startswith("https://bigsoundbank.com/UPLOAD/")
                title,author,license_id,license_url=data["name"],data.get("author"),"CC0-1.0","https://creativecommons.org/publicdomain/zero/1.0/"
            else:
                title,author,license_id,license_url,seconds=COMMONS[animal]
                name=title.replace(" ","_"); digest=hashlib.md5(name.encode()).hexdigest()
                original_url="https://upload.wikimedia.org/wikipedia/commons/"+digest[0]+"/"+digest[:2]+"/"+urllib.parse.quote(name)
                page="https://commons.wikimedia.org/wiki/File:"+urllib.parse.quote(name)
            raw = urllib.request.urlopen(urllib.request.Request(original_url,headers={"User-Agent":"NoahOfflineGame/1.2 audio authoring"}), timeout=60).read()
            original = Path(tmp) / (animal + ".wav"); original.write_bytes(raw)
            decoded = subprocess.run(["ffmpeg", "-v", "error", "-i", str(original), "-ac", "1", "-ar", "22050", "-f", "f32le", "pipe:1"], capture_output=True, check=True).stdout
            samples = np.frombuffer(decoded, dtype="<f4").copy()
            rate = 22050; length = min(len(samples), int(seconds * rate)); step = int(rate * .05)
            starts = range(0, max(1, len(samples) - length + 1), step)
            start = max(starts, key=lambda i: float(np.mean(samples[i:i+length] ** 2)))
            clip = samples[start:start+length]
            rms = float(np.sqrt(np.mean(clip ** 2)))
            peak = float(np.max(np.abs(clip)))
            if peak: clip *= min(.12 / max(rms, .001), .82 / peak)
            fade = min(int(.02 * rate), len(clip) // 8)
            if fade:
                clip[:fade] *= np.linspace(0, 1, fade)
                clip[-fade:] *= np.linspace(1, 0, fade)
            with wave.open(str(target), "wb") as output:
                output.setnchannels(1); output.setsampwidth(2); output.setframerate(rate)
                output.writeframes((np.clip(clip, -1, 1) * 32767).astype("<i2").tobytes())
            metadata[animal] = {"kind": "real_field_recording", "page_url": page, "file_url": original_url,
                "title": title, "author": author, "license": license_id, "license_url":license_url,
                "source_sha256": hashlib.sha256(raw).hexdigest(), "processed_sha256": hashlib.sha256(target.read_bytes()).hexdigest(),
                "excerpt_start_seconds": round(start/rate, 3), "excerpt_seconds": round(length/rate, 3),
                "processing": "excerpt, resample to mono PCM 22050 Hz, level adjustment, edge fades; original animal pitch retained"}
            print("Real animal recording ready:", animal, flush=True)
    (ASSETS / "animal_sources.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__": prepare()
