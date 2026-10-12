# BRIEFING — 2026-09-11T13:25:00Z

## Mission
Specify exact code, imports, and state machine transitions for PolicyState, VideoIdentity, and YouTubePolicyEngine adhering strictly to the 3 core invariants.

## 🔒 My Identity
- Archetype: explorer
- Roles: Teamwork explorer
- Working directory: /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_m1_2
- Original parent: 343d0e8f-a734-424a-a2ff-ed97e9e03f9f
- Milestone: Milestone 1 — Deterministic State Machine & Policy Engine

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Invariant 1: Default to `Verifying` immediately on ANY video identity change (fail-closed before DB lookup)
- Invariant 2: Shorts evaluated and blocked independently of channel policy (never AskUser for Shorts)
- Invariant 3: Ad transitions retain previous verified channel's policy instead of evaluating ad metadata
- User classification flow (`onUserClassified`) saves to repository and updates active state

## Current Parent
- Conversation ID: 343d0e8f-a734-424a-a2ff-ed97e9e03f9f
- Updated: 2026-09-11T13:04:02Z

## Investigation State
- **Explored paths**:
  - `ORIGINAL_REQUEST.md`: R1-R4 requirements, core invariants 1-3.
  - `orchestrator_1/PROJECT.md`: contracts and package structure (`com.safarparmar.app.feature.youtube.policy`).
  - Codebase build configuration (`app/build.gradle.kts`, `gradle/libs.versions.toml`).
  - Survey 2 Gen 2 and Survey 3 Gen 2 architectural blueprints.
  - Explorer 1 (Room persistence) and Explorer 3 (Unit test suite) contracts and progress.
- **Key findings**:
  - Initial StateFlow state is `PolicyState.Verifying(null)`.
  - Immediate synchronous assignment `_state.value = PolicyState.Verifying(identity)` on ANY video identity change enforces Invariant 1 (fail-closed before Room DB lookup).
  - Shorts handling branches directly to `PolicyState.ShortsBlocked(identity, BlockReason.SHORTS)` bypassing DB lookup (Invariant 2).
  - Ad handling retains `lastVerifiedPolicy` without querying advertiser metadata or asking user (Invariant 3).
  - Cold start ad without prior verified channel remains fail-closed in `PolicyState.Verifying(identity)`.
  - `onUserClassified` updates in-memory active state and persists to Room via `repository.saveChannelPolicy`.
  - Secondary constructor with `CoroutineScope` enables deterministic unit testing with `TestScope` and `MainDispatcherRule`.
- **Unexplored areas**: None for M1 state machine scope.

## Key Decisions Made
- Synchronous emission of `PolicyState.Verifying(identity)` followed by asynchronous resolution in `scope.launch` guarantees Turbine and UI observe `Verifying` before resolved states.
- Handled empty channel handle by falling back to `channelName`, and staying in `Verifying` if both are blank.
- Implemented `isSameVideo` in `VideoIdentity` to prevent duplicate UI re-evaluation while correctly detecting ad start/end.

## Artifact Index
- DISPATCH.md — incoming dispatch instructions
- BRIEFING.md — persistent state memory
- progress.md — liveness heartbeat
- handoff.md — hard handoff specification report for Policy Engine
