# JeromeLens v2.0

**Screenshot Text Extractor & Smart Clipboard Manager for Android**

Privacy-first, fully on-device OCR + smart actions.  
Take a screenshot → text is extracted with ML Kit → interactive overlay lets you select & copy → **smart entity actions** (Open link / Call / Email / Copy code) → everything is saved in a searchable history.  
Optional **Floating Bubble** for quick access.

---

## What's new in v2.0

| Feature                        | Status     | Notes                                      |
|--------------------------------|------------|--------------------------------------------|
| Screenshot detection           | ✅         | MediaStore + FileObserver + debouncing     |
| On-device OCR (ML Kit)          | ✅         | With image downsampling                    |
| Interactive text overlay       | ✅         | Tap to select blocks                       |
| Copy to clipboard              | ✅         |                                            |
| History + Search + Favorites   | ✅         | Room database                              |
| Runtime permissions            | ✅         | Media + Notifications                      |
| **Floating Bubble**            | ✅ **New** | Draggable overlay, tap = History           |
| **Smart Entity Parsing**       | ✅ **New** | URL / Email / Phone / Code detection       |
| **One-tap Actions**            | ✅ **New** | Open, Call, Email, Copy Code               |
| GitHub Actions APK build       | ✅         |                                            |
| Multi-language OCR             | ⏳ Later   | ML Kit already supports many languages     |
| On-device LLM (summarize etc.) | ⏳ Later   | Phase 3                                    |

---

## How to install on your Android phone (no PC)

1. Open this repo on your phone: **https://github.com/jay117king/JeromeLens**
2. Go to the **Actions** tab
3. Find the latest **Build Debug APK** run that shows a green checkmark ✅
4. Download the artifact named **JeromeLens-debug-apk**
5. Extract the ZIP and install the `.apk`
6. Allow “Install unknown apps” if asked
7. Open **JeromeLens**
8. Grant the permissions it asks for (Photos/Media + Notifications)
9. Tap **Enable Accessibility Service** → turn JeromeLens on
10. Tap **Enable Overlay Permission** → allow it
11. (Optional) Tap **Start Floating Bubble**
12. Take any screenshot → the overlay should appear with smart actions

You can also manually trigger a new build: **Actions → Build Debug APK → Run workflow**

---

## Permissions required

- **Accessibility Service** – to detect screenshots reliably
- **Display over other apps** – for the text overlay + floating bubble
- **Photos and videos / Media** – to read the screenshot file
- **Notifications** (Android 13+) – for the foreground service

Everything runs **100% on-device**. No cloud, no tracking.

---

## Tech Stack

- Kotlin + Coroutines + Flow
- Hilt (Dependency Injection)
- Room (local database)
- Google ML Kit Text Recognition (on-device)
- Material 3 + View Binding
- SmartEntityParser (pure Kotlin regex + heuristics)
- GitHub Actions for automated APK builds

---

## Project structure

```
app/src/main/java/com/jeromelens/app/
├── data/          # Room entities, DAO, Repository, Hilt module
├── ocr/           # ML Kit OCR processor
├── service/       # ScreenshotDetectionService + FloatingBubbleService
├── ui/            # Activities, OverlayView, Adapters, ViewModels
└── util/          # SmartEntityParser and helpers
```

---

## Team / Development model

This project is advanced with an OpenManus-inspired multi-agent mindset:

- **Lenssmith** – OCR & detection core
- **Overlay Architect** – UI & floating bubble
- **Parser Agent** – Smart entity detection & actions
- **Coordinator** – Roadmap & releases

Contributions welcome via issues and PRs.

---

Built as a complete privacy-first MVP and upgraded to v2.0 with floating bubble + smart actions. Enjoy!
