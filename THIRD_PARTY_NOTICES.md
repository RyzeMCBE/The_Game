# Asset provenance — Noah edition

The vector artwork, animated happy star/fireworks, pentatonic melody and gentle reward chime were created for this project.

## Real animal recordings

All 15 animal clips contain licensed excerpts of actual recordings. No mouth imitations or generated animal effects are used.

| Animal | Recording | Author | License |
| --- | --- | --- | --- |
| cow | [Cow Moos #1 — Free Download WAV & MP3 | BigSoundBank](https://bigsoundbank.com/cow-moos-s0546.html) | Joseph Sardin | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| dog | [Barking dog #3 — Free Sound Effect Download | BigSoundBank](https://bigsoundbank.com/barking-dog-3-s2955.html) | Joseph Sardin | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| cat | [Little meow of a cat #6 — Free Sound Effect Download | BigSoundBank](https://bigsoundbank.com/little-meow-of-a-cat-6-s1476.html) | Joseph Sardin | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| bird | [Robin #1 — Free Sound Effect Download | BigSoundBank](https://bigsoundbank.com/robin-1-s1667.html) | Joseph Sardin | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| duck | [Ducks — Free Download WAV & MP3 | BigSoundBank](https://bigsoundbank.com/ducks-s0276.html) | Joseph Sardin | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| sheep | [Sheep #1 — Free Download WAV & MP3 | BigSoundBank](https://bigsoundbank.com/sheep-1-s2343.html) | Joseph Sardin | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| goat | [Bleating Goat #1 — Free Download WAV & MP3 | BigSoundBank](https://bigsoundbank.com/bleating-goat-1-s0279.html) | Joseph Sardin | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| horse | [Horse Neighing #3 — Free Download WAV & MP3 | BigSoundBank](https://bigsoundbank.com/horse-neighing-3-s0863.html) | Joseph Sardin | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| hen | [Hens Lays — Free Download WAV & MP3 | BigSoundBank](https://bigsoundbank.com/hens-lays-s0978.html) | Joseph Sardin | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| frog | [One frog — Free Sound Effect Download | BigSoundBank](https://bigsoundbank.com/one-frog-s0819.html) | Joseph Sardin | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| owl | [Long-Eared Owl #1 — Free Download WAV & MP3 | BigSoundBank](https://bigsoundbank.com/long-eared-owl-1-s0936.html) | Joseph Sardin | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| pig | [Pig grunt - Erdie.ogg](https://commons.wikimedia.org/wiki/File:Pig_grunt_-_Erdie.ogg) | erdie | [CC-BY-3.0](https://creativecommons.org/licenses/by/3.0/) |
| lion | [Lion raring-sound1TamilNadu178.ogg](https://commons.wikimedia.org/wiki/File:Lion_raring-sound1TamilNadu178.ogg) | த*உழவன் (Uzhavan / Info-farmer) | [Public-Domain](https://commons.wikimedia.org/wiki/Template:PD-self) |
| elephant | [Elephant voice - trumpeting.ogg](https://commons.wikimedia.org/wiki/File:Elephant_voice_-_trumpeting.ogg) | தகவலுழவன் (Info-farmer) | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| monkey | [Campbell's-Monkeys-Use-Affixation-to-Alter-Call-Meaning-pone.0007808.s001.ogg](https://commons.wikimedia.org/wiki/File:Campbell%27s-Monkeys-Use-Affixation-to-Alter-Call-Meaning-pone.0007808.s001.ogg) | Ouattara K, Lemasson A, Zuberbühler K (2009), PLOS ONE, doi:10.1371/journal.pone.0007808, Audio S1: Campbell's monkey hok call | [CC-BY-2.5](https://creativecommons.org/licenses/by/2.5/) |

Adaptations: short excerpt, resampling to mono PCM 22050 Hz, level adjustment and brief edge fades. Original animal pitch is retained. Exact source URLs, excerpt positions and SHA-256 digests are included in `app/src/main/assets/animal_sources.json`. Attribution, source links, license links and the modification notice are bundled in `credits.txt` and accessible in Familia → Créditos. The Campbell's monkey sample is the research recording's “hok” call, not a generic composite monkey chatter.

## Fictional child-character narration

Milo's 128 Spanish recordings are synthetic, with a child-character vocal register. They are not recordings of a real child. The build-authoring pipeline first synthesizes text using Piper 1.3.0 and `es_MX-claude-high`, then uses WORLD via pyworld 0.3.5 to adjust pitch and spectral envelope independently, keeping speech duration unchanged. Target voiced median: 280 Hz; spectral-envelope scale: 1.18. Per-clip measurements and profile are bundled in `voice_profile.json`.

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
