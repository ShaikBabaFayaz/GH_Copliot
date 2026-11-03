
---
name: implementation-planning
description: Creates technology-independent, traceable implementation plans from approved requirements, architecture, design reviews, and UI/UX specifications for any software project.
---

# Purpose

Guide SDLC Stage 4: Implementation Planning.

Create clear, ordered, realistic, and testable implementation tasks for projects using any programming language, framework, platform, or technology stack.

# 1. Mandatory Inputs

Read the following documents when applicable:

- `docs/requirements.md`
- `docs/architecture.md`
- `docs/design-review.md`
- `docs/ui-ux-spec.md`
- `docs/project-profile.md`
- Existing repository instructions and project conventions.

Before planning:

1. Verify that mandatory documents exist and have explicit human approval.
2. Identify the approved Project Technology Profile.
3. Inspect the existing repository structure.
4. Identify existing components, modules, dependencies, and configuration.
5. Check design-review findings and unresolved issues.
6. Stop and request human guidance if essential information or approvals are missing.

# 2. Technology Discovery

- Support any programming language, framework, database, platform, or deployment architecture.
- Discover technologies from existing repository configuration and approved architecture.
- Identify frontend, backend, database, API, infrastructure, and testing technologies where applicable.
- Never assume Java, Spring Boot, React, Angular, Vue, Python, or any specific framework.
- Follow existing project conventions and approved technology decisions.
- Do not introduce new frameworks, dependencies, or technologies without explicit approval.
- Distinguish existing technologies from approved proposed technologies.

# 3. Planning Principles

- Plan only approved requirements and features.
- Break complex features into small, manageable, ordered tasks.
- Ensure every task maps to approved requirements or acceptance criteria.
- Identify dependencies between tasks.
- Specify affected files, modules, components, and interfaces.
- Define measurable completion criteria.
- Include implementation and testing activities.
- Avoid duplicate tasks and unnecessary implementation complexity.
- Plan incremental changes instead of unnecessary large-scale rewrites.

# 4. Frontend and UI/UX Planning

When the project includes a user interface:

1. Read the approved `docs/ui-ux-spec.md`.
2. Use `.github/skills/ui-ux-design/SKILL.md`.
3. Identify required pages, screens, and user journeys.
4. Break frontend work into screen and component-level tasks.
5. Identify reusable components and shared design tokens.
6. Plan navigation, forms, tables, dashboards, and interactive elements where applicable.
7. Include responsive behavior for supported devices.
8. Include accessibility requirements.
9. Plan loading, empty, error, success, disabled, and interactive states.
10. Identify frontend API integration and state-management requirements.
11. Include appropriate component and frontend verification tasks.

If the project has no UI, mark frontend planning as NOT APPLICABLE.

# 5. Backend and Data Planning

When applicable:

- Break backend features into services, modules, and API tasks.
- Identify API contracts and integration requirements.
- Plan data models, database changes, and migrations.
- Include input validation and error handling.
- Include authentication and authorization requirements.
- Consider security, performance, scalability, and data integrity.
- Identify external service and infrastructure dependencies.

Do not create backend or database tasks when they are not required.

# 6. Testing and Quality Planning

Plan appropriate testing activities according to project architecture and risk.

Consider:

- Unit testing.
- Component testing.
- API testing.
- Integration testing.
- End-to-end testing.
- Accessibility testing.
- Visual regression testing.
- Performance and security testing.

Rules:

- Use the project's existing approved testing framework.
- Do not assume Maven, JUnit, or any specific test runner.
- Do not automatically include every testing category.
- Include only testing types within the approved scope.
- Identify test cases for normal behavior, invalid inputs, boundary conditions, and expected errors.
- Define expected results and completion criteria.

# 7. Implementation Task Structure

Generate `docs/implementation-plan.md`.

Use the following table:

**Implementation Tasks:**

`Task ID | Workstream | Task Description | Related Requirement | Files/Components | Dependencies | Completion Criteria`

Supported workstreams include:

- Frontend
- Backend
- Database
- API/Integration
- Infrastructure
- Testing
- Documentation
- UI/UX

Use only applicable workstreams.

Each task must:

- Have a unique task ID.
- Have a clear description.
- Reference relevant requirement IDs.
- Identify affected files or components where known.
- Identify prerequisite tasks where applicable.
- Define measurable completion criteria.

# 8. Task Ordering

Arrange tasks in logical implementation order:

1. Project setup or prerequisites, if required.
2. Data models and foundational components.
3. Backend and API implementation where applicable.
4. Frontend components and screen implementation where applicable.
5. Frontend/backend integration.
6. Error handling, validation, and security.
7. Appropriate testing.
8. Final implementation checks and documentation.

Adapt this order to the actual architecture. Do not force irrelevant stages into the plan.

# 9. Validation Checklist

Before presenting the plan, verify:

- [ ] All approved requirements are covered.
- [ ] Tasks are traceable to acceptance criteria.
- [ ] Dependencies and task ordering are correct.
- [ ] Frontend tasks follow the approved UI/UX specification where applicable.
- [ ] Backend and database tasks follow approved architecture where applicable.
- [ ] Testing tasks use approved project tools.
- [ ] Completion criteria are measurable.
- [ ] No unapproved features or technologies are introduced.
- [ ] No essential design-review findings are ignored.
- [ ] No unnecessary tasks are included.

# 10. Human Approval and Output Rules

1. Generate only `docs/implementation-plan.md`.
2. Keep the document concise and preferably within 40 lines, including headings and tables.
3. Present the implementation plan and summarize important dependencies and risks.
4. Ask the human for explicit approval.
5. If rejected, incorporate feedback and request approval again.
6. After approval, request separate authorization before committing the document.
7. Proceed to Stage 5 only after explicit human approval.

Never:

- Invent requirements or architectural decisions.
- Modify approved requirements, architecture, or UI/UX specifications.
- Implement application source code.
- Introduce unapproved technologies or dependencies.
- Treat silence or ambiguous responses as approval.
- Commit without explicit authorization.
