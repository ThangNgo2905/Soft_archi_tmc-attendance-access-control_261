# Frontend Coding Rules

Stack: Vite + React + TypeScript + MUI + React Router + TanStack Query.

## Folder responsibilities

| Path | Role |
|------|------|
| `src/app/` | App shell providers, theme, root router wiring |
| `src/pages/` | Route-level screens only (composition) |
| `src/components/layout/` | AppBar, Drawer, AppShell |
| `src/components/common/` | Reusable presentational pieces |
| `src/features/<domain>/` | Domain hooks, types, feature-specific UI |
| `src/api/` | HTTP client, typed endpoints |

## Rules

1. **Pages compose; they do not own HTTP details** — call hooks from `features/` or `api/`.  
2. **No business rules in components** — validation/display only; domain decisions stay on the backend.  
3. Prefer MUI components and the shared theme in `app/theme`.  
4. Use TanStack Query for server state; avoid duplicating fetch logic in every page.  
5. Environment: `VITE_API_BASE_URL` for the backend base URL.  
6. Route names should mirror product areas (Employees, Gates, Attendance, Reports, Settings).  
7. Actor-oriented nav labels are allowed; real auth guards come after ADR 0002 follow-up.  

## Naming

- Pages: `SomethingPage.tsx`  
- Components: `PascalCase.tsx`  
- Hooks: `useSomething.ts`  

## Quality

- TypeScript strict mode  
- Lint before PR (`npm run lint`)  
- Build must succeed in CI (`npm run build`)  
