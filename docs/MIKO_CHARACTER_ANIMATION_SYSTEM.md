# Miko character and animation system

Status: implementation specification and original art direction. This document does not claim the sprite artwork is integrated into the APK yet.

## Character lock

- Identity: Miko, cute chibi girl.
- Hair: silver/white with cool lavender shadows.
- Eyes: bright blue; signature star-shaped pupils/highlights for excitement.
- Outfit: casual pastel lavender-and-white hoodie outfit.
- Silhouette: oversized expressive head, compact readable body.
- Keep the launcher mark head-only; this document describes the full in-app companion.

## Expression set

| State | Face and visual treatment | Motion |
|---|---|---|
| neutral | relaxed blue eyes, small smile | breathing, occasional blink, subtle sway |
| happy | crescent/smiling eyes, warm blush | gentle bounce |
| excited | bright star pupils, open smile, sparkles | quick double bounce, tiny celebratory fists |
| angry | narrowed eyes with red glow/accents, sharp brows, tiny anger mark | short tense shake; brief aura, then settle |
| shy | averted gaze, blush, small mouth | fidget and slight lean |
| embarrassed | squeezed eyes, deep blush | quick recoil then hide face |
| sleepy | half-lidded eyes, yawn, optional sleep marks | slow droop and gentle breathing |
| sad | lowered brows, watery eyes | shoulders/head lower |
| crying | tears and trembling mouth | small tremble; avoid excessive looping |
| surprised | wide eyes and small O mouth | quick recoil |
| teasing | one-sided grin and wink | head tilt |
| affectionate | warm eyes, blush, heart accents | gentle lean/bounce |
| talking | mouth movement driven by speech callbacks | lightweight mouth-open cycle |

## Animation architecture

1. **Emotion resolver:** turn explicit interaction context and assistant response intent into a small, bounded set of named states. Avoid keyword-only emotion guesses as the final design; keep a safe neutral fallback.
2. **Character state controller:** own current/target expression, animation trigger, transition duration, and speech state. Cancel or replace stale one-shot reactions so they cannot fight each other.
3. **Renderer:** render authored, consistent character artwork. Prefer layered 2D parts with reusable eye/mouth/brow variants and lightweight transforms; use frame animation for special effects. Keep a low-cost fallback for smaller overlay sizes.
4. **Voice sync:** use Android TTS utterance callbacks for start/end and a throttled mouth-open cycle while speaking. Do not claim phoneme-accurate lip sync unless phoneme/viseme data is actually available.
5. **Accessibility and performance:** respect reduced motion where available, avoid continuous expensive work when not visible, and keep animation frame rates modest on Galaxy A31-class hardware.

## Transition rules

- Idle is the default and resumes after one-shot reactions.
- Talking may overlay a compatible expression; it must not erase the emotion.
- Excited/angry/surprised reactions are short one-shots, then return to the prior or neutral state.
- Sleepy and sad are persistent states only when explicitly selected by the emotion resolver.
- State changes should crossfade or interpolate where possible; never abruptly swap unrelated body proportions.

## Original art direction

The first original expression-sheet concept uses a consistent silver-haired, blue-eyed Miko in a lavender/white casual outfit. It is a visual source direction, not a production sprite atlas: individual frames still need transparent cleanup, consistent cell bounds, slicing, and device-scale validation before APK integration.

## Release gates

- All expression states render without clipping at both home-screen and floating-overlay sizes.
- Eye styles are visibly distinct: blue starry excitement and unmistakably angry narrowed/red eyes.
- Talking animation starts/stops with TTS callbacks and returns to the previous expression.
- Transitions do not flicker, leak views, or continue unnecessary work when the companion is hidden.
- Build succeeds and the user verifies install, launch, overlay, chat, and voice on the Galaxy A31.
