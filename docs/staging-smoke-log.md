# Staging Smoke Cycle Log

Operational cycle log (Preflight + deploy gates + manual smoke result).

| UTC Time | Repo | Branch | RunId | HeadSha | Preflight | Run | Deploy | Verify | AgeMin | Decision | Manual Smoke | Evidence | Owner | Notes |
|----------|------|--------|-------|---------|-----------|-----|--------|--------|--------|----------|--------------|----------|-------|-------|
| 2026-02-17T23:07:44Z | V4N1LLA/Eunhye_Hymn | develop | 22119056042 | ee49a0efaa2838d697e9f6b09101a1c826e77d29 | FAIL | PASS | PASS | PASS | 10.5 | HOLD | BLOCKED | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22119056042 | codex | preflight failed |
| 2026-02-17T23:08:49Z | V4N1LLA/Eunhye_Hymn | develop | 22119056042 | ee49a0efaa2838d697e9f6b09101a1c826e77d29 | FAIL | PASS | PASS | PASS | 11.6 | HOLD | BLOCKED | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22119056042 | codex | preflight session expired; preflight failed |
| 2026-02-17T23:13:51Z | V4N1LLA/Eunhye_Hymn | develop | 22119056042 | ee49a0efaa2838d697e9f6b09101a1c826e77d29 | SKIPPED | PASS | PASS | PASS | 16.6 | CONDITIONAL_GO | PENDING | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22119056042 | codex |  |
| 2026-02-18T00:59:10Z | V4N1LLA/Eunhye_Hymn | develop | 22121533982 | c355fada82f6651982abf4adaeeb5b0d163fce28 | PASS | PASS | PASS | PASS | 24.5 | CONDITIONAL_GO | PENDING | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22121533982 | codex |  |
| 2026-02-18T02:51:37Z | V4N1LLA/Eunhye_Hymn | develop | 22121533982 | c355fada82f6651982abf4adaeeb5b0d163fce28 | PASS | PASS | PASS | PASS | 137 | HOLD | BLOCKED | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22121533982 | codex | run too old(age=137, max=120) |
| 2026-02-18T02:52:11Z | V4N1LLA/Eunhye_Hymn | develop | 22121533982 | c355fada82f6651982abf4adaeeb5b0d163fce28 | PASS | PASS | PASS | PASS | 137.5 | HOLD | BLOCKED | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22121533982 | codex | run too old(age=137.5, max=120) |
| 2026-02-18T02:52:47Z | V4N1LLA/Eunhye_Hymn | develop | 22121533982 | c355fada82f6651982abf4adaeeb5b0d163fce28 | PASS | PASS | PASS | PASS | 138.1 | HOLD | BLOCKED | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22121533982 | codex | run too old(age=138.1, max=120) |
| 2026-02-18T03:02:31Z | V4N1LLA/Eunhye_Hymn | develop | 22124607214 | c355fada82f6651982abf4adaeeb5b0d163fce28 | PASS | PASS | PASS | PASS | 8 | CONDITIONAL_GO | PENDING | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22124607214 | codex |  |
| 2026-02-18T03:03:05Z | V4N1LLA/Eunhye_Hymn | develop | 22124607214 | c355fada82f6651982abf4adaeeb5b0d163fce28 | PASS | PASS | PASS | PASS | 8.5 | CONDITIONAL_GO | PENDING | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22124607214 | codex |  |
| 2026-02-18T03:03:39Z | V4N1LLA/Eunhye_Hymn | develop | 22124607214 | c355fada82f6651982abf4adaeeb5b0d163fce28 | PASS | PASS | PASS | PASS | 9.1 | CONDITIONAL_GO | PENDING | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22124607214 | codex |  |
| 2026-02-20T01:11:34Z | V4N1LLA/Eunhye_Hymn | develop | 22206920873 | 4052cc9e3753fcd2bc824f89df1780f90b09c2dd | FAIL | PASS | PASS | PASS | 8.9 | HOLD | BLOCKED | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22206920873 | codex | preflight session expired; preflight failed |
