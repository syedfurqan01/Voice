# Voice by Spark

**Android AI digital assistant** developed under FeatherTech.

Local-first. No mandatory backend or API key for core functionality.  
Selectable as the default Android digital assistant.

## Current status

**Phase 0 + Phase 1 complete**

- Project foundation (Kotlin + Jetpack Compose + modular structure)
- Minimal premium dark UI dashboard
- VoiceInteractionService registered so the system can select Voice as default assistant
- Temporary session overlay only (no permanent floating bubble)
- GitHub Actions workflow that builds and uploads the debug APK

## Roadmap

| Phase | Focus |
|-------|--------|
| 0 | Architecture & project foundation |
| 1 | Android assistant foundation + dashboard |
| 2 | Speech recognition + Text-to-Speech |
| 3 | Local AI brain (llama.cpp + quantized GGUF) |
| 4 | Phone control (Accessibility, Intents, system APIs) |
| 5 | App-specific workflows (YouTube, Instagram, WhatsApp…) |
| 6 | Opt-in adaptive learning & local workflow memory |
| 7 | Multi-step autonomous tasks + release |

## Build

Push to `main` or run the workflow manually → download the **voice-debug-apk** artifact.

```bash
./gradlew assembleDebug
```

APK appears at `app/build/outputs/apk/debug/`.

## Target device

Optimized for **Poco X3 Pro** (Android 12 / API 31) while remaining compatible with newer Android versions.

## Safety principles

- Explicit user permission for sensitive capabilities
- Confirmation before sensitive actions
- No CAPTCHA bypass or unrestricted hidden-screen access
- Proper lifecycle, error handling and resource cleanup

---

FeatherTech · 2026
