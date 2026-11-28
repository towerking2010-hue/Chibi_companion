# Miko 0.9.3 — companion experience redesign

## Design intent
Miko is the primary experience: a character-first home screen, a prominent summon/hide action, compact feature destinations, and a floating companion that remains available over other apps.

## Visual system
- Midnight navy surfaces with cool blue and restrained violet accents.
- High-contrast white text and muted blue-grey secondary text.
- Shared palette and surface language across home, full chat, notes, tasks, settings, and contextual sheets.
- Floating quick chat and its reply bubble use the same dark palette; neither uses the previous pale lavender panel.
- Official launcher artwork uses the approved moonlit-sakura illustration: full face visible, head and hair emerging over the circular badge, no lettering. The same artwork is used by the app launcher and long-press summon shortcut.
- Native Android Views and lightweight ViewPropertyAnimator transitions; no heavy animation framework.

## Architecture and platform behavior
- Native Kotlin/Android Views for predictable performance on the Galaxy A31.
- MikoPresenceStore is the single persisted state gateway for floating presence and Quick Settings refresh.
- FloatingCompanionService owns the actual WindowManager overlay and its quick composer.
- MikoSpriteView remains the three-user-image mouth renderer. It is timed mouth-flap during TTS, not phoneme-level lip-sync.
- Home-screen entrance, quick composer entrance, and reply-bubble entrance use native view animations.
- Quick Settings tile toggles summon/hide. On Android 12 the user must add the tile manually through Quick Settings Edit. The launcher long-press shortcut is an additional route.
- Floating overlay requires Android's Display over other apps permission. The persistent notification includes a Turn off action.

## Existing features and limits
- Chat history, notes, tasks, reminders, and memory remain local unless an explicitly configured provider is used.
- Cloud AI is opt-in and requires a user-supplied provider key. This release does not include a bundled paid AI service or guarantee free cloud inference.
- Quick chat supports image attachments, text files, first-page PDF image extraction, and a video's opening frame. It does not process full videos or arbitrary Office documents.
- Offline responses are basic local fallback behavior, not a full offline language model.
- Camera, voice recognition, and screen understanding depend on Android permissions and supported provider/device capabilities.

## Release validation
- Version 0.9.3 / versionCode 16.
- CI build must succeed and produce a signed release APK.
- Physical Samsung Galaxy A31 / Android 12 testing is still required for overlay positioning, keyboard resizing, Quick Settings behavior, icon masking, voice/camera permissions, process recreation, and performance.
- CI success is not a substitute for physical-device testing. Unverified behavior must remain marked as unverified.
