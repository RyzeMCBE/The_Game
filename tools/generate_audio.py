"""Regenerate the bundled Spanish recordings and original musical/sound effects.

Optional developer tool: pip install piper-tts==1.3.0; pass --model PATH.
Normal Android builds use the committed WAV files and never run TTS or download models.
"""
from pathlib import Path
import argparse
import array
import json
import math
import random
import wave

ROOT = Path(__file__).resolve().parents[1]
AUDIO = ROOT / "app/src/main/assets/audio"
PHRASES = {
    "welcome": "¡Hola! Soy Milo. ¿Qué quieres hacer hoy?",
    "paint_help": "Este es tu taller. Elige un color y pinta con tu dedo.",
    "fill_help": "Toca una parte del dibujo para llenarla de color.",
    "stamp_help": "Toca tu hoja para poner una pegatina.",
    "games_help": "Vamos a jugar. Elige una actividad.",
    "learn_help": "Vamos a descubrir colores, números y animales.",
    "memory_help": "Busca dos animales iguales. Toca una tarjeta.",
    "memory_match": "¡Encontraste una pareja!",
    "memory_again": "Miremos otra vez. Busca dos iguales.",
    "puzzle_help": "Vamos a armar el dibujo. Toca una pieza y después su lugar.",
    "puzzle_pick": "Ahora toca el lugar de la pieza.",
    "puzzle_again": "Probemos en otro lugar.",
    "numbers_help": "Cuenta las manzanas. ¿Cuántas hay?",
    "try_again": "Vamos pasito a pasito. Prueba otra vez.",
    "praise_1": "¡Muy bien! Lo descubriste.",
    "praise_2": "¡Qué bien! Sigamos explorando.",
    "praise_3": "¡Lo lograste! Aquí tienes una estrellita.",
    "finished": "¡Terminaste! Vamos a celebrar.",
    "saved": "¡Tu dibujo está guardado!",
    "save_error": "No pude guardar el dibujo. Vamos a intentarlo otra vez.",
    "gallery_help": "Aquí están tus dibujos. Toca uno para mirarlo.",
    "break_time": "Es hora de descansar. Vamos a jugar un ratito fuera de la pantalla.",
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
    "ask_cow": "¿Qué animal hace este sonido? Escucha a la vaca.",
    "ask_dog": "¿Qué animal hace este sonido? Escucha al perro.",
    "ask_cat": "¿Qué animal hace este sonido? Escucha al gato.",
    "ask_bird": "¿Qué animal hace este sonido? Escucha al pajarito.",
    "ask_duck": "¿Qué animal hace este sonido? Escucha al pato.",
    "cow": "¡Es la vaca! La vaca hace mu.", "dog": "¡Es el perro! El perro hace guau, guau.",
    "cat": "¡Es el gato! El gato hace miau.", "bird": "¡Es el pajarito! El pajarito hace pío, pío.",
    "duck": "¡Es el pato! El pato hace cuac, cuac.",
}


def write_wave(name, samples, rate=22050):
    pcm = array.array("h", (int(max(-1, min(1, x)) * 28000) for x in samples))
    import sys
    if sys.byteorder != "little":
        pcm.byteswap()
    with wave.open(str(AUDIO / (name + ".wav")), "wb") as output:
        output.setnchannels(1); output.setsampwidth(2); output.setframerate(rate); output.writeframes(pcm.tobytes())


def effects():
    rate = 22050
    rng = random.Random(17)
    for animal in ["cow", "dog", "cat", "bird", "duck"]:
        duration = 1.8
        samples = []
        phase = 0.0
        for i in range(int(rate * duration)):
            t = i / rate
            envelope = math.sin(math.pi * min(1, t / duration)) ** 1.5
            if animal == "cow":
                frequency = 118 + 25 * math.sin(t * 2.7)
                phase += 2 * math.pi * frequency / rate
                sample = (.42 * math.sin(phase) + .17 * math.sin(phase * 2) + .08 * math.sin(phase * 4)) * envelope
            elif animal == "cat":
                frequency = 460 + 150 * math.sin(t * 3.5)
                phase += 2 * math.pi * frequency / rate
                sample = (.35 * math.sin(phase) + .12 * math.sin(phase * 2.01)) * envelope
            elif animal == "bird":
                local = t % .45
                frequency = 1800 + 1100 * math.sin(local * 13)
                phase += 2 * math.pi * frequency / rate
                sample = .32 * math.sin(phase) * math.sin(math.pi * min(local / .25, 1)) ** 2 if local < .25 else 0
            else:
                local = t % .65
                burst = math.sin(math.pi * min(local / .27, 1)) ** 2 if local < .27 else 0
                frequency = (170 if animal == "dog" else 350) + 50 * math.sin(t * 5)
                phase += 2 * math.pi * frequency / rate
                sample = (.27 * math.sin(phase) + .14 * math.sin(phase * 3) + .07 * rng.uniform(-1, 1)) * burst
            samples.append(sample)
        write_wave("sound_" + animal, samples)
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


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", type=Path, required=True)
    args = parser.parse_args()
    from piper import PiperVoice, SynthesisConfig
    AUDIO.mkdir(parents=True, exist_ok=True)
    voice = PiperVoice.load(str(args.model))
    config = SynthesisConfig(length_scale=1.12, noise_scale=.6, noise_w_scale=.7, volume=.85)
    for key, text in PHRASES.items():
        print("Recording", key, flush=True)
        with wave.open(str(AUDIO / (key + ".wav")), "wb") as output:
            voice.synthesize_wav(text, output, syn_config=config)
    effects()
    (AUDIO.parent / "narration.json").write_text(json.dumps(PHRASES, ensure_ascii=False, indent=2), encoding="utf-8")
