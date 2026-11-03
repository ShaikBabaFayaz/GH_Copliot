---
name: requirements-analysis
description: Analyzes Confluence user stories to produce complete, testable requirements with traceability, ambiguity checks, and human approval.
---

# Purpose
Guide the Requirements Agent through SDLC Stage 1 using a consistent, evidence-based analysis process.

# Analysis Procedure
1. Read the specified user story from Confluence; never invent unavailable source content.
2. Identify the story's goal, users, expected behavior, and acceptance criteria.
3. Derive functional requirements, non-functional requirements, business rules, constraints, dependencies, and edge cases.
4. Identify ambiguities, contradictions, missing details, and assumptions; record unresolved items as open questions.
5. Assign stable IDs: US-01, FR-01, NFR-01, AC-01, BR-01, Q-01.
6. Generate `docs/requirements.md` using the agent's required tables, keeping the entire document within 40 lines.
7. Check that requirements are clear, testable, consistent, and traceable to their source story.
8. Present a concise summary and ask the human to explicitly approve or reject the document; revise and ask again if rejected.


## UI/UX Requirements Analysis

When applicable:
- Identify target users and their workflows.
- Identify required screens and navigation.
- Capture usability and accessibility requirements.
- Identify responsive device requirements.
- Document important interaction states.
- Define measurable UI acceptance criteria.

If the project does not require a UI, mark UI/UX as NOT APPLICABLE.


# Concepts and Table Definitions
- **Functional requirement:** A specific behavior the application must provide.
- **Non-functional requirement:** A measurable quality or constraint, such as performance, security, or usability.
- **Acceptance criteria:** Conditions that must hold for a story to be considered complete; use Given/When/Then where applicable.
- **Business rule:** A domain restriction or decision that governs application behavior.
- **Constraint/dependency:** A limitation or prerequisite affecting implementation.
- **Open question:** Missing information requiring clarification; never silently convert it into a confirmed requirement.
- **Traceability:** Links each user story to its requirements and acceptance criteria.
- **Required tables:** User Stories (`ID | Title | Description | Source`); Functional Requirements (`ID | Requirement | Related Story | Priority`); Non-Functional Requirements (`ID | Category | Requirement | Acceptance Measure`); Acceptance Criteria (`Story ID | Criterion ID | Given | When | Then`); Business Rules (`Rule ID | Rule | Validation`); Dependencies/Constraints (`ID | Type | Description | Impact`); Open Questions (`ID | Question | Impact | Status`); Traceability (`Story ID | Requirement IDs | Acceptance Criteria IDs`).
- Use compact Markdown tables; include only applicable rows, preserve essential information, and flag unavailable details instead of fabricating values.
- **Human approval gate:** Stop after presenting requirements; Stage 2 must not begin until the human explicitly approves.