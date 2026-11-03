# Stage 1 Requirements Analysis
**Status:** Stage 1 completed and approved by the user on 2026-11-03. **Project:** Inventory Management System; single-location inventory with a responsive UI.
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
| NFR-01..05 | Responsive UI; persistence; integrity/validation; error handling/security; automated testing | Provide the qualities listed by SCRUM-2. | Measurable targets and security/test specifics are not stated; see Q-01. |
## Acceptance Criteria
| Story ID | Criterion ID | Given | When | Then |
|---|---|---|---|---|
| US-01 / SCRUM-3 | AC-01..07 | Inventory manager uses product management. | Add, view list/details, update, or delete a product. | Capture name, SKU, description, category, unit price, threshold; enforce unique SKU, required/valid values; allow deletion only when business rules permit. |
| US-02 / SCRUM-4 | AC-08..14 | An existing product has stock. | Add stock or reduce it for a sale/removal. | Update and persist quantity; reject negative resulting balance or nonpositive movement quantity; show an appropriate error for invalid operations. |
| US-03 / SCRUM-6 | AC-15..20 | Stock changes occur. | Record and view a product's movement history. | Record every IN/OUT movement with product ID, type, quantity, reason/reference, date/time; retain records and display chronologically. |
| US-04 / SCRUM-7 | AC-21..27 | Product has a configurable minimum threshold. | Stock changes or low-stock products are viewed. | Mark/list products with stock <= threshold, including zero; update automatically; do not mark products above threshold. |
| US-05 / SCRUM-8 | AC-28..35 | Products exist. | Search by name/SKU and filter by category/stock status, together or cleared. | Identify in-stock/low-stock products; show a helpful no-match message; clearing restores all products. |
## Business Rules
| Rule ID | Rule | Validation |
|---|---|---|
| BR-01..05 | SKU unique; stock cannot be negative; movement quantity positive; low stock means current quantity <= minimum threshold (zero included); deletion is subject to unspecified business rules. | Enforce on product/stock operations; define deletion behavior in Q-03. |
## Dependencies and Constraints
| ID | Type | Description | Impact |
|---|---|---|---|
| D-01..03 | Dependency/constraint | Product records underpin stock, search, thresholds, and movement records; stock movements must persist with their associated change; single-location scope, with four Epic exclusions above. | Product data and atomic stock/history behavior are foundational; storage/transaction choices await stack discovery. |
## Open Questions
| ID | Question | Impact | Status |
|---|---|---|---|
| Q-01..06 | Q-01: UI/accessibility and measurable quality targets; Q-02: product formats, SKU normalization, category source and initial stock; Q-03: deletion with history; Q-04: movement reason, actor and timestamp/time zone; Q-05: currency precision; Q-06: concurrency/atomicity. | Needed for testable acceptance and safe data behavior; stack confirmation is recorded in project-profile.md. | Open; resolve before implementation where applicable. |
## Traceability
| Story ID | Requirement IDs | Acceptance Criteria IDs |
|---|---|---|
| US-01 (SCRUM-3)<br>US-02 (SCRUM-4)<br>US-03 (SCRUM-6)<br>US-04 (SCRUM-7)<br>US-05 (SCRUM-8) | FR-01, NFR-01..05, BR-01<br>FR-02, NFR-02..05, BR-02..03, D-01..02<br>FR-03, NFR-02..04, D-02<br>FR-04, NFR-01..04, BR-04<br>FR-05, NFR-01, D-01 | AC-01..07<br>AC-08..14<br>AC-15..20<br>AC-21..27<br>AC-28..35 |