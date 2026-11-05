# Stage 1 Requirements Analysis
**Status:** Stage 1 completed and approved. Stage 5 policy/T01 decisions approved 2026-11-03; dependency installation awaits separate approval. Approved: INVENTORY_MANAGER Basic auth with environment credentials; product validation/opening stock; movement reason/actor/UTC timestamp; `/api/v1` Problem Details contract; terminal deactivation; WCAG 2.2 AA, target viewports/visual direction; INR scale 2; category vocabulary, field bounds and integer stock bounds. Jira is unchanged.
**Epic:** [SCRUM-2](https://agentic-sdlc-practise.atlassian.net/browse/SCRUM-2), To Do, Medium. Scope: product and stock management, movement history, low-stock monitoring, search/filter; barcode scanning, suppliers, purchase orders, and multi-warehouse support are out of scope.
**Technology profile:** Confirmed by the user: full-stack web app; Java/Spring Boot backend; React/TypeScript frontend; in-memory H2; local deployment. See [project-profile.md](project-profile.md).
## User Stories
| ID | Title | Description | Source |
|---|---|---|---|
| US-01..05 | Product Management; Stock Management; Stock Movement History; Low Stock Monitoring; Product Search and Filtering | Maintain product records; record incoming/outgoing stock and quantities; audit movements; identify below-threshold products; find products by search and filters. All Medium, To Do, parent SCRUM-2. | [SCRUM-3](https://agentic-sdlc-practise.atlassian.net/browse/SCRUM-3); [SCRUM-4](https://agentic-sdlc-practise.atlassian.net/browse/SCRUM-4); [SCRUM-6](https://agentic-sdlc-practise.atlassian.net/browse/SCRUM-6); [SCRUM-7](https://agentic-sdlc-practise.atlassian.net/browse/SCRUM-7); [SCRUM-8](https://agentic-sdlc-practise.atlassian.net/browse/SCRUM-8) |
## Functional Requirements
| ID | Requirement | Related Story | Priority |
|---|---|---|---|
| FR-01..05 | Product CRUD with name, SKU, description, category, unit price, minimum threshold, unique SKU and validation; stock IN/OUT with positive quantity, nonnegative balance, persistence and errors; complete chronological per-product movement history; automatic low-stock identification at current <= threshold including zero; combined name/SKU search and category/status filters with empty-state and clear behavior. | US-01..05 | Medium (Jira) |
## Non-Functional Requirements
| ID | Category | Requirement | Acceptance Measure |
|---|---|---|---|
| NFR-01..05 | Responsive UI; persistence; integrity/validation; error handling/security; automated testing | Provide SCRUM-2 qualities and approved security/data rules. | WCAG 2.2 AA; 360/768/1024/1440 px; provisional API p95 <=500 ms at <=1,000 products; reassess after measurement. |
## Acceptance Criteria
| Story ID | Criterion ID | Given | When | Then |
|---|---|---|---|---|
| US-01 / SCRUM-3 | AC-01..07 | Inventory manager uses product management. | Add, view list/details, update, or deactivate a product. | Require name/SKU/category/INR price/minimum threshold; description optional; trim text, uppercase unique SKU, start at zero stock; deletion always deactivates and preserves history. |
| US-02 / SCRUM-4 | AC-08..14 | An active product has stock. | Record opening/IN stock or reduce/OUT stock. | Start at zero; record opening stock as IN; reject inactive-product operations, nonpositive movement and negative result; require expected version and reject stale versions without a movement. |
| US-03 / SCRUM-6 | AC-15..20 | Stock changes occur. | Record and view a product's movement history. | Record movement ID, product ID, IN/OUT, positive quantity, required reason/reference, authenticated username and server UTC timestamp; retain history, show chronologically in user-local time. |
| US-04 / SCRUM-7 | AC-21..27 | Product has a configurable minimum threshold. | Stock changes or low-stock products are viewed. | Mark/list products with stock <= threshold, including zero; update automatically; do not mark products above threshold. |
| US-05 / SCRUM-8 | AC-28..35 | Products exist. | Search by name/SKU and filter by category/stock status, together or cleared. | Identify in-stock/low-stock products; show a helpful no-match message; clearing restores all products. |
## Business Rules
| Rule ID | Rule | Validation |
|---|---|---|
| BR-01 | SKU is trimmed and uppercased; enforce case-insensitive uniqueness at database level. | Reject duplicates after normalization. |
| BR-02 | Price, minimum threshold and stock cannot be negative; low stock means quantity <= threshold, including zero. | Reject negative input and compute low-stock status at the inclusive threshold. |
| BR-03 | Every stock movement quantity is a positive integer countable unit. | Reject zero/negative and fractional quantities. |
| BR-04 | Category is a seeded extensible lookup: Electronics, Office & Stationery, Furniture, Tools & Equipment, Cleaning & Janitorial, Packaging & Shipping, Consumables, Other. | Add categories by lookup data rows, without schema/code changes; no category-admin workflow in scope. |
| BR-05 | All product deletions are soft deletions (deactivation), regardless of whether movement history exists. | Set active=false; never physically delete the product or its movement history. |
| BR-06 | Product prices use INR with exactly two fractional digits. | Reject values with more than two fractional digits at the UI/API boundary; persist decimal scale 2; never silently round. |
| BR-07 | Name (1-120 Unicode code points), SKU (1-64 trimmed ASCII matching `[A-Z0-9][A-Z0-9._/-]{0,63}` after uppercase), category, price and threshold are required; description optional, max 2,000 code points; reject controls except newline/tab. Initial stock is zero. | Enforce bounds at UI/API; represent opening balance as an IN movement. |
| BR-08 | Every movement requires reason/reference and records authenticated username plus server-generated UTC timestamp. | Reject missing reason; display UTC instant in user's local timezone. |
| BR-09 | Countable stock and movement quantities are integer units, max 999,999,999,999; no fractional quantity/UOM in scope. | Store as integer-scale decimal (`DECIMAL(12,0)`); reject fractional/out-of-range values. |
## Dependencies and Constraints
| ID | Type | Description | Impact |
|---|---|---|---|
| D-01..03 | Dependency/constraint | Product records underpin stock, search, thresholds, and movement records; stock movements must persist with their associated change; single-location scope, with four Epic exclusions above. | Product data and atomic stock/history behavior are foundational; H2 restart data loss is accepted. |
## Open Questions
| ID | Question | Impact | Status |
|---|---|---|---|
| Q-01 | Actual API benchmark results; p95 <=500 ms is provisional until measured with the approved T01 setup. | Determine observed performance under the agreed 1,000-product workload and record environment/results. | Deferred to implementation verification; no result claimed. |
## Traceability
| Story ID | Requirement IDs | Acceptance Criteria IDs |
|---|---|---|
| US-01 (SCRUM-3)<br>US-02 (SCRUM-4)<br>US-03 (SCRUM-6)<br>US-04 (SCRUM-7)<br>US-05 (SCRUM-8) | FR-01, NFR-01..05, BR-01, BR-04..07<br>FR-02, NFR-02..05, BR-02..03, BR-07..09, D-01..02<br>FR-03, NFR-02..04, BR-08..09, D-02<br>FR-04, NFR-01..04, BR-02<br>FR-05, NFR-01, D-01 | AC-01..07<br>AC-08..14<br>AC-15..20<br>AC-21..27<br>AC-28..35 |