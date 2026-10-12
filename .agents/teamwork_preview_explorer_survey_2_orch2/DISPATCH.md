# Task Assignment: Survey Explorer 2 (orchestrator_2)

## Working Directory
`/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_2_orch2`

## Scope
Pillars 3 & 4 (Phases 3, 4, 7, 8, 11, 12, 15):
- Structural Subtree Parsing: `YouTubeUiParser`, locating watch metadata subtree, player subtree, channel header subtree, shorts player subtree (avoid flattening entire tree).
- Shorts Detection & Interception: structural reel/player signals + bottom nav click interception (`TYPE_VIEW_CLICKED`).
- Unified Detection / Fusion Layer: `YouTubeContentDetector` fusing Accessibility & MediaSession inputs into `CanonicalYouTubeContent` with `DetectionConfidence`.
- Event Serialization: Event channel/stream before updating `PolicyEngine`.
- Ad Handling: Retain verified policy during ad of known video; enter Verifying if playback fingerprint changes with an ad; never adopt advertiser info as channel identity.
- Stale Protection & Job Cancellation: Generation tokens, active job cancellation.
- Suggested videos / channel page interactions.

## Mandatory Reading
- `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md` (read the entire file, especially latest request `## 2026-09-11T15:25:10Z`)
- Current codebase under `app/src/main/java/com/safarparmar/app/feature/youtube/`

## Output Requirements
Produce `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_2_orch2/handoff.md` with:
- Current state of parser, detection service, ad handling, and event flow.
- Detailed gap analysis against Phases 3, 4, 7, 8, 11, 12, 15.
- Exact file paths, class/interface definitions, and concrete recommendations for implementation.

## 2026-09-11T15:27:46Z
You are Survey Explorer 2. Your working directory is /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_2_orch2.
Read your instructions in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_2_orch2/DISPATCH.md and read the mandatory specification in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md (especially the 15-phase fixes plan under ## 2026-09-11T15:25:10Z).

Your focus is Pillars 3 & 4 (Phases 3, 4, 7, 8, 11, 12, 15):
- Structural Subtree Parsing & Shorts Interception
- Unified Detection / Fusion Layer & Event Serialization
- Ad Policy Retention & Stale Lookup Protection

Explore the codebase under app/src/main/java/com/safarparmar/app/feature/youtube/.
Write your comprehensive analysis and handoff report to /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_2_orch2/handoff.md. When done, send a message to the caller with your summary and report path.

