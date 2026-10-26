# Asset provenance — Noah edition

The vector artwork, animated happy star/fireworks, pentatonic melody and gentle reward chime were created for this project.

## Real animal recordings

The five animal WAV files contain excerpts of actual recordings, rather than generated sound effects. Sources are published under CC0-1.0 by BigSoundBank:

| Animal | Recording | Author |
| --- | --- | --- |
| Cow | [Cow Moos #1](https://bigsoundbank.com/cow-moos-s0546.html) | Joseph Sardin |
| Dog | [Barking dog #3](https://bigsoundbank.com/barking-dog-3-s2955.html) | Joseph Sardin |
| Cat | [Little meow of a cat #6](https://bigsoundbank.com/little-meow-of-a-cat-6-s1476.html) | Joseph Sardin |
| Bird | [Robin #1](https://bigsoundbank.com/robin-1-s1667.html) | Joseph Sardin |
| Duck | [Ducks](https://bigsoundbank.com/ducks-s0276.html) | DenisChardonnet |

Adaptations: short excerpt, resampling to mono PCM 22050 Hz, level adjustment and brief edge fades. Original animal pitch is retained. Exact source URLs, excerpt positions and SHA-256 digests are included in `app/src/main/assets/animal_sources.json`.

## Fictional child-character narration

Milo's 55 Spanish recordings are synthetic, with a child-character vocal register. They are not recordings of a real child. The build-authoring pipeline first synthesizes text using Piper 1.3.0 and `es_MX-claude-high`, then uses WORLD via pyworld 0.3.5 to adjust pitch and spectral envelope independently, keeping speech duration unchanged. Target voiced median: 280 Hz; spectral-envelope scale: 1.18. Per-clip measurements and profile are bundled in `voice_profile.json`.

- Base model: `rhasspy/piper-voices`, revision `5041a7a49ae5b3a0f30e6837769eb03fdbb231bd`.
- [Model card](https://huggingface.co/rhasspy/piper-voices/blob/5041a7a49ae5b3a0f30e6837769eb03fdbb231bd/es/es_MX/claude/high/MODEL_CARD) identifies the original dataset as Apache-2.0.
- Source dataset: https://huggingface.co/spaces/HirCoir/Piper-TTS-Spanish
- Piper build-time engine: https://github.com/OHF-Voice/piper1-gpl (GPL-3.0).
- WORLD build-time vocoder: https://github.com/mmorise/World (modified BSD); Python wrapper: https://github.com/JeremyCCHsu/Python-Wrapper-for-World-Vocoder (MIT).

Only rendered PCM WAV files are packaged. The Android app contains no TTS engine, vocoder, neural model, downloads or external service. Transcripts, credits and Apache-2.0 license text are bundled.

Normal Android builds use the committed recordings. Optional authoring tools:

```sh
CC=gcc CXX=g++ python3 -m pip install -r tools/requirements-audio.txt
python3 tools/generate_audio.py --model /path/to/es_MX-claude-high.onnx
python3 tools/prepare_animal_audio.py
```

The latter downloads the licensed source recordings only during development; it is never run inside the APK.
