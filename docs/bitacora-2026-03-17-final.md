# Bitácora Técnica 2026-03-17 Final

## Contexto de arranque
- El backend ya tenía auth operativo:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - JWT access token
  - `GET /api/auth/me`
- El onboarding inicial ya estaba implementado como flujo autenticado separado del registro, con persistencia de respuestas, cálculo de rating inicial y mapeo a categoría Uruguay.
- Los social matches ya existían desde slices anteriores con create/join/leave, pero faltaba estabilizar todo el lifecycle antes de usarlos como fuente confiable de rating.

## 1. Cierre del lifecycle de matches antes de rating
La primera decisión importante del día fue no conectar ranking/rating sobre resultados todavía inestables. Antes de tocar rating se cerró el lifecycle mínimo que permitiera tratar un partido como fuente de verdad razonable.

### Cancelación
- Se consolidó `POST /api/matches/{id}/cancel`.
- Regla final: solo el creador puede cancelar.
- No se permite cancelar un match con resultado pendiente o ya completado.
- El estado `CANCELLED` quedó persistido como parte explícita del lifecycle.

### Carga inicial de resultado
- Se consolidó `POST /api/matches/{id}/result`.
- El resultado quedó persistido en tabla dedicada `match_results`.
- En esta etapa el resultado no completa directamente el match: primero entra como pendiente.

### Equipos explícitos
- Se agregó asignación explícita de equipos en `match_participants.team`.
- Endpoint implementado: `POST /api/matches/{id}/teams`.
- Restricción: solo el creador define equipos por ahora.
- Regla de jugabilidad: el match debe quedar `2 vs 2` antes de aceptar resultado.

### Confirmación de resultado
- Se refinó el flujo con `POST /api/matches/{id}/result/confirm`.
- Un resultado pendiente solo pasa a estado final cuando lo confirma un jugador del equipo opuesto.
- El submitter no puede confirmarse a sí mismo.
- El match pasa a `COMPLETED` recién después de esa confirmación.

### Rechazo de resultado
- Se agregó `POST /api/matches/{id}/result/reject`.
- Solo puede rechazar un participante del equipo opuesto al submitter.
- El submitter no puede rechazar su propio resultado.
- El rechazo solo aplica mientras el resultado esté `PENDING`.
- Se persistió metadata mínima de rechazo:
  - `rejected_by_player_id`
  - `rejected_at`
  - `rejection_reason`

### Resubmisión controlada
- Después de un rechazo:
  - `match_result.status` pasa a `REJECTED`
  - `match.status` vuelve de `RESULT_PENDING` a `FULL`
  - los equipos quedan intactos
  - el resultado puede volver a enviarse
- No se implementó historial versionado de múltiples resultados; se mantuvo un único resultado activo por match para no sobreingenierizar antes de ranking.

## 2. Motivo para esperar antes de implementar rating
La decisión de producto/técnica fue explícita: no aplicar rating sobre resultados `PENDING` ni sobre resultados que todavía podían ser disputados. El objetivo fue llegar primero a una fuente confiable basada en:
- equipos explícitos
- submit
- confirm
- reject
- resubmit controlado

Solo después de estabilizar eso se abrió el slice de rating.

## 3. Aclaración oficial del modelo de rating
Durante el día se fijó la regla oficial de producto:
- la escala activa de rating del proyecto es `1.00..7.00`
- onboarding y progresión por partidos usan exactamente la misma escala
- esto no es un Elo clásico `1200+`
- no se debía inventar otra fórmula ni reutilizar una escala distinta

Esto obligó a alinear el modelo persistido de jugador con la escala oficial.

## 4. Implementación de rating sobre resultados confirmados
Con el lifecycle ya estable, se implementó el primer slice real de rating.

### Alineación del modelo de jugador
Archivo principal:
- `src/main/resources/db/migration/V8__align_player_rating_scale_and_history.sql`

