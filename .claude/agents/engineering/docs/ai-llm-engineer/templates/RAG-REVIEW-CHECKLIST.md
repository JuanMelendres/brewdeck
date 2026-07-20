# RAG Review Checklist

## Sources

- [ ] Source owner known
- [ ] Data classification known
- [ ] Access rules defined
- [ ] Freshness defined
- [ ] Deletion defined
- [ ] Provenance retained

## Ingestion

- [ ] Parsing validated
- [ ] Chunking justified
- [ ] Metadata complete
- [ ] Duplicates handled
- [ ] Versioning defined
- [ ] Failures observable

## Retrieval

- [ ] User or tenant filters
- [ ] top-k bounded
- [ ] Score threshold
- [ ] Maximum context
- [ ] No-evidence behavior
- [ ] Freshness behavior
- [ ] Retrieval evaluation

## Generation

- [ ] Untrusted context marked
- [ ] Real citations only
- [ ] Unsupported claims rejected
- [ ] Structured output validated
- [ ] Prompt injection tested

## Operations

- [ ] Reindex plan
- [ ] Model-change plan
- [ ] Cost budget
- [ ] Latency budget
- [ ] Feature flag
- [ ] Kill switch
- [ ] Metrics and alerts
