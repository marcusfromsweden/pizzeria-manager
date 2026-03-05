# Development Workflow

Conventions and protocols for working on the pizzeria platform (backend + frontend).

**Last Updated:** 2026-02-11

---

## Table of Contents

- [Session Protocol](#session-protocol)
- [Core Principles](#core-principles)
- [After Implementation Protocol](#after-implementation-protocol)
- [Restart Requirements](#restart-requirements)
- [Commit Conventions](#commit-conventions)
- [Multi-Repo Workflow](#multi-repo-workflow)

---

## Session Protocol

### Starting a New Session

1. Read these documentation files for context:
   - `CLAUDE.md` — Project instructions, architecture, API reference
   - `CODEBASE_ANALYSIS.md` — Deep analysis of backend and frontend code
   - `WORKFLOW.md` — This file (development workflow)
   - `TROUBLESHOOTING.md` — Known issues and solutions
   - `CODING_PATTERNS.md` — Reusable patterns and pitfalls

2. These files are the source of truth for:
   - What features are implemented and their current state
   - How to work with the codebase (build, test, run)
   - Common issues and their solutions
   - Code patterns to follow and pitfalls to avoid

3. **Keep files updated** after significant sessions:
   - Add new issues to `TROUBLESHOOTING.md`
   - Add new patterns to `CODING_PATTERNS.md`
   - Update `CODEBASE_ANALYSIS.md` if architecture changes significantly

---

## Core Principles

### Do Not Auto-Commit

- Make changes without committing automatically
- Leave changes staged but uncommitted
- Wait for explicit user instruction (e.g., "commit changes")
- User decides how to organize commits

### Always Inform About Restart Needs

- After making any changes, state what needs to be restarted
- Check which files were modified and classify (see [Restart Requirements](#restart-requirements))
- State explicitly: "You need to restart: [backend/frontend/both/none]"
- Provide restart commands

### Run Spotless Before Commits

- Backend Java code must be formatted before committing
- Run `mvn spotless:apply` from `pizzeria-service/` before staging Java changes
- Spotless uses Google Java Format

### Run Tests When Relevant

- Run `mvn test` after backend changes
- Run `npm test` from `pizzeria-front-end/` after frontend changes
- Do not commit with failing tests unless explicitly agreed

---

## After Implementation Protocol

**Every time implementation is completed, provide restart information.** This is not optional.

### Mandatory Information Block

```
## Restart Required

Backend: [Yes/No] — [Reason]
Frontend: [Yes/No] — [Reason]
Database: [Yes/No] — [Reason, e.g., new Liquibase changelog]

How to restart:
- Backend: cd pizzeria-service && mvn spring-boot:run
- Frontend: cd pizzeria-front-end && npm run dev
- Database: cd pizzeria-service && docker-compose down && docker-compose up -d
```

### What to Include

1. **Which services need restarting** (backend, frontend, database, none)
2. **Why** each service needs restarting (e.g., "Modified Java service class", "Added new React
   component")
3. **How** to restart (exact commands)
4. **Testing steps** — how to verify the changes work

### When Required

- After implementing a feature
- After fixing a bug
- After modifying any code files
- After adding/changing Liquibase changelogs
- After completing any planned work

### When NOT Required

- Reading files for analysis only (no changes)
- Creating plans/strategies (before implementation)
- Answering questions about existing code

### Example

```
## Implementation Complete: Add pizza search endpoint

### Changes Made
- Added SearchController with GET /api/v1/pizzerias/{code}/pizzas/search
- Added SearchService with fuzzy name matching
- Added SearchControllerTest with 4 test cases

### Restart Information

Backend: Yes — New controller and service classes added
Frontend: No — No frontend changes
Database: No — No schema changes

How to restart:
  cd pizzeria-service && mvn spotless:apply && mvn spring-boot:run

### Testing
1. Start backend
2. GET /api/v1/pizzerias/ramonamalmo/pizzas/search?q=marg
3. Should return Margarita pizza
```

---

## Restart Requirements

### File Change Classification

| Change Type | Restart Required | Reason |
|-------------|------------------|--------|
| **Backend Java files** (controllers, services, repos, config) | Restart backend | Spring Boot needs reload |
| **Backend resources** (application.yaml, messages.properties) | Restart backend | Classpath resources reloaded on start |
| **Liquibase changelogs** (db/changelog/*) | Restart backend + recreate DB | Migrations run on startup |
| **Frontend TypeScript/React** (src/**/*.tsx, *.ts) | Usually auto-reloads (Vite HMR) | Vite hot-reloads; manual F5 if HMR fails |
| **Frontend config** (vite.config.ts, tailwind.config.js) | Restart frontend | Config only read on startup |
| **package.json / pom.xml** | Restart respective service | Dependency changes |
| **Docker Compose** | Restart containers | Container config changed |
| **Documentation** (*.md) | No restart | Documentation only |
| **CSV data files** (db/changelog/data/*.csv) | Restart backend + recreate DB | Loaded via Liquibase |
| **i18n locale files** (src/i18n/locales/*.json) | Usually auto-reloads (Vite HMR) | JSON changes trigger HMR |

### How to Restart

**Backend:**
```bash
# In backend terminal, Ctrl+C then:
cd pizzeria-service
mvn spring-boot:run
```

**Frontend:**
```bash
# In frontend terminal, Ctrl+C then:
cd pizzeria-front-end
npm run dev
```

**Database (full reset):**
```bash
cd pizzeria-service
docker-compose down -v  # -v removes volumes (data loss!)
docker-compose up -d
# Then restart backend to run Liquibase migrations
mvn spring-boot:run
```

### Important Notes

- Backend uses **in-memory token storage** — restarting backend invalidates all active sessions
- Verification tokens are also in-memory — users mid-verification lose their tokens on restart
- Liquibase runs on startup — if changelogs change, the database must be compatible or recreated
- Frontend Vite HMR usually works, but config changes (tailwind, vite, tsconfig) require restart

---

## Commit Conventions

### Message Format

Use Conventional Commits: `type: description`

| Prefix | Use Case |
|--------|----------|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `chore:` | Maintenance, dependency updates |
| `refactor:` | Code restructuring without behavior change |
| `test:` | Adding or updating tests |
| `docs:` | Documentation changes |

### Rules

- Use imperative mood ("Add feature" not "Added feature")
- Keep the first line under 72 characters
- Add body for complex changes (what and why, not how)
- Do not reference AI tools in commit messages
- Always include `Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>` at the end

### Examples

```
feat: add pizza search endpoint with fuzzy matching

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
```

```
fix: prevent NaN values in score input fields

parseInt/parseFloat can return NaN when user clears the input.
Fall back to the minimum valid value (1) instead.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
```

---

## Multi-Repo Workflow

The project has two separate Git repositories:

| Repo | Directory | Remote |
|------|-----------|--------|
| Backend | `./pizzeria-service/` | github.com/marcusfromsweden/pizzeria-service |
| Frontend | `./pizzeria-front-end/` | github.com/marcusfromsweden/pizzeria-front-end |

### Committing Changes

When the user says "commit changes":

1. Check **both** directories for changes (`git status` in each)
2. For each repo with changes:
   - Run formatting (`mvn spotless:apply` for backend)
   - Stage relevant files
   - Generate an appropriate commit message
   - Commit
   - Push to remote (if requested)
3. Commit to each repo separately — they have independent histories

### Coordinated Changes

When a feature spans both repos (e.g., new API endpoint + frontend page):

1. Commit backend first (API must exist before frontend consumes it)
2. Commit frontend second
3. Use related commit messages:
   - Backend: `feat: add pizza search endpoint`
   - Frontend: `feat: add pizza search page`

### Analysis Before Committing

When asked "what to commit" or "changes?":

1. Run `git status` and `git diff` in both repos
2. Summarize changes per repo
3. Suggest commit messages
4. Include restart recommendations
