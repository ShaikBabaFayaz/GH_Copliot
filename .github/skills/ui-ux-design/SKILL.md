
---

name: ui-ux-design

description: Provides technology-independent UI/UX design guidance for software projects, including product discovery, wireframes, design systems, responsive layouts, accessibility, interaction states, and visual quality validation.


---

# UI/UX Design Skill

## 1. Purpose

Create professional, intuitive, consistent, accessible, and responsive user interfaces for any software project.

This skill is technology-independent. It must work with the project's existing frontend framework, backend architecture, design tools, and technology stack.

Do not assume Java, Spring Boot, React, Angular, Vue, Bootstrap, Tailwind, or any specific framework.

## 2. Project Discovery

Before designing or implementing a UI:

1. Inspect the existing repository structure and project instructions.
2. Identify the application type: web, mobile, desktop, embedded, or other.
3. Identify the frontend framework, styling system, component library, and build tools.
4. Identify existing design systems, reusable components, assets, and conventions.
5. Understand the target users, primary workflows, and business objectives.
6. Identify supported browsers, devices, screen sizes, and accessibility requirements.
7. Reuse existing technologies and conventions unless an approved change is required.
8. Do not introduce dependencies without justification and approval.

If the project has no user interface, mark UI/UX design as NOT APPLICABLE and explain why.

## 3. UX Research and Information Architecture

Identify:

- Target users and their needs.
- Primary user journeys and workflows.
- Navigation hierarchy.
- Information architecture.
- Important user actions and decision points.
- Forms, tables, dashboards, and other required interface elements.
- Error prevention and recovery paths.

Keep navigation intuitive and minimize unnecessary interactions.

## 4. Wireframe and Screen Design

For each required screen:

1. Define its purpose.
2. Identify its main layout and content hierarchy.
3. Define navigation and user interactions.
4. Identify reusable interface components.
5. Specify desktop, tablet, and mobile behavior.
6. Document loading, empty, error, success, disabled, and focus states.

Create wireframes or low-fidelity layouts before detailed implementation.

Use visual mockups when supported by available design tools. Otherwise, provide clear screen specifications and layout diagrams.

Never claim that a mockup or visual artifact was generated unless it actually exists.

## 5. Design System

Define a consistent design system containing:

- Color palette and semantic color roles.
- Typography and text hierarchy.
- Spacing scale and layout rules.
- Grid and alignment conventions.
- Buttons and interactive controls.
- Forms and validation messages.
- Cards, tables, dialogs, and navigation.
- Icons and image guidelines.
- Borders, shadows, and corner radii.
- Component states and interaction behavior.

Prefer existing project design tokens and components.

Avoid arbitrary styling, inconsistent spacing, excessive colors, unnecessary gradients, and decorative elements that reduce usability.

## 6. Visual Design Quality

Follow these principles:

- Establish a clear visual hierarchy.
- Prioritize readability and content clarity.
- Use consistent alignment and spacing.
- Maintain appropriate contrast.
- Avoid overcrowded layouts.
- Use whitespace effectively.
- Keep interfaces visually consistent across screens.
- Make primary actions easy to identify.
- Provide meaningful feedback after user actions.
- Use realistic content instead of unnecessary placeholder text.
- Avoid generic template-like interfaces when the project requires a distinctive product identity.

## 7. Responsive Design

Design for the actual supported device range.

Consider:

- Mobile-first or project-appropriate responsive layouts.
- Flexible containers and grids.
- Responsive navigation.
- Appropriate typography scaling.
- Touch-friendly interactive elements.
- Responsive forms, tables, dialogs, and dashboards.
- Prevention of unintended horizontal overflow.

Do not simply shrink the desktop interface to fit mobile screens.

## 8. Accessibility

Follow WCAG 2.2 AA where applicable and required.

Check:

- Semantic structure.
- Keyboard navigation.
- Visible focus indicators.
- Appropriate labels and accessible names.
- Color contrast.
- Alternative text for meaningful images.
- Accessible form errors.
- Screen-reader compatibility.
- Reduced-motion preferences where applicable.

Do not rely on color alone to communicate status.

## 9. Interaction and Component Behavior

Specify and implement:

- Hover, focus, active, and disabled states.
- Loading indicators and progress feedback.
- Empty states with useful guidance.
- Error messages and recovery actions.
- Success confirmations.
- Form validation.
- Dialog and menu behavior.
- Navigation and routing.
- Appropriate feedback for asynchronous operations.

Ensure that controls perform their advertised actions.

## 10. Frontend Implementation Guidance

1. Follow the approved architecture and UI/UX specification.
2. Reuse existing components and design tokens.
3. Build modular, reusable interface components.
4. Separate presentation, application state, and business logic appropriately.
5. Follow the framework's established conventions.
6. Keep styling maintainable and avoid duplicated rules.
7. Connect UI components to approved APIs and data contracts.
8. Handle loading, empty, error, and success states.
9. Preserve existing functionality.
10. Avoid unrelated frontend rewrites.

Do not change the project's technology stack without explicit approval.

## 11. Visual Quality Verification

When supported and authorized:

- Run the application in the target environment.
- Inspect all important screens.
- Check responsive layouts at representative viewport sizes.
- Verify navigation and interactive controls.
- Check loading, empty, error, and success states.
- Inspect browser console errors.
- Check accessibility and keyboard navigation.
- Capture screenshots as implementation evidence.
- Compare the implementation with the approved design.

Record actual results. Never claim visual verification without evidence.

## 12. Design Review Checklist

Before approving the design, verify:

- [ ] All required screens are identified.
- [ ] Primary user journeys are covered.
- [ ] Navigation and information hierarchy are clear.
- [ ] Reusable components are identified.
- [ ] Design tokens are documented.
- [ ] Responsive behavior is specified.
- [ ] Accessibility requirements are addressed.
- [ ] All important interaction states are documented.
- [ ] Design is consistent with project architecture.
- [ ] Design decisions are traceable to approved requirements.

## 13. Constraints

- Do not invent business requirements.
- Do not introduce unnecessary frameworks or libraries.
- Do not replace existing design systems without approval.
- Do not implement unapproved screens or functionality.
- Do not modify architecture without authorization.
- Distinguish proposed designs from approved designs.
- Request clarification when essential design decisions are missing.
- Preserve existing project conventions.
- Obtain human approval before implementation when the workflow requires it.
