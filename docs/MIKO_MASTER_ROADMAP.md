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


## Owner-requested core experience — Miko lives on the phone

This is a first-class product requirement, not a decorative extra. The goal is to make Miko available across the phone without requiring the user to reopen the Miko app for every interaction.

### Intended interaction
1. **Summon:** The user invokes Miko from anywhere on the phone using a system-level gesture/assistant invocation where Android and the device support it. Miko's small, recognisable floating character appears above the current app.
2. **Open chat:** Double-tapping the floating character opens Miko's compact chat panel. The user can type or speak a request. Miko routes it to the appropriate supported phone skill or configured AI provider, and reports honestly when a permission, network connection, provider key or confirmation is needed.
3. **Keep Miko present:** Closing the chat panel closes only the conversation UI. The floating character remains visible and can be dragged/repositioned; it must not stop the companion service.
4. **Dismiss:** A second invocation of the chosen summon gesture (or a clearly accessible dismiss control/Quick Settings tile) hides/stops the floating character. The user should not have to open the app to dismiss her.
5. **Return:** Summoning again restores the floating character. A deliberate user setting controls whether Miko returns in the last position and whether the companion starts automatically after reboot.

### Invocation strategy and Android constraints
- Preferred path: integrate with Android's default digital-assistant role / VoiceInteractionService where the platform and Samsung firmware allow it. The user must explicitly choose Miko as the default assistant; system navigation gestures then remain controlled by Android/One UI.
- Provide a Quick Settings tile and persistent notification actions as reliable, user-controlled summon/dismiss fallbacks. These must work while another app is in the foreground.
- Investigate optional accessibility-based gesture support only if it is genuinely necessary, clearly disclosed, user-enabled in Android Settings, narrowly scoped, and compliant with Play/device policies. Never silently enable accessibility or intercept private input.
- A custom arbitrary global swipe gesture is not guaranteed to be available to ordinary Android apps. Do not promise Google Assistant's exact gesture on every device. Device model, Android version, One UI settings and assistant-role support must be tested on the owner's Galaxy A31.
- A wake word is a separate opt-in feature requiring visible microphone state, battery/privacy safeguards and background execution testing. Do not imply it is already implemented.

### Floating companion behaviour
- Double-tap avatar opens/closes chat; closing chat leaves avatar/service running.
- Dragging repositions the avatar; long-press or an explicit control may expose dismiss/settings actions.
- Avatar and chat overlays must respect system bars, keyboard, screen rotation and safe screen bounds.
- Companion service must have clear notification controls, permission onboarding, crash recovery and battery-conscious behaviour. Do not start a foreground service invisibly or bypass Android background-start restrictions.
- Summon/dismiss and chat state should be separate states: hidden, floating-idle, chat-open, listening, thinking, speaking and error/permission-needed.

### Acceptance criteria
- After one-time setup and permissions, user can summon Miko while using another app without opening Miko's launcher activity.
- Double-tap opens chat; chat close leaves avatar visible; invoking dismiss hides avatar; summon restores it.
- Supported phone actions work from this floating chat with the same confirmation and permission safeguards as in-app chat.
- No paid AI calls occur unless explicitly configured/approved; offline limitations remain clearly communicated.
- Test overlay, assistant invocation, Quick Settings tile, notification controls, battery use, reboot behaviour and gestures on the physical Galaxy A31 before calling this complete.

## Current implementation checkpoint

### Companion access implementation status (0.7.0 development increment)
- Registered a Quick Settings tile to toggle the floating companion without opening the launcher. First use still requires the user to grant Display over other apps; the tile opens that permission screen when missing.
- Registered an Android VoiceInteractionService and session that requests the floating companion when invoked. Main screen includes a request to set Miko as the default assistant. Android may reject the role if firmware or eligibility requirements are not met; this requires physical Galaxy A31 testing.
- Existing avatar double-tap still opens/closes chat; closing chat leaves the floating avatar running. The tile and assistant invocation control the companion service separately.
- The system navigation gesture remains owned by Android/One UI. Miko does not intercept gestures or enable Accessibility automatically.


The Android rebuild branch already has native chat, configurable cloud providers, encrypted on-device memory, speech recognition and TTS, a floating service, basic phone intents, opt-in screen capture flow, and the reserved model viewport. It does not yet have a local general-purpose LLM, system-wide wake word, default assistant integration, unrestricted app control, or laptop/TV clients.

The first new product increment is a local task list and encrypted notes space. Tasks are stored on-device and can be added, completed, deleted and cleared. Notes are encrypted with Android Keystore and support create, edit, search and delete. This is a foundation feature, not a claim that full calendar/reminder automation is complete.

## Release gates
- Build and sign APK in CI.
- Inspect build output and artifact.
- Install and test on a physical Galaxy A31.
- Verify chat, tasks, voice, overlay start/stop, app actions, permission flows and battery behaviour.
- Never equate CI success with device verification.
- Keep development on `miko/rebuild-foundation`; do not merge the draft PR without owner approval.
