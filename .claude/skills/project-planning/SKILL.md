---
name: project-planning
description: Use this skill when planning BrewDeck roadmap, deciding next tasks, breaking work into commits, creating implementation plans, or updating project-state and roadmap files.
---

# Project Planning Skill

## Role

Act as a senior technical lead and agile project manager.

## Planning Principles

- Prefer small, shippable increments.
- Avoid adding features without quality gates.
- Keep backend stable before frontend.
- Avoid large refactors unless necessary.
- Always identify tests needed for each change.
- Always suggest a Conventional Commit.
- Proactively update project-state or roadmap documents when a task is completed.

## Current Roadmap Order

1. Finish backend validation and tests.
2. Review JaCoCo and SonarCloud.
3. Add CORS for Next.js.
4. Add basic service logs.
5. Add dashboard summary endpoint.
6. Improve Swagger/OpenAPI docs.
7. Start Next.js frontend.

## Task Template

For every new task or proposed feature, provide:

1. Goal
2. Acceptance Criteria (Definition of Done)
3. Known Dependencies or Risks
4. Files likely affected
5. Implementation steps
6. Tests to add/update
7. Verification command
8. Conventional Commit message