# Frontend / Backend Alignment: Capacitor mobile packaging

Fecha: 2026-04-08

## Objetivo

Iniciar el empaquetado mobile sin cambiar contratos backend por motivos de packaging.

## Estado auditado

- frontend ya tiene `Capacitor`
- la API usa `VITE_API_BASE_URL` con fallback local a `http://localhost:8081`
- backend ya acepta origins mobile de `Capacitor`
- deep links mobile siguen apoyandose en el flujo existente de query params
- Android e iOS ya existen como proyectos nativos dentro del repo

## Decision de alineacion

- se mantiene `Capacitor` en el frontend sin tocar contratos de negocio
- se mantiene backend como source of truth
- se deja Android listo para abrir en Android Studio
- se deja iOS plug-and-play a nivel repo, aunque el build real sigue dependiendo de macOS + Xcode

## Reglas tecnicas

- mobile debe usar `VITE_API_BASE_URL` explicita para builds no-dev
- backend debe aceptar origins de `Capacitor`:
  - `http://localhost`
  - `capacitor://localhost`
  - `ionic://localhost`
- el soporte de deep links mobile debe adaptarse sobre el flujo actual de invites, no reemplazarlo
- push nativo queda para un slice posterior sobre la infraestructura backend ya implementada

## Estado actual del repo

- `frontend/android` generado y sincronizado
- `frontend/ios` generado y sincronizado
- custom scheme nativo disponible:
  - `sentimospadel://app`
- iOS ya declara:
  - permisos de camara para foto de perfil
  - permisos de libreria de fotos
  - soporte HTTP en webview para QA local
- frontend excluye `android/`, `ios/` y `dist/` del chequeo de TypeScript para que `npm run lint` siga estable
