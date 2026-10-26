# Asset provenance

The vector artwork and the short pentatonic melody and animal sound effects were created for this project. Animal effects are synthesized illustrations of sounds, not field recordings.

Spanish narration is synthesized, using Piper 1.3.0 and `es_MX-claude-high` from `rhasspy/piper-voices`, revision `5041a7a49ae5b3a0f30e6837769eb03fdbb231bd`. The voice model's model card identifies its source dataset as Apache-2.0:

- Model card: https://huggingface.co/rhasspy/piper-voices/blob/5041a7a49ae5b3a0f30e6837769eb03fdbb231bd/es/es_MX/claude/high/MODEL_CARD
- Source dataset: https://huggingface.co/spaces/HirCoir/Piper-TTS-Spanish
- Build-time synthesizer: https://github.com/OHF-Voice/piper1-gpl (GPL-3.0)

Only rendered PCM WAV recordings are included in the APK. Neither Piper nor the neural voice model is distributed inside the Android app. Audio transcripts are in `app/src/main/assets/narration.json`. The attribution and Apache-2.0 text are also bundled in the APK.

To regenerate recordings, install `piper-tts==1.3.0` in an isolated development environment, download the pinned model and configuration, then run:

```sh
python3 tools/generate_audio.py --model /path/to/es_MX-claude-high.onnx
```

The app itself never executes this script or downloads any content.
