## Git Commit Rules

Every commit must follow these rules in both its message and content.

### Commit Message

The commit message must use one of these formats:
- `[topic] summary`
- `[topic] subject: summary`

Notes:
- Do not use action-oriented words in `topic` or `subject`.
  - Verbs such as `fix`, `new`, `add`, or `refactor` are not allowed in `topic` or `subject`.
  - Verbs may appear in `summary`.
- `topic` is a short, stable area/aspect, not a meta category.
  - Not allowed: meta categories such as `features`, `chores`.
  - Examples of allowed topics:
    - generic in code: `ui`, `model`, `service`, `util`, `test`.
    - specific in code: `*-ui`, `settings`, `mediastore`, `room`, `action`.
    - build system: `dependency`, `build`, `toolchain`.
    - release: `changelog`, `release`, `preview`.
    - miscellaneous: `docs`, `scripts`, `ci`.
    - ...
- `subject` is _optional_ and names the concrete subject of the commit.
  - Prefer short, symbolic, referable entities such as a class name.
  - It may be approximate; it does not need to be an exact identifier.
- `summary` is required and describes this commit.
  - Action verbs are allowed here.
  - Keep it concise; _avoid a trailing period_.

### Commit Content: CLEAN

The content of a commit must be **CLEAN**:

- **C — Compilable**. Every commit must **build**.
  - A commit should be complete and "whole", and pass the project’s build.
    - Run the Gradle build before committing.
    - If a logical change cannot build when split, keep it as one _atomic_ commit.
  - Do not create a commit that cannot pass the build.

- **L — Logical**. Commits record **logical** development history.
  - Order commits by component dependencies so the history can be understood from the bottom up.
    - Do not commit a development diary: rebase to remove trial-and-error noise.
    - Arrange commits in dependency order rather than chronological development order.
  - Do not preserve detours or abandoned design revisions in the final history.

- **E — Encapsulated**. Commits should be self-contained and **merge-friendly**.
  - A commit should be orthogonal and understandable without another unmerged commit.
    - Structure commits to keep _rebases_, _cherry-picks_, and _merges_ producing few local conflicts.
    - If possible, separate moves, renames, and formatting from logic changes.
    - Make sure diffs are focused and easy to review.
  - Do not mix unrelated "by-the-way" changes into the commit.

- **A — Atomic**. One commit should serve **one** job or goal.
  - A commit is atomic when it cannot be split into smaller meaningful, buildable commits.
    - Every commit should _not_ be divided into smaller commits while it still stands alone.
    - Keep each commit as the smallest meaningful, buildable unit.
  - Do not squash everything into one commit.

- **N — Neat**. Keep each commit **minimal** and **clean**.
  - Neat commits change as _few_ files and lines as possible within their atomic purpose.
    - A commit should change the minimum necessary lines and files.
    - If it can be split safely, split it into _atomic_ commits.
  - Avoid large commits that change millions of files or lines.
    - Exception: reorganizing the file/package hierarchy.
    - Exception: renaming a symbol leading to changes in imports.
    - Exception: unavoidable refactoring that changes interfaces.