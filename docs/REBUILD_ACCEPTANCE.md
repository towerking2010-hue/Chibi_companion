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
