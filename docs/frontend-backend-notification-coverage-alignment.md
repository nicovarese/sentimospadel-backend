# Frontend / Backend Notification Coverage Alignment

## Objetivo
- Mantener `pending actions` como fuente oficial para tareas accionables
- Expandir el inbox con eventos reales del core MVP
- Evitar que el frontend invente notificaciones derivadas de estado local

## Hallazgos
- Hoy `GET /api/notifications` solo devuelve notificaciones sincronizadas desde `PendingActionService`
- El sync actual desactiva cualquier notificacion activa cuya `actionKey` no siga siendo pendiente
- Eso impide mezclar en la misma tabla:
  - tareas vivas (`submit` / `confirm`)
  - eventos historicos simples (`resultado confirmado`, `torneo lanzado`, `verificacion aprobada`)

## Decision
- Mantener una sola tabla `player_notifications`
- Separar dos ciclos de vida:
  - `managedBySync=true` para pending actions
  - `managedBySync=false` para eventos persistentes
- Agregar cobertura MVP para eventos oficiales de backend:
  - `MATCH_FULL`
  - `MATCH_CANCELLED`
  - `MATCH_RESULT_CONFIRMED`
  - `MATCH_RESULT_REJECTED`
  - `TOURNAMENT_LAUNCHED`
  - `TOURNAMENT_RESULT_CONFIRMED`
  - `TOURNAMENT_RESULT_REJECTED`
  - `CLUB_VERIFICATION_APPROVED`
  - `CLUB_VERIFICATION_REJECTED`

## Reglas
- Las pending actions siguen saliendo de cálculo backend al consultar inbox
- Los eventos nuevos se publican en el momento exacto del cambio de estado oficial
- El frontend solo presenta y enruta:
  - abrir acción pendiente si todavía existe
  - abrir torneo o verificación si aplica
  - marcar como leída si es solo informativa

## Criterio de done
- `GET /api/notifications` mezcla pending actions activas con eventos reales persistidos
- El sync de pending actions ya no apaga eventos históricos
- Matches, torneos y club verification emiten notificaciones útiles para el MVP
- El inbox frontend muestra labels/iconos coherentes para ambos tipos de notificación

## Slice implementado
- Se agregó `managed_by_sync` en `player_notifications`
- `PlayerNotificationService` ahora separa:
  - notificaciones sincronizadas desde pending actions
  - eventos persistentes del dominio
- El backend publica eventos para:
  - partido completo
  - partido cancelado
  - resultado social confirmado
  - resultado social rechazado
  - torneo lanzado
  - resultado de torneo confirmado
  - resultado de torneo rechazado
  - verificacion de club aprobada/rechazada
- El frontend mantiene el mismo inbox, pero ahora:
  - renderiza iconos y labels para eventos no accionables
  - abre estado de torneo cuando corresponde
  - abre verificacion del jugador cuando corresponde
  - marca como leida una notificacion informativa sin mostrar el mensaje viejo de “accion no disponible”
