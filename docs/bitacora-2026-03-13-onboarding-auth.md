# Bitácora Técnica - 2026-03-13 - Continuación de Auth + Onboarding

## Contexto al inicio de la sesión
- El backend ya tenía implementado el primer slice de autenticación.
- Estado de auth existente antes del trabajo de hoy:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - soporte de JWT access token
  - `GET /api/auth/me`
- Registro, login, JWT y el endpoint de usuario autenticado ya estaban funcionando y se mantuvieron intactos durante esta sesión.
- El repositorio ya tenía:
  - Flyway como mecanismo de migraciones
  - Spring Security configurado
  - manejo global de errores
  - módulos base para `auth`, `user`, `player` y `club`

Archivos relevantes ya existentes al comenzar:
- `src/main/java/com/sentimospadel/backend/auth/controller/AuthController.java`
- `src/main/java/com/sentimospadel/backend/auth/service/AuthService.java`
- `src/main/java/com/sentimospadel/backend/auth/service/JwtService.java`
- `src/main/java/com/sentimospadel/backend/config/security/SecurityConfig.java`
- `src/main/resources/db/migration/V1__init_core_schema.sql`

## Requisito de producto introducido hoy
- Agregar el flujo de encuesta inicial de onboarding como paso autenticado posterior al registro.
- Mantener onboarding separado de register.
- Persistir:
  - respuestas crudas de la encuesta
  - resultado calculado del onboarding
  - resultado actual espejado en `player_profiles`
- No agregar todavía:
  - lógica de ranking
  - lógica de partidos
  - lógica de torneos
  - workflow real de verificación por club

Endpoints pedidos:
- `POST /api/onboarding/initial-survey`
- `GET /api/onboarding/initial-survey`

## Decisiones de diseño tomadas al principio
1. Mantener onboarding separado de register.
   - Register sigue creando solamente el `User`.
   - La encuesta inicial queda como paso autenticado posterior.
   - No se mezcló onboarding con register ni se tocó la semántica de login/JWT.

2. Persistir onboarding en una tabla dedicada con historial.
   - Las respuestas crudas no debían ir dentro de `users`.
   - Tampoco se comprimieron en un blob opaco.
   - Se eligió una tabla explícita `initial_survey_submissions` para dejar el dominio claro y habilitar a futuro versionado o historial de reenvíos.

3. Espejar únicamente el resultado actual en `player_profiles`.
   - `player_profiles` sigue siendo el perfil de negocio actual del jugador.
   - El historial de la encuesta queda aparte.
   - El resultado vigente se replica en `player_profiles` para simplificar lecturas futuras.

## Cambios de persistencia - primera migración de onboarding
Se agregó:
- `src/main/resources/db/migration/V2__add_initial_onboarding_survey.sql`

Esta migración introdujo:

### Tabla `initial_survey_submissions`
Campos:
- `id`
- `player_id`
- `survey_version`
- `q1`..`q10`
- `weighted_score`
- `normalized_score`
- `initial_rating`
- `estimated_category`
- `requires_club_verification`
- `created_at`
- `updated_at`

Notas:
- Las respuestas se guardan explícitamente como `A`..`E`.
- `weighted_score` se persiste como entero.
- `normalized_score` e `initial_rating` se persisten como numéricos.
- `estimated_category` se persiste como string de enum.
- `requires_club_verification` queda guardado por submission.

### Tabla `player_profiles`
Campos agregados inicialmente:
- `survey_completed`
- `survey_completed_at`
- `initial_rating`
- `estimated_category`
- `requires_club_verification`
- `club_verification_status`
- `public_category_visible`

Importante:
- `public_category_visible` apareció por una interpretación inicial que luego resultó incorrecta.
- Ese campo fue eliminado en una migración de cleanup hecha más tarde en la misma sesión.

## Modelado de dominio agregado
Se creó el módulo `onboarding` bajo:
- `src/main/java/com/sentimospadel/backend/onboarding`

Archivos nuevos:
- `enums/AnswerOption.java`
- `entity/InitialSurveySubmission.java`
- `repository/InitialSurveySubmissionRepository.java`
- `dto/InitialSurveyRequest.java`
- `dto/InitialSurveyResponse.java`
- `service/InitialSurveyCalculationResult.java`
- `service/InitialSurveyCalculationService.java`
- `service/OnboardingService.java`
- `controller/OnboardingController.java`

Enums de apoyo agregados en `player/enums`:
- `UruguayCategory.java`
- `ClubVerificationStatus.java`

Cambios de soporte en player:
- `player/entity/PlayerProfile.java`
- `player/dto/PlayerProfileResponse.java`
- `player/service/PlayerProfileService.java`
- `player/repository/PlayerProfileRepository.java`

