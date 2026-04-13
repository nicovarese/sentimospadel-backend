# Frontend / Backend Alignment: environments

Fecha: 2026-04-08

## Objetivo

Separar desarrollo diario, demo estable para CEO y produccion real sin mezclar datos ni configuraciones.

## Hallazgos auditados

- El frontend depende de `VITE_API_BASE_URL`.
- El frontend tenia fallback a `localhost` fuera del ambiente local.
- El backend tenia defaults locales validos para desarrollo.
- El backend tenia seeds demo mezclados dentro del flujo principal de Flyway.
- Mobile y web comparten el mismo backend, asi que la URL base debe salir del ambiente y no del canal.

## Decision

- `local` y `staging` comparten seeds demo.
- `production` usa solo migraciones de esquema y no carga demo data.
- El frontend queda con modos explicitos `staging` y `production`.
- `staging` es el ambiente del CEO y QA manual.
- `Android Studio` queda solo para pruebas nativas concretas, no como ambiente principal de negocio.

## Corte tecnico aplicado

- Se movieron las migraciones demo a `classpath:db/seed`.
- `application.yml` mantiene local/demo por defecto cargando `db/migration` + `db/seed`.
- `application-staging.yml` exige variables reales de entorno y conserva demo data.
- `application-production.yml` exige variables reales de entorno y carga solo `db/migration`.
- El frontend ya puede construir con `--mode staging` o `--mode production`.

## Variables de entorno a controlar

Frontend:

- `VITE_API_BASE_URL`
- `VITE_GEMINI_API_KEY` opcional

Backend:

- `SPRING_PROFILES_ACTIVE`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `EMAIL_VERIFICATION_BASE_URL`
- `EMAIL_VERIFICATION_LOGIN_URL`
- `EMAIL_VERIFICATION_FROM`
- `MATCH_INVITATION_BASE_URL`
- `TOURNAMENT_INVITATION_BASE_URL`
- `PLAYER_PROFILE_PHOTO_STORAGE_PATH`
- `PLAYER_PROFILE_PHOTO_PUBLIC_BASE_URL`
- `PUSH_NOTIFICATIONS_PROVIDER`
- `PUSH_NOTIFICATIONS_LOG_PAYLOADS`

## Recomendacion operativa

- `develop` para trabajo diario
- `staging` para demos, QA y CEO
- `main` solo para lo aprobado
