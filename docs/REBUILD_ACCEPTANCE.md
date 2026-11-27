# Miko rebuild — acceptance criteria

This branch is a product rebuild, not a branding-only patch.

## Character identity
- Name: Miko.
- Cute chibi girl with long silver-white hair, blue eyes, and a blue/lavender casual outfit.
- Launcher mark: head-only character icon.
- In-app companion: full readable character, not a tiny sticker.
- Current renderer is resolution-independent Android Canvas vector drawing. It is not a production illustration, Live2D rig, or 3D model.

## Interaction and animation
- Idle breathing, blinking, gentle floating/bobbing and subtle sway.
- Dragging follows the finger and leans in the direction of movement.
- Pickup, release, tap, double-tap and long-press reactions.
- Talking state follows Android TextToSpeech utterance callbacks.
- Current animation is whole-character transforms plus procedural face states; hair is not independently physics-rigged.

## App behavior
- Home, chat and floating companion now use a dedicated model viewport placeholder (`ModelSlotView`). The old procedural character renderer is not displayed. The viewport is intended for the owner's future animated PNG/Live2D/3D asset.
- Voice input uses Android SpeechRecognizer; spoken replies use installed Android TextToSpeech voices.
- Cloud AI requires a user-configured provider key; no paid service is enabled automatically.
- Offline fallback is scripted, not an offline LLM.
- Long-term memory is explicitly user-managed and encrypted with Android Keystore.
- Implemented phone intents include app launching, web search, time/date responses, Maps navigation, calendar event drafts, email drafts, dialer/SMS drafts, alarms/timers and folder picker. Consequential actions remain user-confirmed. Consequential actions remain user-confirmed.
- Screen understanding is an opt-in capture flow and requires a configured vision-capable provider.

## Not yet implemented / not to be advertised as complete
- System-wide always-listening wake word.
- Full Android VoiceInteractionService / default assistant integration.
- Broad device settings control or unrestricted automation.
- A local general-purpose LLM.
- Production-quality illustrated character asset, Live2D physics rig, 3D model, or professional voice model.

- Local notes are encrypted on-device and support create, edit, search and delete.
- Task and notes screens are native Android UI; notes are not synchronized to cloud services.

## Device and release gates
- Target the Galaxy A31 class of hardware.
- CI success is a compile/package check only, not proof of device behavior.
- Do not call a release complete until the APK artifact is checked and the user verifies install, launch, overlay, chat, voice and touch behavior on-device.
- Keep work on this branch; do not merge into main without user approval.

## Implementation status — 2026-11-26 (model deferred build)
- The shared renderer remains procedural Canvas vector artwork. The outfit was refined toward the approved reference (white blouse, navy pinafore, lavender bow, pleated skirt and flower boots); a five-petal lavender hair clip and additional flowing hair strands were added. The face now includes layered sapphire irises, lashes, star / heart eyes, tears, mood-specific brows and speech mouth movement.
- These are vector refinements, not integration of the supplied flattened reference image. The two long back-hair locks now have independent, low-amplitude sway transforms; front hair remains a combined layer and there is no bone/mesh physics rig.
- Happy / excited faces should remain open-eyed except for brief natural blinks; sleepy is the only intentionally closed-eye resting state.
- The app is a functional foundation, not yet the fully featured personal assistant described in the product vision.


## Model deferred phase
- Owner requested all app work to continue while the animated character model is deferred to the final integration step.
- Home, chat and floating companion surfaces use the reusable `ModelSlotView` placeholder. No geometric procedural avatar or generated reference image is included in this build.
- Chat voice recognition now submits the recognized phrase automatically; speech replies still use the device's installed Android TTS voice.
- Home screen includes direct quick-action prompts for alarms, timers, web search and launching installed apps.
- Offline phone-intent handling includes local time/date, Maps navigation, calendar event drafts and email drafts in addition to the existing intents.
- This is not yet equivalent to Google Assistant or Siri: no system-wide hotword, default VoiceInteractionService, unrestricted settings control, offline general-purpose LLM, or guaranteed third-party app actions. Cloud AI still requires a user-provided provider key.
