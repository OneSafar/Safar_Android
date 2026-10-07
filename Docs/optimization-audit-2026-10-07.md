# Android optimization — 7 October 2026

The cross-project findings, validation and staging plan are in `../../SAFAR/docs/optimization-audit-2026-10-07.md`.

Android changes cancel live-room collectors on leaving, remove duplicate room joins, close HTTP 429 bodies before retry, ensure maintenance response cleanup, and extend maintenance polling to randomized 60–90 seconds. Offline live-session fallback polling remains 25–55 seconds because it recovers status when the socket is unavailable.

Validation: production-debug compilation and 36 relevant unit tests across six suites passed. No APK/AAB release or deployment was performed.
