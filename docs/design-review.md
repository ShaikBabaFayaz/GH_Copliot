# Stage 3 Design Review
**Status:** Stage 3 completed and approved by the user on 2026-11-03. Stage 2 architecture remains approved.
**Scope:** Epic SCRUM-2; child stories SCRUM-3, SCRUM-4, SCRUM-6, SCRUM-7, SCRUM-8. Review uses approved requirements, architecture, and project profile; no source or manifests exist, so this is not runtime verification.
**Summary:** React/TypeScript -> Spring Boot -> in-memory H2 covers product, stock, history, low-stock, and search flows (FR-01..05). Soft deletion, Basic authentication, optimistic locking, and acceptance of restart data loss are confirmed decisions. Recommendations below are design follow-ups, not new requirements.
**Findings**
| ID | Severity | Finding | Evidence | Recommendation |
|---|---|---|---|---|
| F-01 | Resolved | Concurrent stock writes use optimistic locking; stale writes conflict rather than overwrite newer balances. | User-confirmed decision; architecture requires version comparison and atomic balance/version plus movement transaction (US-02/03). | Implement conflict response and atomic rollback; verify concurrent OUT, no lost updates, and no movement on conflict. |
| F-02 | Resolved | Product deletion is soft deletion; products are deactivated and history is retained. | User-confirmed soft-deletion decision; architecture uses active flag and retained movements (AC-07, US-01/03). | Never physically delete products; show active state clearly, retain history, and exclude inactive products from normal inventory/low-stock views. |
| F-03 | Resolved | Basic authentication and initial role are specified. | INVENTORY_MANAGER, env credentials, no defaults/logging/storage; loopback HTTP or network HTTPS. | Protect every API and test 401/403 behavior. |
| F-04 | Resolved | Product required fields, normalization, category lookup, initial stock, numeric bounds, and INR rules are set. | Approved decisions and BR-01..09; countable integer stock only; reject negative/out-of-range values and price scale >2 without rounding. | Enforce requirements at UI/API/database boundaries. |
| F-05 | Resolved | `/api/v1`, identity, product, movement, query, and Problem Details status contract are specified. | 400/401/403/404/409 mapping; stale versions must not create movements. | Implement exactly and test validation/auth/conflict paths. |
| F-06 | Resolved | UI/quality targets are specified; latency target remains provisional and dependencies await approval. | WCAG 2.2 AA, four viewport widths, IBM Plex Sans, neutral/teal, p95 target 500 ms at 1,000 products. | Measure against recorded hardware/workload; do not report target met without test evidence. |
| F-07 | Resolved | Deactivation is terminal; inactive products cannot be reactivated or receive stock. | Approved lifecycle policy; preserve ID/history and hide from normal lists. | Enforce and test terminal deactivation. |
| F-08 | Accepted | In-memory H2 loses all inventory and movement data when the process restarts. | User explicitly accepts restart data loss; confirmed local H2 constraint (US-03). | Keep this limitation visible to users/operators; no persistence redesign is proposed in this review. |
**Open Issues**
| ID | Issue | Impact | Status |
|---|---|---|---|
| Q-01 | Actual performance result against the approved benchmark. | Evidence for provisional p95 target. | Open until benchmark is run; report results without presuming success. |
| Tooling | Exact backend test framework and dependency versions. | Build/test reproducibility and approval gate. | Awaiting separate approval before install. |
**Readiness:** Policy decisions approved; implementation awaits exact dependency/version approval and Q-01/02 specifics. H2 restart loss is accepted. Traceability: SCRUM-2; US-01..05 / SCRUM-3, SCRUM-4, SCRUM-6, SCRUM-7, SCRUM-8; FR-01..05, NFR-01..05, BR-01..08, AC-01..35. No code implemented.