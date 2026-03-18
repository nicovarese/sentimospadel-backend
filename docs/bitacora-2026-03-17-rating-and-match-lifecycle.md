# Bitácora Técnica - 2026-03-17

## Objetivo del día
Cerrar el lifecycle base de social matches hasta dejar los resultados lo suficientemente estables como para recién ahí conectar el primer slice real de rating. La premisa de producto fue explícita: no avanzar a ranking/ELO mientras el flujo de resultados no tuviera equipos explícitos, confirmación y rechazo controlado. También quedó aclarado que la escala oficial del producto es `1.00..7.00`, tanto para onboarding como para evolución posterior por partidos.

## Estado inicial al comenzar el día
Antes de este bloque de trabajo, el backend ya tenía:
- auth operativo con `register`, `login`, JWT access token y `GET /api/auth/me`
- onboarding survey inicial autenticado, separado del registro, con persistencia de respuestas y cálculo de rating/categoría inicial
- primeros social matches con:
  - `POST /api/matches`
  - `POST /api/matches/{id}/join`
  - `POST /api/matches/{id}/leave`
  - `GET /api/matches`
  - `GET /api/matches/{id}`

En ese punto todavía no existía un lifecycle completo de resultados suficientemente estable para usarlo como fuente de rating.

## Secuencia cronológica de trabajo

### 1. Cerrar el siguiente slice del lifecycle de matches: cancelación y carga de resultado
Primero se extendió el modelo de match para soportar cancelación y resultado cargado, pero todavía sin ranking/ELO.

Archivos y migración principales:
- `src/main/resources/db/migration/V5__add_match_results.sql`
- `src/main/java/com/sentimospadel/backend/match/enums/MatchStatus.java`
- `src/main/java/com/sentimospadel/backend/match/entity/MatchResult.java`
- `src/main/java/com/sentimospadel/backend/match/repository/MatchResultRepository.java`
- `src/main/java/com/sentimospadel/backend/match/service/MatchService.java`
- `src/main/java/com/sentimospadel/backend/match/controller/MatchController.java`

Decisiones:
- se eligió una tabla dedicada `match_results` en vez de embutir el resultado dentro de `matches`
- se agregó `COMPLETED` al lifecycle de `MatchStatus`
- el resultado quedó como una estructura simple con ganador y score resumido

Endpoints incorporados en este bloque:
- `POST /api/matches/{id}/cancel`
- `POST /api/matches/{id}/result`
- `GET /api/matches/{id}/result`

Reglas que se fijaron:
- solo el creador puede cancelar
- no se cancela un match ya completado
- el resultado no se carga sobre matches cancelados
- el resultado solo se carga cuando el match ya está efectivamente listo para jugarse

### 2. Frenar el paso a ranking y estabilizar primero el resultado: equipos explícitos + confirmación
Antes de tocar rating se tomó la decisión de endurecer el flujo de resultados. El resultado por sí solo seguía siendo demasiado permisivo para convertirse en input de ranking.

Razón principal:
- sin equipos explícitos `2 vs 2` no había forma consistente de repartir rating
- sin confirmación, el resultado quedaba demasiado expuesto a errores o carga unilateral

Archivos y migración principales:
- `src/main/resources/db/migration/V6__add_match_teams_and_result_confirmation.sql`
- `src/main/java/com/sentimospadel/backend/match/enums/MatchParticipantTeam.java`
- `src/main/java/com/sentimospadel/backend/match/enums/MatchResultStatus.java`
- `src/main/java/com/sentimospadel/backend/match/entity/MatchParticipant.java`
- `src/main/java/com/sentimospadel/backend/match/service/MatchService.java`
- `src/main/java/com/sentimospadel/backend/match/dto/AssignMatchTeamsRequest.java`
- `src/main/java/com/sentimospadel/backend/match/dto/MatchTeamAssignmentRequest.java`

Endpoints incorporados:
- `POST /api/matches/{id}/teams`
- `POST /api/matches/{id}/result/confirm`

Reglas consolidadas:
- el creador define equipos por ahora
- el match jugable debe quedar exactamente `2` jugadores en `TEAM_ONE` y `2` en `TEAM_TWO`
- la carga del resultado pasa a estado `PENDING`
- el submitter no puede confirmarse a sí mismo
- la confirmación debe venir desde un participante del equipo opuesto
- solo después de la confirmación el match pasa a `COMPLETED`

Resultado de este bloque:
- recién acá el flujo empezó a parecer apto para servir como fuente de rating

### 3. Agregar la última pieza de estabilidad pre-rating: rechazo y reenvío controlado
Todavía faltaba una salida limpia para resultados mal cargados. Se decidió no pasar a ranking hasta tener una vía de rechazo mínima y auditada.

Archivos y migración principales:
- `src/main/resources/db/migration/V7__add_match_result_rejection.sql`
- `src/main/java/com/sentimospadel/backend/match/dto/RejectMatchResultRequest.java`
- `src/main/java/com/sentimospadel/backend/match/service/MatchService.java`
- `src/main/java/com/sentimospadel/backend/match/controller/MatchController.java`