Decisiones:
- se migró desde el placeholder viejo `current_elo` a `current_rating`
- `current_rating` quedó en `NUMERIC(4,2)`
- se agregó `rated_matches_count`
- onboarding y evolución por partidos quedaron unificados sobre la misma escala `1.00..7.00`

Durante esta tarea también se corrigió la migración para que fuera portable entre PostgreSQL y H2:
- se reemplazó la conversión inline por una migración con columna temporal (`current_rating_legacy`) y luego `UPDATE` + `DROP COLUMN`

### Historial de rating
Se agregó persistencia dedicada:
- tabla `player_rating_history`

Campos principales:
- `player_profile_id`
- `match_id`
- `old_rating`
- `delta`
- `new_rating`
- `created_at`

Esto dejó trazabilidad por partido sin sobrecargar `player_profiles`.

### Trigger correcto
El rating quedó disparado únicamente cuando el resultado pasa a `CONFIRMED`.
- No se aplica en `PENDING`
- No se aplica en `REJECTED`
- No se aplica en submit
- No se aplica en reject

### Protección contra doble aplicación
Se dejó doble guard:
- `match_results.rating_applied`
- constraint único en `player_rating_history (match_id, player_profile_id)`

Con esto, aunque se intente reprocesar la confirmación, el rating se aplica una sola vez.

### Endpoint público de ranking
Se dejó operativo:
- `GET /api/rankings`

Comportamiento:
- lista jugadores por `currentRating` descendente
- expone info básica, rating actual, categoría actual y `ratedMatchesCount`

## 5. Endpoints de lectura de historial de rating
Una vez establecida la persistencia del rating, se agregó lectura orientada a frontend y debugging.

### Endpoints implementados
- `GET /api/players/me/rating-history`
- `GET /api/players/{id}/rating-history`

### Diseño
- `/me` requiere JWT
- `/{id}` quedó público en este slice
- orden: newest-first
- respuesta:
  - id de history row
  - `matchId`
  - `oldRating`
  - `delta`
  - `newRating`
  - `createdAt`
  - pequeño resumen del match/resultado cuando existe

Archivos principales:
- `src/main/java/com/sentimospadel/backend/rating/dto/RatingHistoryEntryResponse.java`
- `src/main/java/com/sentimospadel/backend/rating/dto/RatingHistoryMatchSummaryResponse.java`
- `src/main/java/com/sentimospadel/backend/rating/service/PlayerRatingHistoryService.java`

## 6. Endpoint player-facing de historial de matches
Al final del día se cerró el read endpoint que faltaba para la navegación natural del frontend:
- `GET /api/players/me/matches`

### Motivo
Este endpoint se agregó para resolver una necesidad clara de UX y navegación:
- perfil -> matches
- rating history -> match que originó el cambio
- match history -> resultado/estado/equipos del mismo partido

Con esto el frontend ya no depende solo de `GET /api/matches` global para reconstruir el contexto del usuario autenticado.

### Comportamiento final
- requiere JWT
- devuelve únicamente matches donde participa el `player_profile` autenticado
- ordenados por `scheduledAt` descendente
- incluye:
  - `match id`
  - `status`
  - `scheduledAt`
  - `clubId`
  - `locationText`
  - `notes`
  - `currentPlayerCount`
  - participantes
  - equipos asignados si existen
  - resumen de resultado si existe
  - `authenticatedPlayerIsParticipant`
  - `authenticatedPlayerTeam`
  - `authenticatedPlayerWon` cuando el resultado está `CONFIRMED` y puede derivarse limpiamente

Archivos principales:
- `src/main/java/com/sentimospadel/backend/match/dto/PlayerMatchHistoryEntryResponse.java`
- `src/main/java/com/sentimospadel/backend/match/service/PlayerMatchHistoryService.java`
- `src/main/java/com/sentimospadel/backend/player/controller/PlayerProfileController.java`

