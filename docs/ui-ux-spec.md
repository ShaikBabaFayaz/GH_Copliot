# UI/UX Specification: Inventory Management System
**Status:** Stage 3 UI/UX specification approved by the user on 2026-11-03; proposals remain subject to Stage 4 decisions before implementation. Based on approved requirements and Stage 2 architecture; React/TypeScript, Spring Boot, and in-memory H2 are confirmed. Epic SCRUM-2; child stories SCRUM-3/4/6/7/8. No UI source, design system, mockups, or screenshots exist.

## Users, Journeys, and Navigation
Inventory managers sign in, find/filter products, inspect details, create/edit products, record IN/OUT stock, review movement history, and monitor low stock. Proposed navigation: Products and Low Stock; product details expose Edit, Stock movement, and History. Preserve story scope; do not add workflows.

## Screens and Behavior
- **Login:** Username/password form for HTTP Basic and the INVENTORY_MANAGER role. Provision the credential from local environment variables; never commit defaults. Send through the Authorization header only, never store persistently or log. Use loopback-only HTTP locally and HTTPS if network-bound. Explicit logout clears client-held credentials.
- **Products:** Search name/SKU; combine category/status filters; show count, active status, price, balance and threshold. Require name/SKU/category/INR price/minimum threshold; description optional. Trim text; uppercase SKU; show inactive products only where explicitly requested (no reactivation control).
- **Product detail/edit:** Show price in INR at two fractional digits; reject negative or >2-place input without rounding. Deactivation is terminal for every product; confirm that it leaves normal lists and preserves ID/history. Never offer permanent deletion, reactivation, or stock operations on inactive products.
- **Stock movement:** Choose IN/OUT; positive quantity and required reason/reference; show balance and expected version. Initial stock is zero; opening balance uses IN. On stale-version conflict, reload and let the user review before retry; on inactive-product operations, show a clear rejection. Successful operations refresh balance/status/history.
- **Movement history:** Show chronological direction, quantity, reason/reference, actor username and server UTC instant rendered in the user's local timezone.
- **Low Stock:** List products whose balance is less than or equal to threshold, including zero; link rows to product detail. Search/filter screens support combined filters and clear-to-restore behavior.

## Authentication and Recovery
Every `/api/v1` operation requires Basic authentication. Use consistent Problem Details: 400 validation, 401 sign-in required, 403 forbidden, 404 missing resource, 409 duplicate SKU/stale version/inactive-product stock attempt. On logout clear client-held credentials; do not imply a timeout policy. For network/5xx failures retain safe form input and offer retry; for uncertain stock outcomes refresh balance/history before allowing repeat. Never expose stack traces or credentials.

## Concurrency and Data Integrity
Stock requests carry the expected product version. On version conflict, do not overwrite or automatically resubmit: explain that stock changed, reload current balance/history, let the user review and deliberately retry. A conflict creates no movement; successful balance/version update and movement record are atomic. Reject nonpositive quantities and negative resulting balances. All product deletion deactivates the record, preserving its ID and all movement history. In-memory H2 data, including products and history, is lost on process restart; this is an accepted constraint, not a recovery promise. State the limitation clearly to users/operators.

## Layout, Responsive Design, and Accessibility
Use IBM Plex Sans, neutral surfaces, teal primary actions, compact readable tables, labeled forms, inline errors and explicit status text. Verify layouts at 360/768/1024/1440 px; stack filters/forms on narrow screens and avoid horizontal page overflow. Target WCAG 2.2 AA, keyboard operation, visible focus, logical focus order, programmatic error/status announcements and reduced motion. Provisional API target: p95 <=500 ms for <=1,000 products; measure and report on recorded hardware/workload.

## States and Open Decisions
Specify loading, empty/no-match, validation/API errors, success, disabled/pending, 400/401/403/404/409, logout, and retry states. Keep focus and recoverable input; prevent duplicate actions. Remaining evidence/approval: actual performance results against the provisional benchmark, exact dependency installation approval, and the published list of version pins. Category, field-bound and integer-stock policies are approved. No dependencies are installed.

## Traceability
US-01 / SCRUM-3: product list/detail/edit/deactivation. US-02 / SCRUM-4: IN/OUT movement, validation, version conflict and retry. US-03 / SCRUM-6: retained chronological history and H2 restart-loss notice. US-04 / SCRUM-7: low-stock threshold including zero. US-05 / SCRUM-8: name/SKU search, combined filters, empty and clear states. All stories are children of Epic SCRUM-2. No visual mockups were generated.