Endpoint incorporado:
- `POST /api/matches/{id}/result/reject`

Reglas finales de rechazo:
- solo puede rechazar un participante del equipo opuesto al submitter
- el submitter no puede rechazarse a sí mismo
- solo se puede rechazar mientras el resultado esté `PENDING`
- si el resultado ya está `CONFIRMED`, no se permite rechazo

Decisión importante de persistencia:
- se mantuvo una sola fila activa de `match_results` por match
- si un resultado es rechazado, la misma fila puede reutilizarse en una resubmisión
- no se agregó todavía historial/versionado completo de intentos porque era demasiado para este slice

Transición definida:
- `RESULT_PENDING` + `MatchResultStatus.PENDING`
- rechazo
- `MatchResultStatus.REJECTED`
- el `MatchStatus` vuelve a `FULL`
- los equipos quedan intactos
- el match queda listo para una nueva carga de resultado

Esta fue la última condición antes de habilitar rating.

## Workflow final de matches que quedó al cierre del bloque
El workflow vigente al final del día quedó así:

1. crear match
2. join/leave hasta tener `4` jugadores
3. asignar equipos explícitos `TEAM_ONE` / `TEAM_TWO`
4. cargar resultado
5. resultado queda `PENDING`, match queda `RESULT_PENDING`
6. desde el equipo opuesto:
   - confirmar y completar
   - o rechazar y volver a `FULL`
7. si se rechaza:
   - se puede reenviar resultado
   - los equipos siguen igual
8. recién cuando el resultado queda `CONFIRMED`, el match pasa a `COMPLETED`

Conclusión de diseño:
- solo un resultado confirmado y con equipos explícitos se consideró fuente válida para rating

## Clarificación de producto sobre la escala oficial de rating
Durante el día quedó explícitamente aclarado que:
- la escala oficial del proyecto es `1.00..7.00`
- onboarding usa esa escala
- la progresión posterior por partidos usa exactamente esa misma escala
- no había que implementar un Elo clásico estilo `1200`

Eso obligó a alinear el modelo de `player_profiles`, que todavía venía con un campo heredado `current_elo` estilo placeholder entero.

## Implementación del primer slice real de rating/ELO

### 4. Alinear el modelo activo de jugador a `1.00..7.00`
Se decidió limpiar el modelo, no mantener compatibilidad ficticia con `1200`.

Migración principal:
- `src/main/resources/db/migration/V8__align_player_rating_scale_and_history.sql`

Cambios principales:
- `player_profiles.current_elo` se renombró a `current_rating`
- `current_rating` pasó a `NUMERIC(4,2)`
- se agregó `rated_matches_count`

Regla de migración elegida:
- si el jugador ya tenía `initial_rating`, ese valor pasa a ser su `current_rating`
- si no tenía `initial_rating`, y solo existía el placeholder viejo tipo `1200`, se lo migró a `1.00`

Supuesto explícito:
- los valores `1200` heredados no representaban un Elo válido del producto, solo un placeholder previo a la definición oficial

También se ajustó el onboarding:
- al guardar la encuesta inicial, el `initial_rating` ahora siembra `player_profiles.current_rating`

Archivos relevantes:
- `src/main/java/com/sentimospadel/backend/player/entity/PlayerProfile.java`
- `src/main/java/com/sentimospadel/backend/player/service/PlayerProfileResolverService.java`
- `src/main/java/com/sentimospadel/backend/onboarding/service/OnboardingService.java`

### 5. Persistencia de historial de rating
Se agregó persistencia dedicada para no depender solo del valor vivo en `player_profiles`.

Tabla agregada:
- `player_rating_history`

Columnas relevantes:
- `player_profile_id`
- `match_id`
- `old_rating`
- `delta`
- `new_rating`
- `created_at`

Archivos nuevos:
- `src/main/java/com/sentimospadel/backend/rating/entity/PlayerRatingHistory.java`
- `src/main/java/com/sentimospadel/backend/rating/repository/PlayerRatingHistoryRepository.java`

Objetivo:
- dejar trazabilidad por jugador y por match
- tener una segunda barrera contra doble aplicación

### 6. Trigger correcto: solo al confirmar resultado
Se conectó la aplicación de rating al punto correcto del lifecycle:
- solo cuando el resultado pasa a `CONFIRMED`

Archivo principal:
- `src/main/java/com/sentimospadel/backend/match/service/MatchService.java`

Integración:
- `confirmResult(...)` ahora dispara `RatingApplicationService.applyConfirmedResultIfNeeded(matchId)`

Importante:
- `submitResult(...)` no dispara rating
- `rejectResult(...)` no dispara rating
- repetir `confirmResult(...)` no reaplica si el match ya fue procesado

### 7. Protección contra doble aplicación
Se eligió una protección doble y simple:

1. marcador en `match_results`
- `rating_applied`
- `rating_applied_at`

2. unicidad en historial
- `UNIQUE (match_id, player_profile_id)` en `player_rating_history`

