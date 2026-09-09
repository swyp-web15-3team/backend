---
name: feature-development
description: Plan and implement minimal backend features within a strict 400-line PR limit. Use for new features, feature extensions, continuing a plan, or splitting an oversized feature into follow-up PRs. Exclude standalone reviews and explanatory questions.
---

# Feature development

## Context and execution

- Required reading: `AGENTS.md`, `.agents/docs/project.md`, and `.agents/rules/development.md`.
- Mode: plan and implement in the working session; perform review in a fresh independent session.
- Ownership: preserve unrelated staged, unstaged, and untracked work.
- Resumption: inspect the current diff and prior plan; update remaining work before further implementation.
- History: use GitHub; do not create harness history sections.

## 1. Plan before implementation

- Trace: inspect the affected flow, callers, tests, configuration, and existing reusable code.
- Outcome: state the user-visible behavior and concrete acceptance criteria.
- Scope: list affected files, required changes, and explicit exclusions.
- Minimum solution: prefer existing code, standard library, native platform features, then installed dependencies.
- Verification: identify focused behavior tests and required repository checks.
- PR breakdown: estimate added plus deleted lines, including tests, docs, and configuration.
- Budget: leave room below 400 lines for regression tests and review fixes.
- Dependencies: order independently valid slices; define each PR's base, scope, and acceptance criteria.
- Delivery: present the plan as bullets in the conversation before editing implementation files.
- Approval: proceed within the authorized task; ask only for missing requirements that block correct implementation.

## 2. Implement the smallest complete slice

- Scope: implement one planned PR slice at a time.
- Reuse: extend existing behavior at the shared source of truth.
- Avoid: speculative abstractions, single-use factories, empty layers, future-proof configuration, and unrelated cleanup.
- Dependencies: add a dependency only when existing facilities cannot reasonably satisfy the requirement.
- Correctness: retain input validation, security, error handling, and necessary tests.
- Formatting: keep code readable; never compress code or remove tests to game the line limit.
- Plan changes: update scope and PR breakdown when implementation reveals additional work.

## 3. Enforce the PR limit

- Threshold: added lines + deleted lines must be **at most 400**; 400 passes, 401 fails.
- Coverage: count every changed text file, including tests, configuration, docs, generated files, and lockfiles.
- Comparison: use the actual PR base's merge base with the candidate head; never assume a base branch.
- Renames: count conservatively as deletion plus addition; do not use renames to evade the limit.
- Binary changes: the line check cannot certify them; stop for an explicit review policy instead of counting them as zero.
- Working changes: inspect staged/unstaged diffs and relevant untracked files during implementation.
- Final gate: include all intended files in the candidate commit before running the check; it checks committed changes only.
- Command: `bash .agents/scripts/check-pr-size.sh <actual-base-ref> <candidate-head-ref>`.
- Timing: run before opening/updating each PR and again after review fixes or base changes.
- Failure: reduce the current PR to a coherent slice and move remaining work to a new branch and PR.
- Splitting: separate by behavior or prerequisite; every PR must build and satisfy its own checks.
- Dependencies: either merge the prerequisite first or target a stacked PR at its prerequisite branch.
- Retargeting: recheck the actual diff after a stacked PR's base changes.
- No partial delivery: track remaining acceptance criteria and continue through the planned follow-up PRs.
- Publication: create follow-up PRs when repository access and session authorization permit; otherwise report prepared branches and the specific blocker.
- No evasion: multiple commits inside one oversized PR do not satisfy the limit.

## 4. Verify and review

- Behavior: run focused tests and applicable checks from `.agents/docs/project.md`.
- Review: launch `.agents/skills/code-review.md` in a new session without inherited conversation history.
- Review inputs: provide repository location, scope, and explicit requirements; exclude implementation reasoning and self-assessments.
- Fixes: address supported findings, rerun affected checks, and recount the PR diff.
- Completion: report delivered behavior, checks, line count per PR, and any remaining slices or blockers.

## Data flow

| Artifact | Producer | Consumer |
| --- | --- | --- |
| Acceptance criteria and PR plan in conversation | Working session | Implementation |
| Candidate branch and repository diff | Implementation | Size check and independent reviewer |
| Findings and verification limits | Fresh reviewer | Working session |
| PR links, per-PR counts, and remaining work | Working session | User |

## Failure handling

| Failure | Action |
| --- | --- |
| Missing requirement or actual base | Complete independent investigation; request the missing input |
| More than 400 changed lines | Stop publication; split and rerun the size check |
| Check failure | Fix the cause; do not bypass the gate |
| Required test environment unavailable | Report unverified behavior and blocker; do not claim checks passed |
| Fresh reviewer or publication tool unavailable | Retry once if transient; report blocker without substituting an in-session review |

## Workflow checks

- Normal: plan a small endpoint, implement and test it, pass the line gate, then obtain an independent review.
- Boundary: 400 added/deleted lines pass; 401 fail.
- Overflow: split a larger feature into valid PRs, including tests in each slice's budget.
- Error: an unknown base or binary diff fails the gate rather than reporting a misleading count.
- Self-check: run `python3 .agents/scripts/test_pr_size.py` for boundary and failure cases.
