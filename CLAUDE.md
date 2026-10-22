# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Java/Gradle backend for a note-taking / to-do app ("Kevin's Todo App", Gradle project name `NoteElements`). Fork of `kkraft714/todo_app` (see `git remote -v`: `origin` is this fork, `upstream` is the original; sync with `git pull upstream main` - plain merge (`git pull --no-rebase upstream main`, or set `git config pull.rebase false` once), not `--ff-only`, since the fork carries its own commits like this file). Early prototype - no REST/UI layer, and no persistence of the domain model yet (Hibernate is configured but no classes are mapped). See `todo_app_spec.md` for the product spec and intended UI/data model, and `README.md` for open design questions.

## Commands

```bash
./gradlew build          # compile + test
./gradlew test           # run all tests (JUnit 5 / Jupiter)
./gradlew test --tests server.NoteOrganizerTest   # run a single test class
./gradlew test --tests "server.NoteOrganizerTest.someMethodName"  # single test method
```

Java 21 (Gradle toolchain in `build.gradle`; tests use Java 21 APIs like `List.getFirst()`). `gradle-wrapper.properties` points at Gradle 6.9.4, but `./gradlew` currently fails with `NoClassDefFoundError: org/gradle/cli/CommandLineParser` (wrapper jar mismatch). Regenerate the wrapper (`gradle wrapper` with a local Gradle) or run tests from IntelliJ.

Developed in IntelliJ IDEA (both maintainers, Michael Paulukonis and Kevin Kraft) - `.idea/` project files are checked in. Expect IntelliJ's own test runner to be how this project is normally exercised day-to-day.

### Database (Docker)

```bash
docker-compose up --build    # Postgres + app; see DOCKER_SETUP.md for full details
docker-compose up -d postgres  # just the DB, for local Gradle runs against it
```

- `DBSanityTest` just opens a Hibernate session - it fails with no DB reachable. Start Postgres first if running the full suite.
- Connection config: `src/main/resources/hibernate.properties` (main) and `src/test/resources/hibernate.properties` (test) - placeholders (`PORT`, `DB_NAME`, `USERNAME`, `PASSWORD`) are meant to be filled in or overridden.
- `HibernateUtil` (`src/main/java/server/database/HibernateUtil.java`) overrides the properties file from env vars: `DB_USERNAME`/`DB_PASSWORD` for credentials, and `DB_PORT`+`DB_TEST_NAME` together to rewrite the JDBC URL to `localhost:$DB_PORT/$DB_TEST_NAME` (`DB_PROD_NAME` is read but unused). This naming does **not** match `docker-compose.yml`/`.env.example` (`DB_USER`, `DB_NAME`) - reconcile before assuming Docker Compose env vars flow into Hibernate.

## Conventions

The domain model changes in most upstream PRs, so it isn't summarized here - read the code (`server.note`, `server.NoteOrganizer`, `server.SearchCriteria`); it's small.

- `server.NoteOrganizer` holds the in-memory note tree plus category and type indexes. Add/remove notes through it (`addNote`/`addChildNote`/`deleteNote`); mutating a note directly - including `NoteBase.addCategory` - silently bypasses the indexes.
- The code is heavily annotated with `// ToDo:` comments recording open design questions - read them before changing a class, and don't resolve them unilaterally.

<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:970c3bf2 -->
## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

**Architecture in one line:** issues live in a local Dolt DB; sync uses `refs/dolt/data` on your git remote; `.beads/issues.jsonl` is a passive export. See <https://github.com/gastownhall/beads/blob/main/docs/SYNC_CONCEPTS.md> for details and anti-patterns.

## Agent Context Profiles

The managed Beads block is task-tracking guidance, not permission to override repository, user, or orchestrator instructions.

- **Conservative (default)**: Use `bd` for task tracking. Do not run git commits, git pushes, or Dolt remote sync unless explicitly asked. At handoff, report changed files, validation, and suggested next commands.
- **Minimal**: Keep tool instruction files as pointers to `bd prime`; use the same conservative git policy unless active instructions say otherwise.
- **Team-maintainer**: Only when the repository explicitly opts in, agents may close beads, run quality gates, commit, and push as part of session close. A current "do not commit" or "do not push" instruction still wins.

## Session Completion

This protocol applies when ending a Beads implementation workflow. It is subordinate to explicit user, repository, and orchestrator instructions.

1. **File issues for remaining work** - Create beads for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **Handle git/sync by active profile**:

   ```bash
   # Conservative/minimal/default: report status and proposed commands; wait for approval.
   git status

   # Team-maintainer opt-in only, unless current instructions forbid it:
   git pull --rebase
   bd dolt push
   git push
   git status
   ```

5. **Hand off** - Summarize changes, validation, issue status, and any blocked sync/commit/push step

**Critical rules:**

- Explicit user or orchestrator instructions override this Beads block.
- Do not commit or push without clear authority from the active profile or the current user request.
- If a required sync or push is blocked, stop and report the exact command and error.
<!-- END BEADS INTEGRATION -->
