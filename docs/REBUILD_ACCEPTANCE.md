# Miko rebuild — acceptance criteria

This branch is a product rebuild, not another branding-only patch.

## Product identity
- Keep the name Miko.
- Character identity: very cute chibi girl, silver/white hair, blue eyes, casual pastel outfit.
- App launcher mark: head-only character mark with sparkling blue eyes; no full-body logo.
- In-app companion: full, readable animated chibi, never a tiny sticker by default.

## Experience
- Character-first home screen with a clean, compact control surface.
- Floating companion must be readable at normal phone scale, draggable, and open a usable chat panel.
- Chat must expose connection state clearly: offline scripted fallback vs connected cloud model.
- Voice input/output must show permission and engine errors instead of silently failing.
- Settings must only expose capabilities that are actually implemented.

## Functionality truth
- No claim of a full offline LLM unless one is bundled and tested.
- No claim of always-on wake word, continuous screen understanding, or unrestricted phone control.
- Phone actions must be explicit, permission-aware, and confirm consequential actions.
- Provider failures must be surfaced in the conversation and settings; never disguise them as a normal AI answer.

## Device and release gates
- Build for the Galaxy A31 class of hardware; avoid heavyweight rendering and unnecessary background work.
- CI success is a compile/package check only, not proof of device behavior.
- Do not call a release complete until APK artifact integrity is checked and the user has verified install, launch, floating overlay, chat, voice, and each advertised action on-device.
- Keep this work on a separate branch until it is reviewed; do not force-push or overwrite the user's main branch.


## Current implementation update — 2026-11-25
- Home now includes a direct Settings entry point.
- Chat is a dedicated conversation screen with a persistent character panel, transcript, compact composer, microphone action, and Settings shortcut.
- Settings is a separate screen for provider credentials, voice selection, local memory management, and proactive mode.
- Chat TTS mouth motion is driven by utterance callbacks while the selected emotion remains visible.
- Floating Miko keeps the response emotion instead of resetting to neutral on a timer; its chat panel links to Settings.
- The original procedural Canvas renderer remains as a fallback for missing/unmapped assets.
- The bundled `miko_atlas.webp` is a 2×2 sheet containing four complete chibi poses. The renderer now slices it as 2×2; mapping it as 4×3 caused the half-face crop seen in the first device build.
- The sprite renderer uses lightweight breathing, bobbing, and tilt transforms and clips drawing to its own view bounds; this is an animated 2D sprite system, not Live2D/3D or independently rigged body/face parts.
- Latest CI must pass before considering this code push build-verified. Device verification remains with the user.


## Sprite renderer integration — 2026-11-25
- Added an image-backed `MikoSpriteView` renderer and switched the home, chat, and floating companion views to use it.
- The committed `miko_atlas.webp` is a 128×128 2×2 core sheet (four poses). The renderer now uses the actual 2×2 cell geometry and maps related emotions to the closest available pose.
- Drawing is explicitly clipped to the view bounds to prevent the avatar from painting over neighboring controls.
- This is a lightweight animated 2D sprite implementation, not a Live2D/3D rig. Device verification is still required.
