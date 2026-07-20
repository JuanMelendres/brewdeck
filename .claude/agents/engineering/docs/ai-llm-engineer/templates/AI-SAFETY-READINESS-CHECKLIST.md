# AI Safety and Readiness Checklist

## Product

- [ ] Clear user outcome
- [ ] AI adds justified value
- [ ] Non-goals explicit
- [ ] Limitations visible

## Data

- [ ] Minimum data used
- [ ] Secrets excluded
- [ ] Personal data policy
- [ ] Retention
- [ ] Deletion
- [ ] Provider assumptions
- [ ] Cross-user isolation

## Model and prompts

- [ ] Responsibility boundary
- [ ] Versioned prompt
- [ ] Untrusted context markers
- [ ] Structured schema
- [ ] Validation and fallback
- [ ] Uncertainty behavior

## Tools

- [ ] Tool allowlist
- [ ] Authentication outside model
- [ ] Authorization outside model
- [ ] Input validation
- [ ] Confirmation
- [ ] Idempotency
- [ ] Audit events
- [ ] Maximum tool calls

## Safety

- [ ] Prompt injection tests
- [ ] Data leakage tests
- [ ] Refusal tests
- [ ] No secret logging
- [ ] No arbitrary code or SQL
- [ ] Feature kill switch

## Quality and operations

- [ ] Evaluation dataset
- [ ] Regression baseline
- [ ] Latency budget
- [ ] Token and cost limits
- [ ] Timeouts
- [ ] Provider fallback
- [ ] Observability
- [ ] Incident runbook
