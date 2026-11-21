# Bypass IDE (Android — Kotlin & Jetpack Compose)

Bypass IDE is a native Android mobile IDE rewritten in Kotlin and Jetpack Compose, powered by **Firebase AI Logic** (Gemini 3.8 Flash & Gemini 3.5 Flash-Lite) and App Check.

## Features

- **Bypass AI Agent & Chat (`HomeAgentView`)**:
  - Streaming Gemini responses via `Firebase.ai` with **Chat** (architecture Q&A, structured Markdown, tables, copy/apply code blocks) and **Agent** (autonomous `<file>`, `<edit_file>`, `<mkdir>`, `<delete_file>`, and `<command>` execution) modes.
  - Multi-session chat history with session creation, switching, copying, and clearing.
  - Live `@web` search grounding via DuckDuckGo and Wikipedia APIs with an **Examined Sources** detail modal.
- **File Explorer (`FileExplorerView`)**:
  - App-scoped workspace (`BypassProjects/`) with project switching, project creation, file/folder creation, renaming, deletion, and file-type badges.
- **Code Editor (`CodeEditorView`)**:
  - Multi-tab monospace code editor (`JetBrains Mono`) with line numbers, undo/redo stacks, dirty indicators, find & replace, quick symbol accessory bar, and inline **AI Edit**.
- **Live Preview (`LivePreviewView`)**:
  - Embedded `WebView` rendering the active project's `index.html` with responsive viewport toggles (`Full`, `Mobile 375px`, `Tablet 768px`), real-time JavaScript console telemetry, and **Fix with AI** diagnostics.
- **Interactive Terminal (`TerminalView`)**:
  - Scoped shell command execution inside the active project workspace with quick command chips and exit code reporting.
- **Maria Voice Assistant (`VoiceCallOverlay`)**:
  - Animated neural orb and equalizer UI with Android `SpeechRecognizer` voice input, Gemini conversational answers, and `TextToSpeech` playback.
- **IDE Settings & Network Shield (`SettingsDialog` / `DeveloperProfileView`)**:
  - Reasoning tier selection, `@web` grounding and editor toggles, live public IP & `generate_204` connectivity probes, socket pool reset, and developer profile card.

## Dual-Mode AI Engine

- **Built-in Firebase AI Logic (Default)**: Uses keyless `Firebase.ai` (`gemini-3.8-flash` with automatic `gemini-3.5-flash-lite` fallback) secured by App Check.
- **Optional Direct Gemini API Key**: In **IDE Settings**, users can optionally enter their own Gemini API key and custom model ID (stored strictly on-device) for standalone debug builds or custom endpoints.
- **Fast `@web` Grounding**: DuckDuckGo and Wikipedia queries run in parallel with a strict 4.5s timeout and are opt-in via the `@web` pill or `@web` prompt keyword so standard chats stream immediately.

## GitHub Actions APK Build

Pushing to GitHub automatically runs `.github/workflows/build-apk.yml` using the included Gradle Wrapper (`./gradlew`):
1. Runs `./gradlew :app:testDebugUnitTest`
2. Builds both **Debug** (`BypassIDE-debug.apk`) and **Signed Release** (`BypassIDE-release.apk`) APKs
3. Uploads APKs to **GitHub Actions Artifacts** (`BypassIDE-debug-apk`, `BypassIDE-release-apk`, `BypassIDE-all-apks`)
4. Publishes APKs directly to **GitHub Releases** on pushes to `main`, `master`, or `v*` tags

## Notes

- Original APK-only root-subsystem / VPN-tunnel / wake-word background service features that depend on device root or private backend servers are omitted; all local shell execution runs safely within the app's sandboxed workspace.
