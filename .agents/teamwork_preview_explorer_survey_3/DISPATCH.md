## 2026-09-11T12:54:12Z
Task: Conduct an architectural survey focusing on:
1. Deterministic State Machine & Policy Engine requirements (`YouTubePolicyEngine`):
   - States: Verifying, AskUser, Allowed, Blocked, ShortsBlocked.
   - Core invariants: Default to 'Verifying' on any video identity change; never temporarily allow unknown content.
   - Shorts evaluation: Blocked independently of channel policy.
   - Ad transitions: Retain previous verified channel's policy instead of evaluating ad's metadata.
   - Channel classification: PRODUCTIVE vs DISTRACTING.
2. Existing test infrastructure and test suites in the repository:
   - What test frameworks and libraries are configured (JUnit 4/5, MockK, Robolectric, Turbine, Coroutines Test)?
   - Where are existing unit tests and instrumentation tests located?
   - How to write comprehensive unit tests for `YouTubePolicyEngine` and `YouTubeChannelEntity` Room DAO.
