# Miko rebuild — acceptance criteria

This branch is a product rebuild, not a branding-only patch.

## Character identity
- Name: Miko.
- Cute chibi girl with long silver-white hair, blue eyes, and a blue/lavender casual outfit.
- Launcher mark: head-only character icon.
- In-app companion: full readable character, not a tiny sticker.
- Character artwork must be image-backed; geometric Canvas placeholder art is not an acceptable final character.

## Interaction and animation
- Idle breathing, blinking, gentle floating/bobbing and subtle sway.
- Dragging follows the finger and leans in the direction of movement.
- Pickup, release, tap, double-tap and long-press reactions.
- Talking state follows Android TextToSpeech utterance callbacks.
- Be honest about the difference between sprite animation and independently rigged Live2D physics.

## App behavior
- Home, chat and floating companion use the same character renderer.
- Cloud AI requires a user-configured provider; no paid service is enabled automatically.
- Offline fallback is scripted, not an offline LLM.
- Phone actions remain permission-aware and require confirmation where consequential.

## Device and release gates
- Target the Galaxy A31 class of hardware.
- CI success is a compile/package check only, not proof of device behavior.
- Do not call a release complete until the APK artifact is checked and the user verifies install, launch, overlay, chat, voice and touch behavior on-device.
- Keep work on this branch; do not merge into main without user approval.

## Implementation status — 2026-11-25
- Home, chat and floating companion all use `MikoSpriteView`, which extends the shared `ChibiView`.
- The current committed `miko_atlas.webp` is a **2×2 sheet (four complete poses)**. The renderer must slice it as 2×2; treating it as 4×3 causes partial-face crops.
- The four-pose atlas is only a core expression set. It does **not** provide the full 12-expression range requested for the finished Miko.
- The renderer now draws the image atlas rather than the procedural geometric face. It applies lightweight breathing, bobbing and tilt transforms.
- Floating interaction states include pickup, drag lean, release settling, tap, double-tap and long-press. The home/chat character also responds to touch.
- This is an animated 2D sprite implementation, not Live2D/3D or independently simulated hair physics.
- CI and APK verification are still required for the latest commit. Physical device verification remains with the user.
