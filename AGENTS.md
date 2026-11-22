# Agent instructions (TMC AAC)

**Single source for all coding agents** (Cursor, Claude Code, Copilot, Windsurf, Cline, ChatGPT, etc.).

Do not invent parallel instruction files. Tool-specific stubs must only say: follow `AGENTS.md`.

## Required workflow

1. Read this file.
2. For Java under `backend/`: also read [docs/rules/backend.md](docs/rules/backend.md) and answer every **Agent pre-flight** question there **before** writing or editing code.
3. Enforce `controller → service → domain ← repository` under `com.tmc.aac.features.<feature>` and apply SOLID as in that doc.
4. For frontend: follow [docs/rules/frontend.md](docs/rules/frontend.md).
5. For architecture: [docs/architecture/README.md](docs/architecture/README.md) and `docs/adr/`.
6. After backend changes: run `backend/mvnw -B test` (Windows: `backend/mvnw.cmd -B test`).

## Bootstrap prompt (paste if the tool does not auto-load this file)

```text
Follow AGENTS.md in this repo as the only agent instruction source.
For backend/ Java: also complete Agent pre-flight in docs/rules/backend.md before coding.
Task: {task}
```

## Rules ownership

| Concern | File to edit |
|---------|----------------|
| Agent entry / workflow | **This file (`AGENTS.md`)** |
| Backend layers, SOLID, pre-flight checklist | [docs/rules/backend.md](docs/rules/backend.md) |
| Frontend conventions | [docs/rules/frontend.md](docs/rules/frontend.md) |

Canonical agent prompt = **`AGENTS.md`**. Detailed domain rules stay in `docs/rules/` — do not duplicate them into IDE configs.
