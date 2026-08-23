# JeromeLens v2.1

**Screenshot & Image Text Extractor + Smart Clipboard Manager for Android**

Privacy-first, fully on-device OCR + smart actions + categories.  

- Take a screenshot **or upload up to 10 images** → text is extracted with ML Kit  
- Interactive overlay + **smart entity actions** (Open link / Call / Email / Copy code)  
- Assign clips to **categories** (Work, Code, Receipts, Notes…)  
- Searchable history with category filters  
- Optional **Floating Bubble** for quick access

---

## What's new in v2.1

| Feature                        | Status     | Notes                                      |
|--------------------------------|------------|--------------------------------------------|
| Screenshot detection           | ✅         | MediaStore + FileObserver + debouncing     |
| On-device OCR (ML Kit)          | ✅         | With image downsampling                    |
| Interactive text overlay       | ✅         | Tap to select blocks                       |
| Copy to clipboard              | ✅         |                                            |
| History + Search + Favorites   | ✅         | Room database                              |
| Floating Bubble                | ✅         | Draggable overlay                          |
| Smart Entity Parsing           | ✅         | URL / Email / Phone / Code                 |
| One-tap Actions                | ✅         | Open, Call, Email, Copy Code               |
| **Upload images (max 10)**     | ✅ **New** | Gallery picker → batch OCR                 |
| **Categories**                 | ✅ **New** | Work, Personal, Code, Receipts, Notes…     |
| Category filter in History     | ✅ **New** | Chip filters                               |

---

## How to use Upload + Categories

1. Open JeromeLens
2. Tap **Upload Images (max 10)**
3. Select 1–10 images from your gallery
4. Wait for OCR to finish on each image
5. Choose a **category** from the spinner (Work, Code, Receipts…)
6. Tap **Save All** → clips appear in History under that category
7. In History, use the category chips to filter

---

## How to install on your Android phone (no PC)

1. Open this repo on your phone: **https://github.com/jay117king/JeromeLens**
2. Go to the **Actions** tab
3. Find the latest **Build Debug APK** run that shows a green checkmark ✅
4. Download the artifact named **JeromeLens-debug-apk**
5. Extract the ZIP and install the `.apk`
6. Allow “Install unknown apps” if asked
7. Open **JeromeLens** and grant permissions
8. Enable Accessibility + Overlay if you want automatic screenshot detection
9. Use **Upload Images** anytime for manual OCR + categories

---

## Permissions required

- **Accessibility Service** – automatic screenshot detection
- **Display over other apps** – overlay + floating bubble
- **Photos and videos / Media** – read screenshots & gallery images
- **Notifications** (Android 13+) – foreground service

Everything runs **100% on-device**. No cloud, no tracking.

---

## Tech Stack

- Kotlin + Coroutines + Flow
- Hilt · Room · ML Kit Text Recognition
- Material 3 + View Binding
- SmartEntityParser + Categories
- GitHub Actions for automated APK builds

---

## Project structure

```
app/src/main/java/com/jeromelens/app/
├── data/          # Room + ClipRepository (categories, batch)
├── ocr/           # ML Kit OCR processor
├── service/       # ScreenshotDetection + FloatingBubble
├── ui/            # Main, Overlay, History, BatchOcr
└── util/          # SmartEntityParser, Categories
```

---

Built as a privacy-first OCR tool. Enjoy!
