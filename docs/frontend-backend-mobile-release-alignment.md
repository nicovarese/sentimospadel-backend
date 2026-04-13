# Frontend / Backend Alignment: mobile release and environment strategy

Fecha: 2026-04-08

## Objetivo

Dejar el proyecto plug-and-play para Android e iOS a nivel repo y definir una estrategia de ambientes que no mezcle el desarrollo diario con la validacion del CEO.

## Estado auditado

- frontend ya tenia `Capacitor` instalado
- `android/` ya existia
- iOS todavia no estaba generado
- backend ya aceptaba origins de `Capacitor`
- deep links seguian dependiendo del bridge web actual
- no existe todavia pipeline de staging/production

## Decision de alineacion

- se genera `ios/` en el frontend
- se agregan permisos/config minima para pruebas de perfil e invites en iOS
- se mantiene el backend sin cambiar contratos de negocio
- se documenta una estrategia de ramas `main / staging / develop`
- se documenta un setup local claro para testers no tecnicos

## Ajustes realizados

Frontend:

- se genero `frontend/ios`
- se agrego custom scheme nativo `sentimospadel://app`
- iOS permite carga web HTTP para QA local dentro del webview
- iOS declara permisos de camara y libreria para foto de perfil
- Android declara el mismo custom scheme para apertura de links nativos
- se agrego documentacion de packaging mobile

Backend:

- no se cambiaron contratos
- se mantiene estrategia de base URL por entorno

## Riesgos que siguen abiertos

- invite links publicos todavia salen como URLs web, no universal links nativos
- no existe deploy estable de `staging`
- no existe firma/release mobile cerrada todavia

## Recomendacion

- seguir desarrollando en `develop`
- reservar `staging` para QA / CEO
- no usar local dev como ambiente de demo para stakeholders
