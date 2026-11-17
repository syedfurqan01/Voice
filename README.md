# Voice by Spark

**Android AI digital assistant** developed under FeatherTech.

Local-first. No mandatory backend or API key for core functionality.  
Selectable as the default Android digital assistant.

## Current status

**Phase 2 complete** — Speech recognition + Text-to-Speech

- Project foundation (Kotlin + Jetpack Compose)
- Default digital assistant registration
- Minimal premium dark dashboard
- System SpeechRecognizer (partial results, Hindi + English)
- Text-to-Speech with rate/pitch and duplicate guard
- Command processor: Hello, name, capabilities, time, open settings, stop speaking/listening
- Temporary session overlay only (no permanent floating bubble)
- Honest network status for recognition
- GitHub Actions → debug APK artifact

## Roadmap

| Phase | Focus |
|-------|--------|
| 0–1 | Architecture, assistant foundation, dashboard |
| **2** | **Speech recognition + TTS** |
| 3 | Local AI brain (llama.cpp + GGUF) |
| 4 | Phone control (Accessibility, Intents) |
| 5 | App workflows (YouTube, Instagram, WhatsApp…) |
| 6 | Opt-in adaptive learning |
| 7 | Multi-step tasks + release |

## Build

Push to `main` or run the workflow manually → download **voice-debug-apk**.

```bash
gradle assembleDebug
```

## Target device

Optimized for **Poco X3 Pro** (Android 12 / API 31).

## Privacy

- Microphone requested only when listening
- No raw audio storage by default
- No mandatory backend
- System recognition network status is shown honestly

---

FeatherTech · 2026
