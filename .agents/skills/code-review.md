---
name: code-review
description: Review backend changes for actionable correctness, security, persistence, and API regressions. Use for code review, PR review, regression review, and follow-ups such as re-review, review again, or verify previous findings. Do not trigger for implementation, standalone debugging, or code explanations.
---

# Backend code review

## Role and principles

- Role: one reviewer in a fresh independent session.
- Required reading: `AGENTS.md`, `.agents/docs/project.md`, and `.agents/rules/development.md`.
- Evidence: review demonstrated behavior; exclude hypothetical future requirements.
- Changes: modify source or apply formatting only if the user also requested fixes.
- Publishing: obtain authorization before posting findings to external services.

## Session isolation

- Coordinator: launch a new reviewer session for every review and re-review.
- History: disable inheritance; do not resume or fork the implementation conversation.
- Allowed inputs: repository location, scope (files or base/target revisions),
  explicit user requirements, and this workflow's path.
- Excluded inputs: implementation reasoning, summaries, self-assessments,
  suggested findings, and other workers' conclusions.
- Evidence collection: read the diff and source directly from the repository.
- Reviewer execution: follow this workflow directly; do not recursively launch another reviewer.
- Stable scope: keep reviewed files unchanged while the review runs.
- Changed scope: repeat the review in a new session against the updated files.
- Return value: send independent findings and verification limits to the coordinator.
- Launch failure: report the blocker; do not substitute a review inside the implementation session.

## Input and workflow

1. Scope: identify requested files, commit range, or PR base; inspect working-tree status.
2. Default scope: tracked staged/unstaged changes and relevant untracked files; exclude secrets and generated output.
3. Empty scope: report no changes and request a target; do not invent a base branch.
4. Branch scope: resolve the actual base; review merge-base-to-target changes; exclude unrelated working-tree edits.
5. Unknown base: stop and request it; do not guess the range.
6. Trace: read changed files in context; inspect affected callers, configuration, consumers, and tests.
7. Classify: distinguish introduced regressions from pre-existing problems; inspect requested existing code for audits.
8. Assess: apply the review priorities below; treat repository configuration as authoritative.
9. Verify: run feasible focused checks; avoid starting services or changing data merely for review.
10. Report coverage: distinguish static reasoning from executed verification.
11. Re-review: inspect current code independently before reading prior findings supplied for resolution tracking.
12. Resolve: verify prior findings against current code; require evidence; omit fixed findings from active issues.
13. Context boundary: exclude implementation discussion from re-review inputs.

## Review priorities

- **Correctness:** concrete failing inputs, null/empty boundaries, error paths,
  changed behavior across callers, and missing regression coverage for demonstrated bugs.
- **API and security:** request validation, status preservation, ProblemDetail shape,
  sensitive-data exposure, and authorization/ownership checks where applicable.
  Do not treat an unrequested authentication system as a regression in this foundation.
- **Persistence when present:** transaction boundaries, lazy access with Open Session
  in View disabled, query growth, pagination, race conditions, constraints, and a
  schema application strategy compatible with `ddl-auto: none`.
- **Configuration and delivery:** local/test/prod isolation, hidden production docs
  and diagnostics, environment variables, Java 21 compatibility, and CI coverage.
  `check` runs unit and HTTP tests without a database; database behavior is not covered.
- **Maintainability:** flag complexity only when it causes a concrete maintenance
  problem; do not fill the report with formatter preferences or speculative abstractions.

## Data flow and collaboration

| Input / output                                     | Producer            | Consumer              |
| -------------------------------------------------- | ------------------- | --------------------- |
| Requested scope and repository diff/files          | User and repository | Reviewer              |
| Prior findings, only after independent inspection  | Earlier review      | Reviewer on re-review |
| Findings and verification limits in final response | Independent reviewer | Coordinator and user |

- Session count: one fresh reviewer; no multi-reviewer fan-out required.
- Report file: write only when requested, using the specified path.

## Failures

| Failure                                                      | Action                                                                                                                 |
| ------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------- |
| Transient read/tool failure                                  | Retry once; if still unavailable, identify the missing evidence and limit the review                                   |
| Fresh reviewer session unavailable | Retry once if transient; otherwise report the blocker without substituting an in-session review |
| Missing diff, target, or essential source                    | Stop the affected review and request the missing input; do not claim completion                                        |
| Tests unavailable because of Java, dependencies, or database | Report the cause; continue static review, without claiming tests passed                                                |
| Test fails                                                   | Determine whether the failure is introduced, pre-existing, or environmental; report only what the evidence establishes |

## Output

- Ordering: lead with actionable findings, sorted by severity.

### Finding fields

- `[P1]`, `[P2]`, or `[P3]` and a concise title (use `[P0]` only for an unconditional critical blocker).
- A precise file and the smallest useful line range in the reviewed code.
- The concrete trigger, resulting impact, and supporting evidence; suggest the
  smallest fix when it clarifies the issue.

### Reporting rules

- P1: urgent significant breakage or exposure.
- P2: normal-priority bug.
- P3: minor actionable defect.
- Assumptions: state conditional assumptions explicitly.
- Uncertainty: present unsupported suspicions as questions, not findings.
- Finding count: no quota; report only supported issues.
- Clean result: say “No actionable findings.”
- Closing: list checks run and material coverage limits; do not imply tests passed from static review alone.

## Workflow checks

- Isolation: launch a reviewer with no inherited history and only the allowed inputs;
  confirm that it reads the repository itself and returns findings without spawning another reviewer.
- Isolation failure: an unavailable fresh session produces a blocker, not an in-session review.
- Normal: review a diff exposing rejected validation values; trace the shared handler
  and tests, then report the disclosure at the changed lines with its trigger.
- Re-review: inspect a fix for that disclosure; verify current behavior before marking
  it resolved, and check additional changes for regressions.
- Error: review persistence changes without database verification; complete static
  review and explicitly state that database behavior was not verified.
- Missing input: a clean working tree and no target produces a request for scope,
  not a claim that the branch is safe.
