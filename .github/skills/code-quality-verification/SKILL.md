---
name: code-quality-verification
description: Guides implementation, code review, and verification across SDLC Stages 5–7, ensuring code quality, requirements compliance, and reliable unit testing.
---

# Code Quality and Verification Skill

## Purpose
Guide Stage 5 (Implementation), Stage 6 (Review), and Stage 7 (Verification) using approved requirements, architecture, and implementation plans.

## 1. Mandatory Inputs
- Read `docs/requirements.md`, `docs/architecture.md`, `docs/design-review.md`, and `docs/implementation-plan.md`.
- Confirm required approvals exist. If documents are missing, unapproved, or contain blocking questions, stop and ask the human.
- Never invent requirements, bypass approval gates, or make unauthorized decisions.

## 2. Stage 5 — Implementation
- Implement only approved features and acceptance criteria.
- Follow the approved Java, Spring Boot, Maven, H2, HTML, CSS, JavaScript, and Bootstrap architecture where applicable.
- Keep code readable, maintainable, modular, and consistent with existing conventions.
- Separate controller, service, repository, model, and DTO responsibilities according to the approved design.
- Validate inputs, handle expected errors, protect data integrity, and avoid exposing sensitive information.
- Avoid unnecessary dependencies, duplicated logic, and unrelated changes.
- Do not modify protected configuration, dependencies, or architecture without authorization.
- Write or update focused JUnit 5 unit tests for normal behavior, invalid inputs, boundary conditions, and expected errors.
- Review changed files, execute relevant tests, and report implementation results honestly.

## 3. Stage 6 — Code Review
- Compare implementation against approved requirements, architecture, design review, and implementation plan.
- Check correctness, maintainability, naming, duplication, validation, error handling, security, and data integrity.
- Check unit-test coverage and investigate failures; never assume tests pass without execution.
- Record findings with ID, severity, affected file, evidence, impact, recommendation, and status.
- Classify findings as Critical, High, Medium, or Low.
- Fix authorized defects, update tests, and rerun affected tests.
- Request approval for fixes requiring scope, architectural, dependency, or configuration changes.

## 4. Stage 7 — Verification
- Verify acceptance criteria using implementation evidence and JUnit 5/Maven unit tests.
- Run `mvn test` when the environment permits; inspect results and investigate failures.
- Do not add UI, smoke, API, integration, or end-to-end testing unless explicitly approved.
- Mark results PASS, FAIL, BLOCKED, or NOT RUN, supported by evidence.
- Document test counts, verified criteria, unresolved defects, blockers, limitations, and the overall verification recommendation.
- Never claim successful verification when required checks failed or remain incomplete.

## 5. Documentation and Approval
- Keep documentation consistent with actual implementation; update only authorized documents.
- Review the final diff and report changed files, test results, findings, and remaining risks.
- Ask for explicit human approval at required stage gates; revise rejected work before requesting approval again.
- Commit only with explicit authorization; approval to proceed does not automatically authorize a commit.
- Never bypass gates, invent results, or claim work is complete without evidence.