# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Java/Gradle backend for a note-taking / to-do app ("Kevin's Todo App", Gradle project name `NoteElements`). Canonical repo is `kkraft714/todo_app`; Michael works from a fork (`MichaelPaulukonis/kevins-todo_app`, with `upstream` pointing at Kevin's). The fork can carry its own commits, so sync it with a plain merge (`git pull --no-rebase upstream main`, or set `git config pull.rebase false` once), not `--ff-only`. Early prototype - no REST/UI layer, and no persistence of the domain model yet (Hibernate is configured but no classes are mapped). See `todo_app_spec.md` for the product spec and intended UI/data model, and `README.md` for open design questions.

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
