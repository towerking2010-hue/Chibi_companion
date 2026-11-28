# Miko 0.9.0 — companion experience redesign

## Design intent
Miko is the primary experience, not a utility dashboard with a character placed inside it. The home screen is character-first, with a large owner-supplied sprite viewport, one obvious summon/hide action, and a small grid of destinations. Help and confirmations use compact bottom sheets rather than large blocking alert dialogs.

## Visual system
- Warm pearl/lavender canvas with plum text and restrained pink/blue accents.
- Consistent rounded surfaces, spacing, typography and status/navigation bar contrast.
- Home dashboard prioritises Miko and the summon action.
- Chat, tasks, notes and settings share the same palette and surface language.
- Contextual help and destructive confirmations use MikoSheet, a lightweight native bottom-sheet component.
- No remote fonts, web content, or heavy animation framework is required.

## Architecture direction
The app remains native Kotlin/Android Views for predictable performance on the Galaxy A31 and avoids adding a large runtime dependency solely for presentation.

### Presence domain
`MikoPresenceStore` is the single state gateway for whether the floating companion is visible. The overlay service owns the actual WindowManager view; the store persists the state and refreshes the Quick Settings tile. Launcher, home and tile surfaces read the same state instead of maintaining separate flags.

### UI components
- `CompanionUi`: shared palette, typography, surfaces, buttons and system-bar treatment.
- `MikoSheet`: reusable compact contextual sheet for help and confirmations.
- `MikoSpriteView`: isolated owner-supplied three-frame mouth image renderer.
- Activities remain feature entry points; the foreground service remains responsible for the persistent overlay.

### Existing data and privacy boundaries retained
- Chat history, notes, tasks and reminders remain on-device.
- API credentials and companion memory continue to use Android Keystore-backed encryption.
- Cloud AI remains opt-in and depends on a user-supplied provider key; no paid calls are enabled by this redesign.
- The Quick Settings tile and launcher long-press shortcut are retained.
- The floating overlay continues to require the user's Android overlay permission.

## Performance guardrails
- Keep native views and Canvas/sprite rendering; avoid WebView-based screens.
- Do not introduce continuous background polling for UI state.
- Keep image decoding and AI/network work off the main thread.
- Keep the sprite renderer replaceable; do not couple chat or memory to the avatar implementation.
- Avoid introducing heavyweight dependencies without a measured user-visible benefit.

## Release validation checklist
1. Build debug and permanently signed release APKs in CI.
2. Confirm version 0.9.0 / versionCode 13 and signing continuity with 0.8.1.
3. Install over 0.8.1 on a Galaxy A31 (Android 12 / One UI).
4. Verify home layout and sprite sizing in portrait and landscape.
5. Verify bottom-sheet layout, back dismissal and screen-reader labels.
6. Verify launcher shortcut, Quick Settings summon/hide, persistent notification stop action and state synchronisation.
7. Verify double-tap quick chat stays open after multiple sends; full chat uses the same history.
8. Verify notes, tasks, provider management, voice, camera and attachment flows.
9. Check keyboard resize, status/navigation bars, permission return paths, process recreation and battery use.

CI success is not a substitute for physical-device testing. Any feature not exercised on the Galaxy A31 must remain marked as unverified.