## Reglas de cálculo de onboarding implementadas
La lógica de cálculo quedó aislada en:
- `src/main/java/com/sentimospadel/backend/onboarding/service/InitialSurveyCalculationService.java`

### Mapeo de respuestas
- `A = 0`
- `B = 1`
- `C = 2`
- `D = 3`
- `E = 4`

### Pesos por pregunta
- `Q1 = 5`
- `Q2 = 4`
- `Q3 = 4`
- `Q4 = 3`
- `Q5 = 3`
- `Q6 = 6`
- `Q7 = 4`
- `Q8 = 4`
- `Q9 = 2`
- `Q10 = 5`

### Regla anti-inflación de Q9
- `Q9` cuenta completo solo si `Q6 >= 2`
- Si `Q6 < 2`, entonces:
  - `effectiveQ9 = min(Q9, 1)`

### Weighted score
- `S = Σ(weight_i * effectiveValue_i)`
- Implementado con aritmética entera

### Normalized score
- `S40 = S / 4`
- Persistido como `normalized_score`

### Fórmula piecewise del rating
Implementada exactamente como fue pedida:
- Si `S40 <= 10`
  - `R = 1.00 + (S40 / 10) * 1.40`
- Si `10 < S40 <= 24`
  - `R = 2.40 + ((S40 - 10) / 14) * 2.30`
- Si `24 < S40 <= 35`
  - `R = 4.70 + ((S40 - 24) / 11) * 1.20`
- Si `S40 > 35`
  - `R = 5.90 + ((S40 - 35) / 5) * 1.10`

Redondeo:
- El rating final se redondea a 2 decimales

### Gate de Primera
Para permitir `R >= 6.40`, deben cumplirse ambas:
- `Q10 >= 3`
- `Q6 >= 3`

Si no:
- `R = min(R, 6.39)`

### Gate de Segunda
Para permitir `R >= 5.50`, debe cumplirse:
- `Q6 >= 2`

Si no:
- `R = min(R, 5.49)`

### Mapeo rating -> categoría Uruguay
Se implementó con enum explícito:
- `PRIMERA`: `6.40 <= R <= 7.00`
- `SEGUNDA`: `5.50 <= R < 6.40`
- `TERCERA`: `4.80 <= R < 5.50`
- `CUARTA`: `4.10 <= R < 4.80`
- `QUINTA`: `3.40 <= R < 4.10`
- `SEXTA`: `2.60 <= R < 3.40`
- `SEPTIMA`: `1.00 <= R < 2.60`

## API autenticada de onboarding agregada
Nuevo controller:
- `src/main/java/com/sentimospadel/backend/onboarding/controller/OnboardingController.java`

Endpoints agregados:
- `POST /api/onboarding/initial-survey`
- `GET /api/onboarding/initial-survey`

Cambio de seguridad:
- `src/main/java/com/sentimospadel/backend/config/security/SecurityConfig.java`
- Se agregó `/api/onboarding/**` a la lista de rutas autenticadas junto con `/api/auth/me`

### Comportamiento de POST
- Requiere JWT/Bearer válido
- Recibe `q1`..`q10`
- Valida que todas las respuestas estén presentes
- Payload inválido o enum inválido responde `400`
- Calcula el resultado
- Persiste la submission
- Espeja el resultado actual en `player_profiles`
- Devuelve `201 Created`
- Un segundo envío de la encuesta inicial devuelve `409`

### Comportamiento de GET
- Requiere JWT/Bearer válido
- Devuelve el último resultado guardado del usuario autenticado
- Si no existe onboarding aún, devuelve `404`

## Decisión sobre creación de PlayerProfile durante onboarding
Problema detectado:
- El diseño actual no garantizaba que todo usuario autenticado tuviera ya un `PlayerProfile`.

Decisión:
- No tocar register
- Resolverlo de la forma mínima dentro de onboarding

Implementado en:
- `src/main/java/com/sentimospadel/backend/onboarding/service/OnboardingService.java`

Comportamiento:
- Al enviar onboarding, el servicio resuelve el usuario autenticado usando el email del JWT
- Busca el `PlayerProfile` asociado
- Si no existe, lo crea de forma lazy con defaults mínimos

Defaults usados:
- `fullName` derivado de la parte local del email
- `currentElo = 1200`
- `provisional = true`
- `matchesPlayed = 0`
- flags de onboarding inicializados de forma consistente

Motivo:
- Esto destraba onboarding sin mezclarlo con registro
- Mantiene el cambio acotado y evita inventar en esta sesión un lifecycle de player más grande

## Ajustes de manejo de errores
Se actualizó:
- `src/main/java/com/sentimospadel/backend/shared/exception/GlobalExceptionHandler.java`