Servicio principal:
- `src/main/java/com/sentimospadel/backend/rating/service/RatingApplicationService.java`

Comportamiento:
- si el resultado no está `CONFIRMED`, no hace nada
- si `rating_applied=true`, no hace nada
- si ya existen las `4` filas esperadas en historial para ese match, marca el resultado como aplicado y no toca ratings otra vez

Decisión:
- preferí un guard persistente simple y explícito sobre soluciones más abstractas

### 8. Fórmula de rating implementada
Se implementó la fórmula exacta pedida en un servicio aislado:
- `src/main/java/com/sentimospadel/backend/rating/service/RatingCalculationService.java`

Reglas de negocio implementadas:
- rating por equipo usando promedio de los 2 jugadores
- probabilidad esperada logística con `s = 0.55`
- `K` dinámico según experiencia promedio del equipo
- pequeño bonus por sets
- comfort zone cap por equipo
- reparto del delta dentro del equipo proporcional al rating actual
- anti-frustration cap por jugador
- clamp final a `1.00..7.00`
- redondeo final a 2 decimales
- mapeo final a categoría Uruguay

Mapeo centralizado:
- `src/main/java/com/sentimospadel/backend/player/support/UruguayCategoryMapper.java`

Detalle importante de implementación:
- para esta primera versión, `teamOneScore` y `teamTwoScore` del resultado confirmado se usan como `setsA` y `setsB`
- `TEAM_ONE` se trató como el equipo A y `TEAM_TWO` como el equipo B para la fórmula, incluyendo el paso de comfort zone cap tal como fue especificado

### 9. Endpoint público de rankings
Se agregó una lectura simple de ranking:
- `GET /api/rankings`

Archivos:
- `src/main/java/com/sentimospadel/backend/rating/controller/RankingController.java`
- `src/main/java/com/sentimospadel/backend/rating/service/RankingService.java`
- `src/main/java/com/sentimospadel/backend/rating/dto/RankingEntryResponse.java`

Comportamiento:
- ordena jugadores por `current_rating` descendente
- devuelve:
  - posición
  - `playerProfileId`
  - `fullName`
  - `city`
  - `currentRating`
  - `currentCategory`
  - `ratedMatchesCount`

## Ajustes laterales hechos durante el día
- `MatchResult.submittedAt` dejó de ser `updatable=false` para que un resultado reenviado tras rechazo actualice correctamente la fecha de submit
- `PlayerProfileResponse` pasó a exponer `currentRating`, `currentCategory` y `ratedMatchesCount`
- `PlayerProfileService` ahora calcula la categoría actual a partir de `currentRating`

## Tests agregados o ajustados
Se agregó cobertura real, no cosmética.

Nuevos tests:
- `src/test/java/com/sentimospadel/backend/rating/service/RatingCalculationServiceTest.java`
- `src/test/java/com/sentimospadel/backend/rating/service/RatingApplicationServiceTest.java`
- `src/test/java/com/sentimospadel/backend/rating/controller/RankingControllerTest.java`

Tests ajustados:
- `src/test/java/com/sentimospadel/backend/match/service/MatchServiceTest.java`
- `src/test/java/com/sentimospadel/backend/onboarding/service/OnboardingServiceTest.java`

Cobertura relevante:
- probabilidad esperada
- bandas de `K`
- set bonus
- comfort zone cap
- anti-frustration cap
- clamp final a `1.00..7.00`
- mapeo de categoría Uruguay
- aplicación del rating solo en `CONFIRMED`
- no aplicación en `PENDING` o `REJECTED`
- no doble aplicación
- endpoint `GET /api/rankings`

Resultado:
- la suite quedó pasando con `.\mvnw.cmd test`

## Documentación actualizada
Se actualizaron:
- `README.md`
- `docs/progress.md`

README quedó reflejando:
- escala oficial `1.00..7.00`
- onboarding y progresión continua en la misma escala
- actualización de rating solo sobre `CONFIRMED`
- existencia de `GET /api/rankings`

## Estado final del backend al cierre del día
Al terminar la jornada, el backend quedó así:
- auth estable
- onboarding estable y alineado con la escala oficial `1.00..7.00`
- social matches con lifecycle completo base:
  - create
  - join
  - leave
  - cancel
  - assign teams
  - submit result
  - confirm result
  - reject result
  - controlled resubmission
- rating vivo en `player_profiles.current_rating`
- historial de rating persistido
- actualización de rating exactamente una vez por resultado confirmado
- ranking público simple disponible
- torneos todavía fuera de alcance

## Recomendaciones para la próxima sesión
1. Agregar tests de integración reales con base de datos para validar `Flyway + confirmResult + player_rating_history` end-to-end.
2. Evaluar si producto necesita endpoint de historial de rating por jugador antes de seguir expandiendo ranking.
3. Definir si el workflow de resultado necesita una capa adicional de amendment/dispute o si el nivel actual ya es suficiente.
4. Recién después de considerar estable este flujo confirmado, extender la misma escala al slice de torneos o ranking más avanzado.
