"""Build the fictional child-character narration for Noah (never run on Android).

pip install piper-tts==1.3.0 pyworld==0.3.5 numpy
Only the rendered recordings are packaged; no voice model or vocoder is shipped.
"""
from pathlib import Path
import argparse
import array
import json
import math
import tempfile
import wave

ROOT = Path(__file__).resolve().parents[1]
AUDIO = ROOT / "app/src/main/assets/audio"
PHRASES = {
    "welcome": "¡Hola, Noah! Soy Milo. ¿Jugamos?",
    "paint_help": "Noah, elige un color y pinta con tu dedo.",
    "fill_help": "Toca una parte del dibujo para ponerle color.",
    "stamp_help": "Toca tu hoja para poner una pegatina.",
    "games_help": "Noah, vamos a jugar. Elige una aventura.",
    "learn_help": "Vamos a descubrir cosas nuevas, Noah.",
    "memory_help": "Busca dos animales iguales. Toca una tarjeta.",
    "memory_match": "¡Encontraste una pareja!",
    "memory_again": "Miremos otra vez. Busca dos iguales.",
    "puzzle_help": "Noah, toca una pieza y después su lugar.",
    "puzzle_pick": "Ahora toca el lugar de la pieza.",
    "puzzle_again": "Probemos en otro lugar.",
    "numbers_help": "Cuenta las manzanas. ¿Cuántas hay?",
    "try_again": "Vamos pasito a pasito, Noah. Prueba otra vez.",
    "praise_1": "¡Muy bien, Noah! Lo descubriste.",
    "praise_2": "¡Qué bien, Noah! Sigamos jugando.",
    "praise_3": "¡Muy bien, Noah! Esta estrellita es para ti.",
    "finished": "¡Noah, lo lograste!",
    "saved": "¡Noah, tu dibujo está guardado!",
    "save_error": "No pude guardar el dibujo. Vamos a intentarlo otra vez.",
    "gallery_help": "Aquí están tus dibujos. Toca uno para mirarlo.",
    "break_time": "Noah, vamos a descansar un ratito. Milo también necesita una pausa.",
    "red": "Rojo.", "yellow": "Amarillo.", "blue": "Azul.",
    "green": "Verde.", "purple": "Morado.", "orange": "Naranja.", "brown": "Marrón.",
    "circle": "¡Es un círculo!", "square": "¡Es un cuadrado!", "triangle": "¡Es un triángulo!",
    "ask_red": "Busca el color rojo.", "ask_yellow": "Busca el color amarillo.",
    "ask_blue": "Busca el color azul.", "ask_green": "Busca el color verde.",
    "ask_purple": "Busca el color morado.",
    "ask_circle": "Toca el círculo.", "ask_square": "Toca el cuadrado.", "ask_triangle": "Toca el triángulo.",
    "number_1": "Una manzana. Uno.", "number_2": "Una, dos manzanas. Dos.",
    "number_3": "Una, dos, tres manzanas. Tres.",
    "number_4": "Una, dos, tres, cuatro manzanas. Cuatro.",
    "number_5": "Una, dos, tres, cuatro, cinco manzanas. Cinco.",
    "ask_cow": "¿Qué animal escuchas?", "ask_dog": "¿Qué animal escuchas?",
    "ask_cat": "¿Qué animal escuchas?", "ask_bird": "¿Qué animal escuchas?",
    "ask_duck": "¿Qué animal escuchas?",
    "cow": "¡Sí! Es la vaca.", "dog": "¡Sí! Es el perro.",
    "cat": "¡Sí! Es el gato.", "bird": "¡Sí! Es el pajarito.",
    "duck": "¡Sí! Es el pato.",
}


def write_wave(name, samples, rate=22050):
    pcm = array.array("h", (int(max(-1, min(1, x)) * 28000) for x in samples))
    import sys
    if sys.byteorder != "little": pcm.byteswap()
    with wave.open(str(AUDIO / (name + ".wav")), "wb") as output:
        output.setnchannels(1); output.setsampwidth(2); output.setframerate(rate); output.writeframes(pcm.tobytes())


