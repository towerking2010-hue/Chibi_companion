# Miko — master product roadmap

## Product direction
Miko is a free-first, privacy-conscious personal assistant and animated companion. Android is the first client; the long-term architecture should allow laptop and TV clients to connect to the same identity, preferences and (with explicit opt-in) synchronized memory.

The product should be useful without a character model, and the model must remain a replaceable presentation layer. Do not block assistant capabilities on final animated model integration.

## Product principles
- No paid API calls without explicit user approval. Default spending limit: zero.
- Offline-first for phone actions, local tasks, settings and simple conversation.
- Be honest when a feature needs cloud AI, internet, a provider key, a permission or a supported device.
- Confirm consequential actions such as sending messages, placing calls, purchases, deleting data and executing automation.
- Ask for only the permissions needed for a feature, when they are needed.
- Protect keys and personal memory; provide review, export and deletion controls.
- Optimize for mid-range Android hardware such as the Samsung Galaxy A31.
- Keep the character renderer modular so the owner can integrate the final Live2D/3D model later.

## Delivery phases

### Phase 1 — dependable Android foundation
- Stable chat and non-repeating offline fallback.
- Structured skill/action routing with truthful success and failure responses.
- App launching, search, maps, dialer/SMS/email drafts, alarms, timers, calendar drafts and settings shortcuts.
- Local tasks, notes, reminders and daily planning.
- Encrypted user-managed memory and conversation history controls.
- Provider management, connection tests, cost safeguards and provider fallback.
- Permission dashboard, privacy controls, error handling and low-resource mode.

### Phase 2 — companion intelligence
- Context-aware conversation and conversation summaries.
- Memory review, edit, pin, forget and export workflows.
- Personality and emotional-state engine with consistent behaviour across providers.
- User-controlled proactive mode, quiet hours and scheduled check-ins.
- Better offline intent understanding without pretending a scripted fallback is a general LLM.

### Phase 3 — voice assistant
- Push-to-talk reliability, interruption and speech controls.
- Wake-word prototype with explicit opt-in, visible microphone state and battery testing.
- Android default-assistant / VoiceInteractionService investigation and integration where supported.
- Multilingual speech and text support subject to installed speech engines.

### Phase 4 — floating companion and model integration
- Draggable overlay, docking, minimize/hide/stop controls and safe positioning.
- Touch reactions and animation state API.
- Final owner-supplied Live2D/3D model integration, expressions, physics and lip sync.
- Performance profiles for mid-range devices.

### Phase 5 — vision and context
- User-approved camera and screen capture.
- OCR, image understanding and visual Q&A through a configured vision provider or compatible local model.
- AR placement only on supported devices and with explicit camera permissions.
- Clear capture indicators and no background camera use.

### Phase 6 — productivity and learning
- Calendar and task integrations, habits, routines, focus timer and daily briefings.
- Notes, document summaries, study tools, language practice and quizzes.
- Notifications and proactive suggestions with quiet hours.

### Phase 7 — laptop companion
- Desktop client with shared account/device pairing.
- Voice and text assistant, local app/file workflows with explicit access.
- Secure phone-to-laptop handoff and device presence.
- Platform-specific permissions; no arbitrary remote control by default.

### Phase 8 — TV companion
- TV client for supported Android TV / Google TV platforms.
- Voice-first interaction, media search and playback controls through supported APIs.
- Phone-as-remote pairing and shared household controls only with explicit authorization.
- Other TV brands require separate compatibility work; do not promise universal control.

### Phase 9 — advanced ecosystem
- Skill/plugin SDK, automation builder and conditional routines.
- Multi-device memory sync with encryption and user-controlled scope.
- Local model manager, specialist agents and diagnostics.
- Games, achievements, customization and companion activities.

## Current implementation checkpoint
The Android rebuild branch already has native chat, configurable cloud providers, encrypted on-device memory, speech recognition and TTS, a floating service, basic phone intents, opt-in screen capture flow, and the reserved model viewport. It does not yet have a local general-purpose LLM, system-wide wake word, default assistant integration, unrestricted app control, or laptop/TV clients.

The first new product increment is a local task list. Tasks are stored on-device and can be added, completed, deleted and cleared. This is a foundation feature, not a claim that full calendar/reminder automation is complete.

## Release gates
- Build and sign APK in CI.
- Inspect build output and artifact.
- Install and test on a physical Galaxy A31.
- Verify chat, tasks, voice, overlay start/stop, app actions, permission flows and battery behaviour.
- Never equate CI success with device verification.
- Keep development on `miko/rebuild-foundation`; do not merge the draft PR without owner approval.
