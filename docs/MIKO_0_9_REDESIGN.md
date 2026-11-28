# Miko 0.9.5 — companion experience redesign

## Character animation pack
- Imports 11 user-supplied animated WebP actions: idle, happy, laughing, annoyed, astonished, blush, sad, sleepy, thinking, wave, and little dance.
- Black video backgrounds are converted to transparency with a neutral-black background flood-fill; dark navy character details are preserved.
- Animations are resized to 320 × 400 for the Galaxy A31 floating overlay and encoded as looping WebP at approximately 30 FPS.
- The app imports the ZIP once into private app storage. It does not need the user to import the pack again after an ordinary app update.
- Android's native AnimatedImageDrawable plays one selected animation at a time. It loops while the floating companion is visible and stops when the overlay detaches.
- Chat cues select a relevant expression/action; “dance” and “wave/hello” prompt the corresponding actions. The TTS pipeline keeps the selected full-body animation playing. The previous three-image mouth-flap remains a fallback if the animation pack is not imported.

## Design intent
Miko is the primary experience: a character-first home screen, a prominent summon/hide action, compact feature destinations, and a floating companion that remains available over other apps.

## Visual system
- Midnight navy surfaces with cool blue and restrained violet accents.
- High-contrast white text and muted blue-grey secondary text.
- Shared palette and surface language across home, full chat, notes, tasks, settings, and contextual sheets.
- Floating quick chat and its reply bubble use the same dark palette.
- Official launcher artwork uses the approved moonlit-sakura illustration: full face visible, head and hair emerging over the circular badge, no lettering.
- Native Android Views and lightweight ViewPropertyAnimator transitions.

## Architecture and platform behavior
- Native Kotlin/Android Views for predictable performance on the Galaxy A31.
- FloatingCompanionService owns the WindowManager overlay and quick composer.
- Quick Settings tile toggles summon/hide; Android 12 users add it manually through Quick Settings Edit. Launcher long-press shortcut is an additional route.
- Floating overlay requires Android's Display over other apps permission. Persistent notification includes a Turn off action.

## Existing features and limits
- Shared chat history, notes, tasks, reminders and local memory remain.
- Cloud AI requires the user's own provider key. Offline responses are basic local fallback, not a full offline language model.
- Quick attachments support images and text; PDFs are first-page-only and videos use an opening frame. Full video and arbitrary Office document understanding are not implemented.
- Camera, voice recognition, and screen understanding depend on Android permissions and supported provider/device capabilities.

## Release validation
- Version 0.9.5 / versionCode 18.
- CI build must succeed and produce a signed release APK.
- Physical Samsung Galaxy A31 / Android 12 testing is still required for overlay placement, keyboard resizing, Quick Settings behavior, icon masking, animation memory use, voice/camera permissions and performance.
- CI success is not a substitute for physical-device testing.