def music_and_chime():
    rate = 22050
    melody = [60, 64, 67, 69, 67, 64, 62, 60, 64, 67, 72, 69, 67, 64, 62, 60,
              62, 64, 67, 64, 60, 62, 64, 67, 69, 67, 64, 62, 60, 64, 62, 60]
    beat = .62
    music = []
    for note in melody:
        frequency = 440 * 2 ** ((note - 69) / 12)
        for i in range(int(rate * beat)):
            t = i / rate
            envelope = min(1, t / .025) * max(0, 1 - t / beat) ** 2
            music.append((.28 * math.sin(2 * math.pi * frequency * t) + .06 * math.sin(4 * math.pi * frequency * t)) * envelope)
    write_wave("music", music)
    chime = [0.0] * int(rate * 1.6)
    for index, note in enumerate([76, 79, 84]):
        start = int(index * .14 * rate)
        frequency = 440 * 2 ** ((note - 69) / 12)
        for i in range(int(rate * 1.1)):
            t = i / rate
            if start + i < len(chime):
                chime[start + i] += .22 * math.sin(2 * math.pi * frequency * t) * math.exp(-4 * t) * min(1, t / .012)
    write_wave("reward_chime", chime)


def child_character(source, destination):
    """Independent F0 and spectral-envelope changes; speech duration remains unchanged."""
    import numpy as np
    import pyworld as pw
    with wave.open(str(source)) as recording:
        rate = recording.getframerate()
        samples = np.frombuffer(recording.readframes(recording.getnframes()), dtype="<i2").astype(np.float64) / 32768
    f0, positions = pw.dio(samples, rate, f0_floor=65, f0_ceil=600, frame_period=5)
    f0 = pw.stonemask(samples, f0, positions, rate)
    spectral = pw.cheaptrick(samples, f0, positions, rate)
    aperiodicity = pw.d4c(samples, f0, positions, rate)
    voiced = f0 > 0
    median = float(np.median(f0[voiced])) if np.any(voiced) else 140
    ratio = 280 / median
    child_f0 = np.where(voiced, np.clip(f0 * ratio, 170, 490), 0)
    bins = np.arange(spectral.shape[1])
    shifted = np.array([np.interp(bins / 1.18, bins, row) for row in spectral])
    result = pw.synthesize(child_f0, shifted, aperiodicity, rate, frame_period=5)
    peak = float(np.max(np.abs(result)))
    if peak: result *= .88 / peak
    with wave.open(str(destination), "wb") as output:
        output.setnchannels(1); output.setsampwidth(2); output.setframerate(rate)
        output.writeframes((np.clip(result, -1, 1) * 32767).astype("<i2").tobytes())
    return {"source_median_hz": round(median, 2), "target_median_hz": 280, "pitch_ratio": round(ratio, 4)}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", type=Path, required=True)
    parser.add_argument("--sample-only", action="store_true")
    args = parser.parse_args()
    from piper import PiperVoice, SynthesisConfig
    AUDIO.mkdir(parents=True, exist_ok=True)
    voice = PiperVoice.load(str(args.model))
    config = SynthesisConfig(length_scale=1.12, noise_scale=.6, noise_w_scale=.7, volume=.85)
    analysis = {}
    with tempfile.TemporaryDirectory(prefix="noah-voice-") as tmp:
        source = Path(tmp) / "source.wav"
        for key, text in PHRASES.items():
            if args.sample_only and key != "welcome": continue
            print("Recording child character:", key, flush=True)
            with wave.open(str(source), "wb") as output: voice.synthesize_wav(text, output, syn_config=config)
            analysis[key] = child_character(source, AUDIO / (key + ".wav"))
    if not args.sample_only:
        music_and_chime()
        (AUDIO.parent / "narration.json").write_text(json.dumps(PHRASES, ensure_ascii=False, indent=2), encoding="utf-8")
        profile = {"kind": "synthetic_child_character", "child_name": "Noah", "locale": "es-MX", "duration_preserved": True,
                   "spectral_envelope_scale": 1.18, "source_model": "es_MX-claude-high", "processing": "WORLD F0 and spectral-envelope resynthesis", "clips": analysis}
        (AUDIO.parent / "voice_profile.json").write_text(json.dumps(profile, indent=2), encoding="utf-8")
