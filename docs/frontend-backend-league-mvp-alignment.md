# Frontend / Backend League MVP Alignment

## Objetivo

Cerrar el flujo MVP de torneos tipo Liga sin volver a mover verdad oficial al frontend.

## Lo que ya existia

- formato `LEAGUE` en backend
- launch backend-driven
- fixture automatico double round-robin
- tarjetas de partidos de torneo
- carga / confirmacion / rechazo de resultados
- standings oficiales
- `openEnrollment` persistido en backend
- `competitive` persistido en backend

## Huecos detectados

- el desempate configurable no estaba completo:
  - frontend tipado ya conocia `SETS_DIFFERENCE`
  - backend solo soportaba `GAMES_DIFFERENCE`
  - el orden oficial de standings ni siquiera leía el desempate persistido
- la categoria del torneo existia solo como seleccion visual local
- el alta por link para torneos seguia pendiente
- crear torneo desde frontend siempre mandaba `openEnrollment = true`
- el wording del flujo competitivo no estaba del todo alineado a “por los puntos”

## Decisiones de este slice

- mantener backend como source of truth de:
  - tipo de inscripcion abierta/cerrada
  - categoria declarada del torneo
  - desempate oficial de standings
  - links oficiales de invitacion al torneo
- no cambiar el alcance MVP de Liga:
  - sigue siendo todos contra todos
  - sigue siendo exactamente dos rondas
  - los partidos del torneo siguen sin afectar rating oficial
- la actualizacion “tiempo real” del MVP se resuelve con refresh inmediato del estado backend despues de acciones oficiales, no con websockets