Cambio:
- Se agregó manejo de payload JSON mal formado para que valores de enum inválidos o cuerpos incorrectos respondan `400 Bad Request` y no caigan en un `500` genérico

## Tests agregados y ajustados
Se agregaron:
- `src/test/java/com/sentimospadel/backend/onboarding/service/InitialSurveyCalculationServiceTest.java`
- `src/test/java/com/sentimospadel/backend/onboarding/service/OnboardingServiceTest.java`
- `src/test/java/com/sentimospadel/backend/onboarding/controller/OnboardingControllerTest.java`

Cobertura agregada:
- mapeo de respuestas
- cálculo de weighted score
- regla anti-inflación de Q9
- gate de Primera
- gate de Segunda
- mapeo de rating
- mapeo de categoría Uruguay
- flujo de persistencia del servicio de onboarding
- comportamiento autenticado del controller

Verificación de build:
- `.\mvnw.cmd test`
- La sesión cerró con tests pasando

## Aclaración de producto a mitad de la sesión
Interpretación inicial usada más temprano:
- `PRIMERA` y `SEGUNDA` requerían verificación
- ese requerimiento se interpretó primero como ocultar la categoría públicamente
- por eso apareció `public_category_visible` / `publicCategoryVisible`

Aclaración de producto posterior:
- el rating siempre es visible
- la estimated category también es visible
- `PRIMERA` y `SEGUNDA` siguen requiriendo verificación por club
- la representación correcta es:
  - `requiresClubVerification = true`
  - `clubVerificationStatus = PENDING`
- el requerimiento de verificación no oculta ni rating ni categoría

## Cleanup posterior a la aclaración
Para alinear el backend con la regla correcta:

### Migración de cleanup
Se agregó:
- `src/main/resources/db/migration/V3__remove_public_category_visibility.sql`

Esta migración elimina:
- `player_profiles.public_category_visible`

### Limpieza de código
Se removió `publicCategoryVisible` de:
- `player/entity/PlayerProfile.java`
- `player/dto/PlayerProfileResponse.java`
- `player/service/PlayerProfileService.java`
- `onboarding/dto/InitialSurveyResponse.java`
- `onboarding/service/InitialSurveyCalculationResult.java`
- `onboarding/service/InitialSurveyCalculationService.java`
- `onboarding/service/OnboardingService.java`

### Lógica final de verificación
Regla final implementada al cierre de la sesión:
- Si la categoría estimada es `PRIMERA` o `SEGUNDA`
  - `requiresClubVerification = true`
  - `clubVerificationStatus = PENDING`
- En caso contrario
  - `requiresClubVerification = false`
  - `clubVerificationStatus = NOT_REQUIRED`

Importante:
- `initialRating` sigue visible
- `estimatedCategory` sigue visible
- ya no queda ningún flag de visibilidad ocultando la categoría

## Documentación actualizada durante la sesión
Se actualizaron:
- `README.md`
- `docs/progress.md`

README quedó documentando:
- endpoints de onboarding
- separación entre onboarding y register
- modelo de persistencia de respuestas + resultado calculado
- visibilidad del rating y categoría
- semántica de verificación para `PRIMERA` / `SEGUNDA`

## Estado final del backend al cierre del día
- Auth sigue intacto:
  - register funciona
  - login funciona
  - JWT access token funciona
  - `/api/auth/me` funciona
- Onboarding quedó implementado como flujo autenticado posterior al registro
- El backend persiste:
  - respuestas crudas de la encuesta
  - resultado calculado del onboarding
  - estado actual de onboarding espejado en `player_profiles`
- La fórmula de rating y los thresholds de categoría quedaron implementados exactamente según lo pedido
- `PRIMERA` y `SEGUNDA` son visibles y quedan con verificación `PENDING`
- `TERCERA` a `SEPTIMA` usan `NOT_REQUIRED`
- No se agregó lógica de ranking
- No se agregó lógica de partidos
- No se agregó lógica de torneos
- No se agregó el workflow real de verificación por club
- `publicCategoryVisible` fue removido porque representaba una interpretación incorrecta
- La suite de tests termina pasando

## Próximos pasos recomendados para la siguiente sesión
1. Hacer explícita la creación de `PlayerProfile` dentro del lifecycle autenticado del usuario en lugar de depender de creación lazy durante onboarding.
2. Agregar tests de integración con persistencia real para onboarding y validación de migraciones con Flyway.
3. Definir el workflow futuro de verificación por club:
   - quién verifica
   - qué evidencia se requiere
   - cómo transicionan `PENDING`, `VERIFIED` y `REJECTED`
4. Después de estabilizar player/profile/onboarding, avanzar a modelado de partidos y resultados.
5. Mantener diferido el trabajo de ranking/ELO hasta que exista persistencia clara de resultados.
