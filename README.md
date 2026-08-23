# JeromeLens.vw

**Screenshot Text Extractor & Clipboard Manager for Android**

Privacy-first, fully on-device OCR. Detect screenshots → extract text with ML Kit → interactive overlay to select/copy → searchable history.

## Features (MVP)

- Screenshot detection via Accessibility Service + FileObserver
- On-device OCR with Google ML Kit
- Interactive overlay: tap text blocks to select & highlight
- One-tap copy to clipboard
- Persistent history with search (Room + SQLite)
- Favorites & delete
- Modern Material 3 UI

## How to install on your Android phone (no PC needed)

1. Open this repository on your phone browser: https://github.com/jay117king/JeromeLens
2. Go to the **Actions** tab
3. Select the latest **Build Debug APK** workflow run (or trigger a new one with "Run workflow")
4. Download the artifact **JeromeLens-debug-apk**
5. Extract the ZIP and install the `.apk` (enable "Install unknown apps" for your browser/files app)
6. Open JeromeLens → Enable **Accessibility Service** and **Overlay** permission
7. Take a screenshot → the overlay appears with selectable text!

## Permissions

- Accessibility Service (detect screenshots)
- Display over other apps (overlay)
- Read media / storage (access screenshot files)

Everything runs **on-device**. No cloud, no tracking.

## Tech Stack

- Kotlin + Coroutines + Flow
- Hilt DI
- Room
- ML Kit Text Recognition
- Material 3
- View Binding

## Roadmap

- Phase 2: Floating bubble, smart parsing (URLs, emails, code), categories
- Phase 3: AI actions (summarize, translate), full-text search, export, sync

---

Built as a complete MVP from the specification. Enjoy!
