# Backend agent instructions

- Repository context: read [.agents/docs/project.md](.agents/docs/project.md).
- Before code changes: read [.agents/rules/development.md](.agents/rules/development.md).
- Source of truth: source files and build configuration take precedence over stale notes.
- Instruction style: use concise bullets, numbered steps, or tables throughout `AGENTS.md` and `.agents/`.

## Feature development harness

- Trigger: feature implementation, feature extension, and continuation of planned feature work.
- Workflow: read and follow [.agents/skills/feature-development.md](.agents/skills/feature-development.md).
- Before implementation: inspect existing code and publish a concrete plan in the conversation.
- Implementation: deliver the minimum required behavior; avoid overengineering.
- PR limit: at most 400 added plus deleted lines per PR, including tests, configuration, and documentation.
- Overflow: split into coherent follow-up PRs; never waive the limit or omit necessary tests to fit.

## Code review harness

- Trigger: code review, PR review, regression checks, and re-review of previous findings.
- Workflow: read and follow [.agents/skills/code-review.md](.agents/skills/code-review.md).
- Session: start a new independent session for every review; inherit no conversation history.
- Allowed inputs: review scope, repository location, and explicit requirements.
- Excluded inputs: implementation reasoning, conclusions, and suggested findings.
- Non-triggers: ordinary implementation, debugging, and explanatory questions.
- Loading: use this pointer to the flat Markdown file; do not rely on automatic skill discovery.
- Location: keep harness instructions in `.agents/`.
- History: manage in GitHub; do not add harness history sections or changelogs to these instructions.
