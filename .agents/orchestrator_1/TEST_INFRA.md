# E2E Test Infra: YouTube Guard

## Test Philosophy
- Opaque-box, requirement-driven. No dependency on internal implementation quirks.
- Methodology: Category-Partition + Boundary Value Analysis + Pairwise Interaction Testing + Real-World Workload Scenarios.
- Dual Track verification: E2E testing ensures all user requirements (R1-R4) and acceptance criteria are completely satisfied.

## Feature Inventory
| # | Feature | Source (requirement) | Tier 1 | Tier 2 | Tier 3 |
|---|---------|---------------------|:------:|:------:|:------:|
| F1 | Room Persistence & Classification Retrieval | ORIGINAL_REQUEST §R3, §AC System Integration | 5 | 5 | ✓ |
| F2 | Fail-Closed Transition to Verifying on Video Change | ORIGINAL_REQUEST §R2, §AC Detection & State Machine | 5 | 5 | ✓ |
| F3 | Independent Shorts Blocking | ORIGINAL_REQUEST §R1, §R2, §AC Detection & State Machine | 5 | 5 | ✓ |
| F4 | Ad Policy Retention | ORIGINAL_REQUEST §R1, §R2, §AC Detection & State Machine | 5 | 5 | ✓ |
| F5 | User Classification Flow via Overlay | ORIGINAL_REQUEST §R4, §AC System Integration | 5 | 5 | ✓ |
| F6 | Accessibility Service & Overlay Lifecycle | ORIGINAL_REQUEST §R1, §R4, §AC System Integration | 5 | 5 | ✓ |

## Test Architecture
- Test Runner: Gradle unit test runner (`./gradlew testQaDebugUnitTest`) running with JVM 17/21.
- Test Location: `app/src/test/java/com/safarparmar/app/feature/youtube/e2e/`
- Test Frameworks: JUnit 4, Kotlinx Coroutines Test, Turbine, MockK.
- Pass/Fail Semantics: Exit code 0, all assertions pass, 0 regressions in existing tests.

## Real-World Application Scenarios (Tier 4)
| # | Scenario | Features Exercised | Complexity |
|---|----------|--------------------|------------|
| 1 | Cold launch into educational study session (Productive channel allowed, no prompts) | F1, F2 | Medium |
| 2 | Browsing feed and encountering distracting video (Blocked overlay displayed, touch suppressed) | F1, F2, F5 | Medium |
| 3 | Encountering Shorts feed while watching productive channel (Immediate ShortsBlocked) | F2, F3 | High |
| 4 | Mid-roll ad playing during productive tutorial (Ad allowed without classifying advertiser) | F2, F4 | High |
| 5 | Mid-roll ad playing during distracting video (Blocked state retained) | F2, F4 | High |
| 6 | Discovery of new unclassified channel -> User clicks 'Productive' in Compose overlay -> Transitions to Allowed & persists in Room | F1, F2, F5 | High |
| 7 | Consecutive video autoplay across mixed channel policies (Productive -> Distracting -> Unclassified) | F1, F2, F5 | High |

## Coverage Thresholds
- Tier 1: ≥5 per feature (Total ≥ 30)
- Tier 2: ≥5 per feature (Total ≥ 30)
- Tier 3: Pairwise coverage of major feature interactions (Total ≥ 10)
- Tier 4: ≥7 realistic application scenarios
