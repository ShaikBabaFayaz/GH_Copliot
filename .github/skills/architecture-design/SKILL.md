
---


name: architecture-design


description: Guides technology-independent architecture creation, UI/UX design review, and architecture validation across SDLC Stages 2 and 3, ensuring requirements traceability, technology compatibility, and human approval.

---

# Purpose

Guide SDLC Stage 2 (Architecture) and Stage 3 (Design Review) using a reusable, technology-independent skill.

Support web, mobile, desktop, backend, CLI, library, and other software projects.

# Stage 2: Architecture Design

1. Read `docs/requirements.md`; stop if it is missing, unapproved, or contains blocking questions.
2. Inspect the repository, project instructions, configuration files, dependency manifests, and existing architecture.
3. Identify the application type and discover the existing technology stack.
4. Identify programming languages, frameworks, databases, frontend technologies, build tools, testing tools, and infrastructure where applicable.
5. Identify components, responsibilities, interfaces, dependencies, and interactions.
6. Design frontend architecture when the project includes a user interface.
7. Describe backend, API, database, service, repository, and infrastructure interactions where applicable.
8. Follow existing project conventions and approved technology decisions.
9. Do not assume Java, Spring Boot, React, Angular, Vue, Python, or any other fixed technology stack.
10. Do not introduce new frameworks, dependencies, databases, or infrastructure without human authorization.
11. Explain technology choices, data flows, significant decisions, constraints, dependencies, security considerations, and risks.
12. Clearly distinguish existing technologies, proposed technologies, and unresolved decisions.
13. Generate `docs/architecture.md` with:
    - Architecture overview.
    - Project Technology Profile.
    - Component responsibility table.
    - Frontend architecture where applicable.
    - Backend, API, and data architecture where applicable.
    - Data flow and significant decisions.
    - Relevant risks and constraints.
14. If the project does not require a user interface, document UI/UX as NOT APPLICABLE.
15. Keep `docs/architecture.md` within 40 lines, including headings and tables.
16. Validate completeness, consistency, and alignment with approved requirements and repository conventions.
17. Present the architecture and request explicit human approval.
18. If rejected, incorporate feedback, update the document, and request approval again.
19. After approval, request separate authorization before committing `docs/architecture.md`. Commit only that file if authorized.

# Stage 3: Design Review

1. Begin only after explicit approval of `docs/architecture.md`.
2. Read `docs/requirements.md` and `docs/architecture.md`.
3. Use `.github/skills/ui-ux-design/SKILL.md` when the project includes a user interface.
4. Review requirements coverage, component responsibilities, interactions, data flow, technology choices, constraints, and risks.
5. Check whether the architecture follows the approved Project Technology Profile.
6. Identify missing components, inconsistencies, ambiguities, unnecessary complexity, and architectural gaps.
7. When a UI exists, review:
   - Required screens and user journeys.
   - Wireframes and screen layouts.
   - Navigation and information architecture.
   - Design system and reusable components.
   - Typography, colors, spacing, and visual hierarchy.
   - Responsive layouts for supported devices.
   - Accessibility requirements.
   - Loading, empty, error, success, and interactive states.
8. Ensure UI/UX decisions are consistent with approved requirements and architecture.
9. Identify missing UI/UX information and request clarification rather than inventing design decisions.
10. Generate `docs/design-review.md` with a summary and findings table:
    `ID | Severity | Finding | Evidence | Recommendation`.
11. When a UI exists, generate `docs/ui-ux-spec.md` containing:
    - Screen and user journey specifications.
    - Wireframe descriptions or available visual mockups.
    - Design system and component specifications.
    - Responsive behavior.
    - Accessibility requirements.
    - Interaction states.
12. If the project does not require a UI, mark UI/UX as NOT APPLICABLE.
13. Keep `docs/design-review.md` within 40 lines, including headings and tables.
14. Keep `docs/ui-ux-spec.md` concise and complete; do not omit essential design decisions.
15. Validate traceability, consistency, completeness, and feasibility.
16. Recommend whether the architecture and design are ready for Stage 4: Implementation Planning.
17. Present the design review and applicable UI/UX specification for human approval.
18. If rejected, incorporate feedback, update the relevant documents, and request approval again.
19. After approval, request separate authorization before committing the generated documents. Commit only authorized files.
20. Proceed to Stage 4 only after explicit human approval.

# Guidelines

- Design only what the approved requirements need.
- Support any programming language, framework, platform, or technology stack.
- Never assume a default technology stack.
- Discover and respect existing project conventions.
- Do not introduce features or change approved technologies without authorization.
- Do not introduce unnecessary dependencies or architectural complexity.
- Use the UI/UX design skill only when the project requires a user interface.
- Do not claim that wireframes, mockups, or visual artifacts exist unless they were actually generated.
- Do not modify `docs/requirements.md` or application source code.
- Do not silently resolve design issues or modify approved architecture during review.
- If review findings require architecture changes, explain the changes and request human direction.
- Clearly distinguish confirmed decisions, proposed choices, and unresolved questions.
- Never treat silence or ambiguous responses as approval.
- Never commit without separate explicit authorization.
- Do not begin Stage 3 before architecture approval or Stage 4 before design-review approval.
