# JeromeLens.vw

**Screenshot Text Extractor & Clipboard Manager for Android**

Privacy-first, fully on-device OCR. Take a screenshot → text is extracted with ML Kit → interactive overlay lets you select & copy → everything is saved in a searchable history.

---

## Current Status (MVP + Fixes)

| Feature                        | Status     | Notes                                      |
|--------------------------------|------------|--------------------------------------------|
| Screenshot detection           | ✅ Fixed   | MediaStore + FileObserver + debouncing     |
| On-device OCR (ML Kit)          | ✅ Working | With image downsampling                    |
| Interactive text overlay       | ✅ Working | Tap to select blocks                       |
| Copy to clipboard              | ✅ Working |                                            |
| History + Search + Favorites   | ✅ Working | Room database                              |
| Runtime permissions            | ✅ Fixed   | Media + Notifications                      |
| GitHub Actions APK build       | 🔄 Running | Latest fixes just pushed                   |
| Floating bubble                | ⏳ Later   | Phase 2                                    |
| Smart parsing (URL/email/code) | ⏳ Later   | Phase 2                                    |
| AI actions / Translate         | ⏳ Later   | Phase 3                                    |

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
11. Take any screenshot → the overlay should appear

You can also manually trigger a new build: **Actions → Build Debug APK → Run workflow**

---

## Permissions required

- **Accessibility Service** – to detect screenshots reliably
- **Display over other apps** – for the text overlay
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
- GitHub Actions for automated APK builds

---

## Project structure

```
app/src/main/java/com/jeromelens/app/
├── data/          # Room entities, DAO, Repository, Hilt module
├── ocr/           # ML Kit OCR processor
├── service/       # ScreenshotDetectionService + FloatingBubbleService
└── ui/            # Activities, OverlayView, Adapters, ViewModels
```

---

Built and iteratively fixed as a complete MVP. Enjoy!