No se agregaron filtros (`status`, `scope`) en esta pasada para no ensuciar la API antes de ver la necesidad real del frontend.

## 7. Tests agregados o ajustados
Se agregó cobertura real durante el día en tres niveles.

### Unit/service tests
- `src/test/java/com/sentimospadel/backend/rating/service/RatingCalculationServiceTest.java`
- `src/test/java/com/sentimospadel/backend/rating/service/RatingApplicationServiceTest.java`
- `src/test/java/com/sentimospadel/backend/match/service/MatchServiceTest.java`
- `src/test/java/com/sentimospadel/backend/match/service/PlayerMatchHistoryServiceTest.java`

### Controller tests
- `src/test/java/com/sentimospadel/backend/rating/controller/RankingControllerTest.java`
- `src/test/java/com/sentimospadel/backend/player/controller/PlayerProfileControllerTest.java`

### Integración con DB real de tests
Archivo clave:
- `src/test/java/com/sentimospadel/backend/rating/integration/ConfirmedResultRatingFlowIntegrationTest.java`

Setup:
- `@SpringBootTest`
- perfil `test`
- H2 en memoria
- Flyway real corriendo desde cero

Cobertura lograda:
- las migraciones cargan bien
- un resultado `CONFIRMED` actualiza `current_rating`
- se crean filas en `player_rating_history`
- aumenta `rated_matches_count`
- la confirmación repetida no vuelve a aplicar rating

### Ajustes de infraestructura de tests
Archivos:
- `pom.xml`
- `src/test/resources/application-test.yml`
- `src/test/java/com/sentimospadel/backend/BackendApplicationTests.java`

Durante este bloque también se corrigieron varias incompatibilidades de migraciones para que Flyway funcionara igual en PostgreSQL y H2:
- `V2__add_initial_onboarding_survey.sql`
- `V6__add_match_teams_and_result_confirmation.sql`
- `V7__add_match_result_rejection.sql`
- `V8__align_player_rating_scale_and_history.sql`

## 8. Documentación actualizada
Se actualizaron:
- `README.md`
- `docs/progress.md`

El README quedó alineado con:
- la escala oficial `1.00..7.00`
- endpoints de ranking
- endpoints de rating history
- endpoint `GET /api/players/me/matches`

`docs/progress.md` quedó corregido para reflejar el estado final del día, sin dejar como “pendiente” algo que ya se implementó.

## 9. Estado final del backend al cierre del día
Al cierre de hoy, el backend quedó así:

- Auth estable:
  - register
  - login
  - JWT
  - `/api/auth/me`
- Onboarding estable:
  - survey autenticada
  - rating inicial `1.00..7.00`
  - categoría Uruguay
- Social matches estabilizados:
  - create
  - join
  - leave
  - cancel
  - teams
  - result submit
  - result confirm
  - result reject
  - controlled resubmit
- Rating operativo:
  - misma escala oficial `1.00..7.00`
  - aplicado solo con resultados `CONFIRMED`
  - historial persistido
  - protección contra doble aplicación
  - `GET /api/rankings`
- Lecturas player-facing:
  - `GET /api/players/me/rating-history`
  - `GET /api/players/{id}/rating-history`
  - `GET /api/players/me/matches`

Torneos siguen fuera de alcance. Tampoco se agregó club verification workflow, reservas ni pagos.

## 10. Próximos pasos recomendados para mañana
Orden sugerido:

1. Agregar filtros livianos a `GET /api/players/me/matches`
   - opción más probable: `scope=upcoming|completed|cancelled|pending_result`
   - solo si el frontend ya lo necesita

2. Agregar navegación/lecturas complementarias entre match history y match detail
   - si producto necesita profundizar desde la lista del usuario

3. Recién después abrir foundations de tournament
   - no mezclar torneos antes de consolidar bien la lectura player-facing de matches y rating

4. Mantener el rating sin cambios de fórmula
   - la base ya quedó estable sobre `CONFIRMED` social matches
