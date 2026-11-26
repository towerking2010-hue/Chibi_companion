# Miko — Android companion source

A lightweight native Kotlin Android companion prototype intended for Android 8+ and the Samsung Galaxy A31.

## Current implementation
- Native Android app and foreground service (no WebView).
- User-approved `SYSTEM_ALERT_WINDOW` floating overlay.
- Custom Canvas-drawn silver-haired, blue-eyed chibi with idle bobbing, blinking and simple mood changes.
- Draggable avatar; double-tap opens/closes a native floating chat panel.
- Chat transcript saved locally in SharedPreferences.
- Android TextToSpeech with adjustable pitch and speech rate.
- Gemini, Groq, OpenRouter and custom OpenAI-compatible HTTPS endpoints.
- Sequential provider/key failover: a failed request advances to the next saved provider.
- API keys encrypted using AES-GCM with an Android Keystore key.
- Basic local scripted fallback when cloud providers are unavailable.
- Basic app launching, Android Settings launching, Google search, and Android folder picker commands.
- Local task list with add, complete, delete and clear-completed actions; tasks are stored on-device.
- GitHub Actions workflow for building debug and signed release APKs and uploading both as artifacts.

## Build
Open this directory in Android Studio with JDK 17 and allow Gradle sync. Build `app` > `assembleDebug`, or push to GitHub and run the `Android debug build` workflow. The workflow uploads `app-debug.apk` as `miko-debug-apk`.

## First launch
1. Install the debug APK.
2. Open Miko.
3. Grant Android's Display over other apps permission.
4. Start the floating companion.
5. Double-tap the chibi to open the overlay chat.
6. Add provider keys in the chat/settings activity in priority order.

## Important limitations
The rebuild branch now produces a signed release APK in GitHub Actions. CI confirms compilation and packaging, but this is **not a device-verified release**: install, launch, overlay, chat, voice, and phone-action behavior still need testing on a physical Galaxy A31. The avatar is a lightweight Canvas illustration, not a Live2D model or a finished sprite animation set. Its current animation is idle bobbing/blinking with simple expression changes.

Provider fallback is sequential and request-error based; it does not have reliable provider-side quota telemetry, automatic quota prediction, per-key cooldown scheduling, streaming, or provider health dashboards. Free cloud tiers have changing limits and are not unlimited. If every cloud provider is unavailable, the app uses short scripted offline responses; it does not include a local large language model.

Phone commands are deliberately limited to Android intents and the document-tree picker. They do not provide arbitrary control of other apps or settings. A folder picker asks the user to select and grant access; it does not silently open arbitrary folders.

Android may stop background services under battery restrictions. The foreground notification includes a stop action. The app requires the user to grant overlay permission explicitly. Use only API keys you own and keep them private.
