# Miko companion upgrade — product and implementation plan

This document records the agreed Miko experience and separates working code from work that still needs implementation or real animation assets.

## Character and performance
- Keep Miko as the same 2D chibi character: silver/white hair, bright blue eyes, blue bow/flower detail, navy and ice-blue outfit.
- Personality: playful, affectionate, gently tsundere, protective and emotionally attentive; avoid repetitive catchphrases.
- Keep rendering light for mid-range Android phones, including the Samsung Galaxy A31. Do not require a local GPU or a paid AI service.
- Miko's visible character is rendered from the user's three supplied transparent WebP mouth-state images; no alternative avatar is substituted.

## Floating companion and two chat surfaces
- Miko remains a draggable floating overlay above other apps, with explicit Android overlay permission and a persistent foreground-service notification.
- Double-tapping Miko opens a compact quick-chat typing bar, not a full-screen conversation window.
- A reply appears in a separate coloured speech bubble positioned near Miko without covering her face. Tapping the bubble opens the dedicated chat screen; short replies may dismiss automatically.
- The dedicated in-app chat keeps the full transcript, settings, and longer conversations.
- Quick-chat and full-chat must use the same conversation store and AI engine so a conversation can continue seamlessly between surfaces.
- Attachments are part of the target experience: photo, video, document/file, camera and voice. Use Android system pickers and explicit user consent. Never silently inspect screen content or files.

## Conversation and Japanese support
- Understand English, kana/kanji, and Japanese written in Romaji (Latin letters), including common casual forms such as ohayo/ohayou, arigatou, genki, daijoubu and oyasumi.
- Default reply style is Natural bilingual: mostly English with occasional simple Japanese.
- User-selectable styles: Natural bilingual, Japanese/Romaji, and English.
- Keep normal companion replies to about 1–2 short sentences. If Romaji is ambiguous, ask a short clarification rather than guessing.
- A small offline phrase fallback handles common greetings and simple expressions. It is not a local LLM; full open-ended conversation still requires a configured cloud provider.

## Character visual scope — fixed to three supplied images
- Miko uses only the user's three supplied full-character mouth-state images: closed, half-open and open.
- Do not add LoRA-generated animation clips, animated WebP mood libraries, extra facial expressions, or replacement character artwork unless the user explicitly changes this instruction.
- Closed mouth is used while idle/listening; while TTS speaks, cycle closed → half-open → open → half-open at approximately 115 ms per frame; return to closed on completion or error.
- Keep transparency and consistent image dimensions. Decode to a bounded 640×900 maximum with alpha preserved for Galaxy A31 memory limits.
- The same three mouth images are used by the home preview, floating overlay, quick-chat speech, and full-chat TTS when the overlay is running.
- The three files are imported once through the home-screen ZIP importer and stored privately as `miko_mouth_closed.webp`, `miko_mouth_half.webp`, and `miko_mouth_open.webp`.

## Current implementation status on this branch
- Existing app has a native Android overlay service, the three-image Miko sprite view, double-tap handling, a local shared chat transcript, cloud-provider failover, screen capture with explicit consent, TTS, and a dedicated chat activity.
- Added selectable response styles and a Romaji-aware personality instruction for cloud AI.
- Added offline phrase responses for a small set of common Romaji expressions.
- Replaced the large floating chat panel with a compact quick-chat bar and a separate tappable reply bubble. The full chat activity remains available from the quick bar and bubble.
- The transcript remains shared between quick chat and the full chat; the full chat reloads it on entry/resume.
- The full chat can import one photo, video, PDF, document or text attachment through Android's system document picker, and can capture a camera photo through the installed camera app.
- Attachments are copied into app-private storage (50 MB maximum); image attachments are downscaled before vision requests, and small TXT/MD/CSV/DOCX files can be passed as text to a configured provider; PDF previews include up to the first three pages, and video previews use five sampled frames (no audio).
- Unsupported document/video formats are still saved and listed in the conversation, but are not falsely presented as understood by the AI.
- The floating quick-chat bar offers Attach, Voice and Full chat shortcuts. Attach opens the full chat directly into the system picker; Voice opens it and starts speech recognition.
- Reply bubbles can be tapped to expand, swiped away, and configured to disappear after 5, 9, 15 seconds or remain until dismissed.
- Android 13 notification permission is requested before the floating service is started; target SDK was raised to 33 to clear the expired-target lint failure.

## Not yet implemented / needs assets
- Full multi-page PDF and Office-format parsing, video audio/transcription support, and multi-attachment messages. PDF previews currently show up to three pages; video previews show five frames without audio.
- Camera capture/preview flow.
- No additional visual assets are planned unless the user explicitly requests them.
- The actual transparent WebP animation files. They must be supplied/generated before the final Miko animations can appear in the app.
- Bubble placement collision avoidance, user-configurable dismissal timeout, and richer queued expression/animation state management.
- End-to-end device testing and performance tuning on a physical Galaxy A31.

## Safety and privacy defaults
- Overlay, microphone, screen capture, camera and file access must be user-approved.
- Screen capture remains one-time and opt-in.
- Keep API keys encrypted using Android Keystore. Do not embed keys in source control.
- Never claim that a phone action, attachment inspection or screen analysis succeeded unless it actually did.


## Three-image WebP lip-sync (November 2026)
- The user's three supplied mouth states are imported from the prepared ZIP using **Import Miko mouth images** on the home screen.
- The import accepts the exact files `miko_mouth_closed.webp`, `miko_mouth_half.webp`, and `miko_mouth_open.webp`, and stores them in app-private storage.
- Only these three images are used; no animated WebP expression library or alternative avatar is included.
- While Android TextToSpeech is speaking, the floating sprite cycles closed → half-open → open → half-open at a 115 ms interval, then returns to closed when speech ends or errors.
- Full-chat TTS notifies the floating service so the same mouth frames animate during full-chat replies when the floating companion is active.
- This is lightweight three-frame mouth-flap lip sync, not phoneme-accurate viseme matching or audio-amplitude analysis.
