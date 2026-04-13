# Branching and Environments Guide

Fecha: 2026-04-08

## Objetivo

Separar desarrollo continuo de un ambiente estable para demo y validacion del CEO.

## Recomendacion

No usar `test/dev/prod` como nombres de ramas. Conviene un flujo mas claro:

- `main`: produccion estable
- `staging`: ambiente de validacion, demo CEO y QA manual
- `develop`: desarrollo activo con Codex

## Regla operativa

- Todo trabajo nuevo entra primero en `develop`.
- Cuando un bloque esta funcional y validado, se mergea a `staging`.
- El CEO prueba siempre `staging`, nunca `develop`.
- Solo lo aprobado en `staging` pasa a `main`.

## Ambientes recomendados

- `develop`: puede romperse, sirve para iterar rapido
- `staging`: debe apuntar a una base y frontend estables para demo
- `production`: solo cambios ya aprobados

## Lo importante

La estabilidad del CEO no se resuelve solo con ramas. Tambien necesita:

- un backend desplegado o una maquina fija para staging
- un frontend desplegado o un build mobile/web estable
- una base de datos staging separada de desarrollo
- variables de entorno propias de staging

## Variables por ambiente

Frontend:

- `VITE_API_BASE_URL`

Backend:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `EMAIL_VERIFICATION_BASE_URL`
- `EMAIL_VERIFICATION_LOGIN_URL`
- `MATCH_INVITATION_BASE_URL`
- `TOURNAMENT_INVITATION_BASE_URL`
- `PLAYER_PROFILE_PHOTO_PUBLIC_BASE_URL`

## Corte recomendado a partir de hoy

1. checkpoint tecnico del estado actual
2. crear `staging` desde ese checkpoint
3. dejar `develop` para seguir iterando
4. mantener `main` como lo ultimo realmente aprobado

## Comandos sugeridos cuando hagamos el corte

Frontend:

```powershell
git checkout main
git pull
git checkout -b staging
git push -u origin staging
git checkout -b develop
git push -u origin develop
```

Backend:

```powershell
git checkout main
git pull
git checkout -b staging
git push -u origin staging
git checkout -b develop
git push -u origin develop
```

## Decision practica

Mi recomendacion es:

- no seguir usando `main` para trabajo diario
- usar `develop` conmigo
- reservar `staging` para el CEO desde el proximo push estable
