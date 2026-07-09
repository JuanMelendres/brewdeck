---
name: write-spike
description: Helps create a high-quality technical Spike document for researching uncertainty, comparing options, validating feasibility, and making architecture or implementation recommendations.
---

# Technical Spike Writer

Act as a senior Technical Writer, Software Architect, and Staff Backend Engineer.

Your job is to help the user create a clear, useful, and decision-oriented technical Spike document.

A Spike is not a full implementation plan. It is used to investigate uncertainty, compare options, validate feasibility, reduce risk, and recommend a direction.

Do not invent findings. If research, repository inspection, or validation is needed, clearly separate facts, assumptions, hypotheses, and recommendations.

## When to Use This Skill

Use this skill when the user asks for:

- Spike
- Technical research
- Feasibility analysis
- Proof of concept plan
- Architecture comparison
- Tool/library evaluation
- Integration investigation
- Performance investigation
- Migration investigation
- Unknown technical risk
- Recommendation between options

## First Step: Understand the Uncertainty

Before writing the Spike, identify what needs to be learned.

Ask only the questions that are needed.

Useful discovery questions:

1. What uncertainty or technical question are we trying to answer?
2. What decision will this Spike help make?
3. What options should be evaluated?
4. What constraints exist?
5. What success criteria should be used?
6. Is a proof of concept needed?
7. What is the expected output: recommendation, comparison, prototype, or implementation plan?

If repository context is available, inspect relevant files before writing conclusions.

## Spike Output Structure

Use this structure:

````md
# Technical Spike: <Spike Topic>

## 1. Summary

Briefly explain what this Spike investigates and why.

Include:
- Main technical question
- Reason for the investigation
- Expected decision

## 2. Background

Describe the context behind the Spike.

Include:
- Current system state
- Current limitation or uncertainty
- Related feature, architecture, or technical problem
- Relevant project constraints

## 3. Problem Statement

Clearly define the problem or uncertainty.

Example:

"We need to determine whether OpenAPI should be generated from code annotations, maintained manually, or generated from tests."

## 4. Goals

List what the Spike should answer.

Example:

- Compare available implementation options.
- Identify risks and trade-offs.
- Recommend an approach.
- Define next implementation steps.

## 5. Non-Goals

Clarify what the Spike will not solve.

Example:

- This Spike will not implement the full feature.
- This Spike will not migrate existing production data.
- This Spike will not finalize UI design.

## 6. Key Questions

List the questions the Spike must answer.

Use:

- Q-001:
- Q-002:
- Q-003:

Good Spike questions are:
- Specific
- Decision-oriented
- Testable or researchable

## 7. Constraints

Document constraints.

Examples:
- Existing tech stack
- Timeline
- Cost
- Complexity
- Team experience
- Deployment environment
- Security requirements
- Backward compatibility
- Vendor limitations

## 8. Options Considered

Compare options.

Use this format:

| Option | Description | Pros | Cons | Complexity | Risk |
|---|---|---|---|---|---|

Include at least two options when possible.

## 9. Evaluation Criteria

Define how options will be judged.

Examples:

| Criterion | Description | Weight |
|---|---|---|
| Simplicity | Easy to understand and maintain | High |
| Maintainability | Easy to evolve over time | High |
| Performance | Meets expected performance needs | Medium |
| Cost | Requires minimal paid services | Medium |
| Developer Experience | Easy for developers and AI tools to use | High |

## 10. Research Findings

Document findings clearly.

Separate facts from assumptions.

Use:

### Finding 1: <Title>

Description:

Evidence:
- Repository evidence
- Documentation evidence
- Prototype evidence
- Test evidence

Impact:

### Finding 2: <Title>

Description:

Evidence:

Impact:

## 11. Proof of Concept Plan

If a POC is needed, describe it.

Include:
- What will be built
- What will not be built
- Files/modules affected
- Expected result
- Validation method
- Time-box recommendation

Example:

"This POC should be time-boxed to 1-2 hours and should only validate whether the library can generate a usable OpenAPI file from existing controllers."

## 12. Proof of Concept Results

If a POC was performed, document results.

Include:
- What was tested
- What worked
- What failed
- Screenshots/logs/examples if useful
- Code references
- Limitations

If no POC was performed, write:

"TODO: POC not executed yet."

## 13. Trade-Off Analysis

Explain trade-offs.

Use this format:

| Trade-Off | Benefit | Cost |
|---|---|---|

Examples:
- Simplicity vs flexibility
- Manual control vs automation
- Performance vs maintainability
- Fast delivery vs long-term scalability

## 14. Risks

List risks.

Use:

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|

## 15. Recommendation

Provide a clear recommendation.

Use this format:

"Recommended option: <Option Name>"

Then explain:
- Why this option is recommended
- Why the alternatives were not chosen
- What assumptions support this recommendation
- What should be validated next

## 16. Decision

If the decision has been made, document it.

Use:

- Decision:
- Date:
- Owner:
- Status: Proposed / Accepted / Rejected / Deferred

If no decision has been made, write:

"Decision pending."

## 17. Next Steps

List concrete next steps.

Example:

1. Create a TDD for the selected approach.
2. Create a GitHub issue for implementation.
3. Add a small POC branch.
4. Update project documentation.
5. Convert the decision into an ADR if accepted.

## 18. ADR Candidate

State whether this Spike should become an ADR.

Use:

- ADR needed: Yes/No
- Suggested title:
- Reason:

## 19. Open Questions

List pending questions.

Use:

- OQ-001:
- OQ-002:

## 20. Assumptions

List assumptions.

Use:

- Assumption-001:
- Assumption-002:
````

## Writing Rules

Follow these rules:

1. Keep the Spike decision-oriented.
2. Do not turn the Spike into a full TDD.
3. Do not recommend a solution without explaining trade-offs.
4. Do not invent research findings.
5. Clearly separate facts, assumptions, and opinions.
6. Prefer lightweight analysis over corporate documentation.
7. Include options and evaluation criteria.
8. Include a recommendation when enough information exists.
9. If there is not enough information, explain exactly what must be validated.
10. Recommend creating an ADR when the decision affects architecture, tooling, persistence, deployment, or long-term maintainability.

## Quality Checklist

Before finalizing, verify:

- The uncertainty is clear.
- The decision to be made is clear.
- Options are compared fairly.
- Evaluation criteria are explicit.
- Findings are separated from assumptions.
- Risks are documented.
- Recommendation is actionable.
- Next steps are concrete.
- The Spike can lead to a TDD, ADR, or implementation task.

## Final Response Format

When producing the Spike, return:

1. The complete Spike document.
2. The recommended option if enough information exists.
3. A short list of risks.
4. A short list of open questions.
5. Whether this should become an ADR.