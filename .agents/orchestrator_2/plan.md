# Plan: YouTube Channel Blocker 15-Phase Fixes Plan

## Goal
Achieve reliable, fail-closed blocking across normal videos, suggested videos, autoplay, fullscreen, channel pages, ads, and Shorts.

## Requirements Breakdown (15 Phases grouped into 5 Core Pillars)
- **Pillar 1 / M1: Fail-Closed Overlay & State Machine (Phases 1, 6, 13)**
  - Phase 1: `OverlayState.Verifying` overlay via `TYPE_ACCESSIBILITY_OVERLAY`, touch consuming.
  - Phase 6: Fullscreen & autoplay transitions enter Verifying.
  - Phase 13: Edge cases (rapid video switching, cold start).
- **Pillar 2 / M2: Canonical Channel Identity & Fingerprinting (Phases 2, 5, 14)**
  - Phase 2: `ChannelIdentity` model (real `@handle` or `displayName`), canonical keys (`handle:<handle>` or `name:<normalized_display_name>`).
  - Phase 5: `VideoFingerprint` (mediaId prioritized, or normalized title + channel key).
  - Phase 14: Room entity & repository updates without fake handles.
- **Pillar 3 / M3: Structural Subtree Parsing (Phases 3, 7)**
  - Phase 3: `YouTubeUiParser` parsing specific subtrees (watch metadata, player, channel header, shorts player).
  - Phase 7: Shorts detection via structural reel/player signals + bottom nav click interception (`TYPE_VIEW_CLICKED`).
- **Pillar 4 / M4: Unified Detection / Fusion Layer & Event Stream (Phases 4, 8, 11, 12, 15)**
  - Phase 4: `YouTubeContentDetector` fusing Accessibility & MediaSession with `DetectionConfidence`.
  - Phase 8: Event serialization channel before PolicyEngine.
  - Phase 11: Ad policy retention & ad video switch handling.
  - Phase 12: Stale lookup / cancellation protection via generation tokens.
  - Phase 15: Suggested video click interception & channel page handling.
- **Pillar 5 / M5: Lifecycle, Robustness & E2E Verification (Phases 9, 10)**
  - Phase 9: Foreground detection via global window state changes; dismiss overlay & cancel detection on exit.
  - Phase 10: `NotificationListener` reconnect lifecycle & `MediaController.Callback` leak fixes.
  - Final Verification: Run comprehensive unit and E2E test suites with 100% pass rate.

## Execution Sequence
1. Phase 0: Survey Codebase State (Spawn 3 Explorers)
2. Milestone Decomposition & Architecture Plan (`PROJECT.md`)
3. Milestone-by-Milestone Implementation & Verification (Explorer -> Worker -> Reviewers -> Challengers -> Forensic Auditor)
4. Comprehensive Verification & Handoff to Sentinel
