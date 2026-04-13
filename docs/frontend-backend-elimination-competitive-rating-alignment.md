# Frontend / Backend Alignment: Eliminatoria competitiva y rating oficial

Fecha: 2026-04-08

## Estado auditado

El flujo de `ELIMINATION` ya cubre:

- creacion con categoria, cupos, competitivo/recreativo y abierta/cerrada
- inscripcion abierta por link y desde la app
- registro por equipo con nombre y preferencias horarias
- asignacion de grupos y generacion de cruces al lanzar
- standings por grupos y clasificados a playoffs
- carga, confirmacion y rechazo de resultados
- polling liviano en frontend para reflejar cambios operativos

## Gap detectado

El frontend ya muestra el torneo como `Por los puntos` cuando `competitive=true`, pero el backend todavia no aplicaba rating oficial al confirmar resultados de `ELIMINATION`.

Eso generaba una inconsistencia:

- la UX insinuaba impacto competitivo real
- el estado oficial de rating no cambiaba
- el historial de rating del jugador no podia reflejar esos cruces

## Decision de alineacion

Se alinea el sistema a estas reglas:

- `LEAGUE` no modifica rating oficial del jugador
- `AMERICANO` no modifica rating oficial del jugador
- `ELIMINATION` modifica rating oficial solo cuando `competitive=true`
- `ELIMINATION` recreativo no modifica rating oficial

## Impacto tecnico esperado

- backend aplica rating al confirmar resultados de `tournament_matches`
- `player_rating_history` soporta historial proveniente de partido social o partido de torneo
- frontend consume `affectsPlayerRating` desde backend y deja de inferir ese contrato localmente
