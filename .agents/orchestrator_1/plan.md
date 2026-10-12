# Orchestration Plan: YouTube Content Blocker

## Objective
Design, implement, and verify a YouTube content blocker Android component using AccessibilityService, MediaSessionManager, Room database, deterministic YouTubePolicyEngine, and Compose TYPE_ACCESSIBILITY_OVERLAY.

## Phases
1. **Phase 0: Survey & Scope Mapping**
   - Dispatch 3 parallel Explorers:
     - Explorer 1: Project structure, Gradle modules, dependencies, Room DB integration, Compose setup.
     - Explorer 2: AccessibilityService architecture, manifests, existing background/overlay services, permissions.
     - Explorer 3: Policy engine patterns, test harness, Robolectric/Android unit test infrastructure.
   - Synthesize findings into `PROJECT.md` with Feature Inventory and Architecture.
2. **Phase 1: Dual Track Setup**
   - E2E Testing Track: Launch E2E test planner/writers to establish test suites (Tiers 1-4).
   - Implementation Track: Milestone decomposition based on module boundaries:
     - Milestone 1: Room persistence & Channel classification entity/DAO (`YouTubeChannelEntity`).
     - Milestone 2: Deterministic State Machine & Policy Engine (`YouTubePolicyEngine`) + Unit tests.
     - Milestone 3: Core Detection Services (`YouTubeGuardService` AccessibilityService + `YouTubeNotificationListener` MediaSessionManager).
     - Milestone 4: Compose Blocking Overlay (`TYPE_ACCESSIBILITY_OVERLAY`).
3. **Phase 2: Milestone Execution & Verification Loop**
   - Explorer -> Worker -> Reviewer -> Challenger -> Auditor -> Gate check.
4. **Phase 3: E2E Integration & Verification**
   - Verification against 100% E2E tests and Acceptance Criteria.
   - Adversarial coverage hardening.
5. **Phase 4: Final Synthesis & Reporting**
   - Final audit, documentation, and reporting to parent.
