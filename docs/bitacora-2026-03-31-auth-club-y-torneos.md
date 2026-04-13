# Bitácora 2026-03-31

## Qué quedó cerrado

### 1. Split real entre cuentas Persona y Club
- El login/registro ya distingue entre:
  - `PLAYER`
  - `CLUB` / `ADMIN`
- El backend devuelve `managedClubId` y `managedClubName` en auth.
- El frontend ya monta shells separados:
  - jugador: experiencia jugador
  - club: experiencia club
- Se decidió explícitamente trabajar con cuentas separadas, no con cambio de contexto dentro de la misma cuenta.

### 2. Panel real de club
- `Club View` quedó backend-driven.
- El admin del club ya tiene:
  - dashboard
  - usuarios
  - agenda
  - acciones operativas de agenda
- Backend usa `managed_club_id` para resolver qué club administra cada cuenta club.

### 3. Torneos ELIMINATION
- `ELIMINATION` quedó backend-driven.
- Ya soporta:
  - create
  - join / leave
  - launch
  - grupos
  - standings por grupo
  - matches generados
  - submit / confirm / reject
  - avance automático de playoff

### 4. Torneos AMERICANO fijo
- `AMERICANO fijo` quedó backend-driven.
- Ya soporta:
  - create
  - launch
  - generated matches
  - standings
  - submit / confirm / reject
  - pending actions

### 5. Torneos AMERICANO dinámico
- `AMERICANO dinámico` quedó backend-driven.
- Se agregó `entryKind` para distinguir:
  - `REGISTERED`
  - `GENERATED_MATCH_PAIR`
- El backend ahora genera parejas dinámicas oficiales por partido sin mezclar eso con el roster oficial del torneo.
- Ya soporta:
  - create
  - launch
  - generated matches
  - standings individuales
  - submit / confirm / reject
  - pending actions

### 6. Selección de jugadores en torneos
- Se cerró el gap de fallback con `MOCK_FRIENDS` en el path oficial.
- Crear torneo y agregar equipos ahora usan roster real cargado desde backend.
- Si no carga el roster, el fallback mínimo es solo el usuario autenticado, no amigos inventados.

## Qué quedó validado

### Backend
- `.\mvnw.cmd test`
- Resultado final de la última corrida: `118` tests green

### Frontend
- `npm run lint`
- `npm run build`
- Ambas pasaron después del cierre de `AMERICANO dinámico` y después de quitar el fallback mock de jugadores en torneos

## Qué sigue pendiente para equilibrar front y back

Lo más importante que sigue pendiente hoy:

1. Preview local de launch de torneos
- El launch real ya lo decide backend.
- La preview previa al launch sigue siendo presentacional/local.

2. Extras premium/demo todavía visibles
- Hay que revisar qué bloques visibles siguen apoyados en lógica demo y decidir:
  - implementar backend real
  - o bajar/ocultar

3. Club verification workflow
- Existen flags reales en onboarding y perfil.
- Falta el workflow operativo real de verificación.

4. Clases
- Todavía no existe dominio real de clases.
- Si mañana se decide abrir eso, hay que modelarlo desde backend.

## Próximo paso recomendado

Orden recomendado para seguir mañana:

1. decidir si cerramos primero:
   - `club verification`
   - o `CRUD de canchas para club`
2. después revisar extras premium/demo visibles
3. dejar QA seeds de Americano para probar sin armar torneos desde UI

## Nota operativa
- El core visible de jugador, club admin y torneos ya quedó mucho más real.
- Lo que conviene evitar mañana es abrir features nuevas sin cerrar antes los huecos visibles que todavía parezcan producto real.

## Update posterior

### 7. Club verification workflow
- `club verification` ya quedó backend-driven.
- Se agregó dominio real de solicitudes de verificación por club.
- El jugador ahora tiene path oficial para:
  - ver estado real
  - solicitar verificación a un club real
  - ver historial reciente de solicitudes
- El club admin ahora tiene path oficial para:
  - listar solicitudes
  - aprobar
  - rechazar
- Aprobar/rechazar actualiza el estado oficial en `player_profiles.club_verification_status`.

### Backend
- Nuevo workflow real:
  - `GET /api/players/me/club-verification`
  - `POST /api/players/me/club-verification/request`
  - `GET /api/clubs/me/management/verification-requests`
  - `POST /api/clubs/me/management/verification-requests/{id}/approve`
  - `POST /api/clubs/me/management/verification-requests/{id}/reject`

### Frontend
- Perfil de jugador ahora consume ese workflow real para la categoría alta.
- `Club View` ahora expone una cola real de verificaciones.

### Validación
- `.\mvnw.cmd clean test`
- Resultado final: `127` tests green
- `npm run lint`
- `npm run build`
- Ambas pasaron después del cierre de `club verification`

## Próximo paso recomendado ahora

1. `CRUD de canchas para club`
2. revisión de extras premium/demo todavía visibles
3. preview de launch de torneos desde backend
4. QA seeds de Americano

## Update cierre de canchas

### 8. CRUD de canchas para club
- `CRUD de canchas para club` ya quedo backend-driven.
- El club admin ahora tiene path oficial para:
  - listar canchas activas e inactivas
  - crear canchas
  - editar nombre y tarifa
  - desactivar / reactivar
  - reordenar
- La agenda sigue consumiendo solo canchas activas reales.
- Se agregaron guardas de backend para no romper verdad oficial:
  - no se puede desactivar una cancha con reservas futuras
  - no se puede renombrar una cancha si eso romperia partidos reales futuros resueltos por `locationText`

### Backend
- Nuevo contrato real:
  - `GET /api/clubs/me/management/courts`
  - `POST /api/clubs/me/management/courts`
  - `PUT /api/clubs/me/management/courts/{courtId}`
  - `POST /api/clubs/me/management/courts/reorder`

### Frontend
- `Club View` ahora expone una pantalla real de `Canchas`.
- El dashboard del club reemplaza el acceso muerto de `Torneos` por el flujo real de configuracion de canchas.

### Validacion
- `.\mvnw.cmd clean test`
- Resultado final: `136` tests green
- `npm run lint`
- `npm run build`
- Ambas pasaron despues del cierre de `CRUD de canchas para club`

## Proximo paso recomendado desde ahora

1. revision de extras premium/demo todavia visibles
2. preview de launch de torneos desde backend
3. QA seeds de Americano
4. evaluar si `clases` entra o no como siguiente dominio real

## Update auth hardening

### 9. Registro con telefono unico y verificacion de email
- `register` ya no activa la cuenta automaticamente.
- Toda cuenta nueva queda en `PENDING_EMAIL_VERIFICATION` hasta confirmar el correo.
- Se agrego `phone` obligatorio para `PLAYER` y `CLUB`.
- Backend ahora rechaza duplicados tanto por `email` como por `phone`.
- El backend genera token de verificacion con expiracion, guarda solo hash y expone:
  - `GET /api/auth/verify-email?token=...`
  - `POST /api/auth/verify-email/resend`
- Se agrega envio de mail real cuando haya SMTP configurado.
- En local/dev queda soporte `log-only` para no bloquear el flujo.
- Las cuentas `PLAYER` ahora crean `player_profile` inicial en el registro para que el nombre oficial quede backend-driven desde el principio.

### Frontend
- El registro ahora exige telefono y deja de auto-loguear luego de crear la cuenta.
- La pantalla de login ahora muestra el estado pendiente y deja reenviar el correo de confirmacion.

### Buscar jugador
- En torneos, `Buscar jugadores...` funciona hoy filtrando en frontend sobre roster cargado desde backend (`GET /api/players`).
- En `Clubs`, el affordance de `Buscar` para invitar jugadores sigue sin ser real y todavia depende de `MOCK_FRIENDS`.

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `141` tests green
- `npm run lint`
- `npm run build`
- Ambas pasaron despues del cierre de este slice

## Update club booking publico

### 10. Reserva de club con disponibilidad real
- El flujo publico de `Clubs` ya no depende de slots ni canchas mockeadas para reservar.
- Backend ahora expone disponibilidad real por club y fecha en:
  - `GET /api/clubs/{id}/booking-availability?date=YYYY-MM-DD`
- La respuesta publica incluye:
  - club
  - fecha
  - canchas activas
  - tarifa por hora
  - slots oficiales
  - estado `AVAILABLE | RESERVED | BLOCKED`
- `POST /api/matches` ahora valida reservas de club reales:
  - el club debe existir
  - la cancha debe ser una cancha activa real del club
  - la hora debe coincidir con el grid oficial del club
  - el slot no puede estar bloqueado
  - el slot no puede estar reservado por agenda o por partido real existente

### Frontend
- `Clubs` ahora consume disponibilidad real desde backend antes de confirmar reserva.
- El buscador de clubes ya filtra por nombre o zona.
- Se removieron del flujo de booking las opciones que todavia no tienen backend oficial:
  - invitaciones
  - lobby privado
  - toggle de pago
  - nivel admitido
  - posicion
- La reserva publica del MVP ahora crea un partido real de 4 jugadores sobre una cancha y horario oficiales.

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `145` tests green
- `npm run lint`
- `npm run build`
- Ambas pasaron despues del cierre de este slice

## Update legal y archive de torneos

### 11. Registro con aprobacion legal persistida
- El registro ahora exige aprobacion explicita de:
  - `Terminos y Condiciones`
  - `Politica de Privacidad y Tratamiento de Datos`
- Tambien persiste preferencias opt-in para:
  - tracking de actividad
  - notificaciones operativas
- Los documentos legales vigentes ahora salen de backend en:
  - `GET /api/legal/documents`
- Backend guarda evidencia auditable en `users`:
  - version aceptada
  - timestamp de aceptacion
  - version del bundle de consentimientos
  - estado y timestamp de tracking
  - estado y timestamp de notificaciones operativas

### Frontend
- La pantalla de registro mantiene el layout base pero ahora carga los documentos legales reales desde backend.
- Se agregaron checkboxes obligatorios y opt-in opcionales.
- Los documentos se pueden abrir desde la UI antes de registrarse.

### 12. Archivar torneo ya no es solo local
- El boton visible de `Archivar torneo` ahora tiene respaldo oficial en backend.
- Nuevo endpoint:
  - `POST /api/tournaments/{id}/archive`
- `GET /api/tournaments` y el detalle ahora devuelven `archived` y `archivedAt`.
- El frontend mantiene el mismo comportamiento visual, pero ya filtra sobre estado oficial del backend.

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `146` tests green
- `npm run lint`
- `npm run build`
- Ambas pasaron despues del cierre de este slice

### Pendiente de torneos en este frente
- `launch preview` todavia sigue siendo local en frontend
- quedan ramas legacy de generacion local dentro de `App.tsx`

## Update club view: crear torneo y crear partido

### 13. Club View ahora puede disparar creacion operativa
- No hizo falta cambiar contratos backend:
  - `MatchService#createMatch` ya resolvia creador por email autenticado
  - `TournamentService#createTournament` ya resolvia creador por email autenticado
- El gap real estaba en frontend: la cuenta `club/admin` no tenia entradas visibles para usar esos flujos.

### Frontend
- `Club View` ahora expone dos acciones nuevas:
  - `Crear Partido`
  - `Crear Torneo`
- `Crear Torneo` reutiliza el modal operativo existente, pero ahora:
  - precarga la sede administrada por el club cuando existe
  - no preselecciona al admin de club como jugador participante
  - hidrata roster y clubes reales tambien para cuentas `club`
- `Crear Partido` reutiliza `ClubsBookingView`, pero ahora:
  - agrega boton de vuelta a `Club View`
  - prioriza la sede administrada por el club
  - vuelve al dashboard del club despues de confirmar la reserva

### Validacion
- `npm run lint`
- `npm run build`

## Update eliminatoria competitiva: rating oficial backend-driven

### 20. Eliminatoria competitiva ya no miente sobre "por los puntos"
- Se cerrÃ³ el gap principal del flujo `Grupos + Clasificados + EliminaciÃ³n`.
- Hasta este slice, el frontend podÃ­a mostrar un torneo de eliminaciÃ³n como `Por los puntos`, pero el backend no aplicaba rating oficial al confirmar resultados.
- Ahora el contrato queda alineado:
  - `ELIMINATION + competitive=true` actualiza rating oficial
  - `ELIMINATION + competitive=false` no actualiza rating oficial
  - `LEAGUE` y `AMERICANO` siguen sin impactar rating oficial

### Backend
- Nuevo slice de persistencia:
  - `tournament_match_results` ahora guarda `rating_applied` y `rating_applied_at`
  - `player_rating_history` ahora soporta historial proveniente de `match_id` o de `tournament_match_id`
- Nuevo servicio:
  - `TournamentRatingApplicationService`
- Regla aplicada:
  - solo al confirmar resultados de cruces de eliminatoria competitiva
  - requiere equipos confirmados `2 vs 2`
  - reusa la misma fÃ³rmula oficial de rating que los partidos sociales
- El historial oficial de rating del jugador ahora tambiÃ©n puede devolver entradas originadas en cruces de torneo.

### Frontend
- `TournamentStatusView` ahora muestra una nota honesta sobre si ese torneo afecta o no el rating oficial.
- El historial de rating del perfil ya puede renderizar entradas provenientes de partidos sociales y de `tournament matches` competitivos sin depender de heurÃ­sticas locales.
- La tarjeta fallback del historial distingue mejor un cruce de torneo de un partido social.

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `193` tests green
- `npm run lint`
- `npm run build`

## Update mobile packaging: iOS plug-and-play y guias de ambiente

### 24. iOS queda preparado a nivel repo
- Se genero `frontend/ios` para que el wrapper mobile ya no quede solo en Android.
- El entorno Windows sigue sin poder compilar iOS de punta a punta, pero el repo ya queda listo para que en una Mac sea abrir, sincronizar y correr.

### Frontend
- `Info.plist` ahora declara:
  - custom scheme `sentimospadel://app`
  - permisos de camara y libreria de fotos para subida de avatar
  - `NSAllowsArbitraryLoadsInWebContent` para QA local con backend HTTP
- `AndroidManifest.xml` tambien declara el custom scheme `sentimospadel://app`
- `tsconfig.json` ahora excluye `android/`, `ios/` y `dist/` para que Capacitor no rompa `tsc --noEmit`
- `frontend/README.md` dejo de apuntar a AI Studio/Gemini y ahora refleja el setup real del proyecto
- Se agrego `frontend/docs/mobile-packaging.md`

### Release / testing docs
- Se agrego `backend/docs/branching-and-environments-guide.md`
- Se agrego `backend/docs/ceo-local-setup-and-demo-guide.md`
- Se agrego `backend/docs/frontend-backend-mobile-release-alignment.md`
- Recomendacion operativa:
  - `develop` para seguir iterando
  - `staging` para QA / CEO
  - `main` para lo realmente aprobado

### Validacion
- `npm run lint`
- `npm run build`
- `npx cap sync android`
- `npx cap sync ios`

## Update Capacitor: arranque real del empaquetado mobile

### 21. Scaffold mobile inicial sobre el frontend backend-driven
- Se iniciÃ³ el empaquetado mobile con `Capacitor` sin cambiar contratos de negocio.
- El objetivo de este slice fue dejar lista la base para abrir `Android Studio` y empezar pruebas reales en device/emulador.

### Frontend
- Dependencias agregadas:
  - `@capacitor/core`
  - `@capacitor/cli`
  - `@capacitor/android`
  - `@capacitor/ios`
  - `@capacitor/app`
- Config nueva:
  - `capacitor.config.ts`
  - scripts `cap:*` en `package.json`
  - `.env.example` con `VITE_API_BASE_URL`
- Proyecto nativo generado:
  - `frontend/android`
- Deep links:
  - se agregÃ³ bridge nativo liviano para que `matchInvite` y `tournamentInvite` sigan usando el flujo actual aunque la app abra desde `appUrlOpen`
  - `index.tsx` ahora sincroniza la URL nativa inicial y registra listener de apertura
- API mobile-safe:
  - `backendApi.ts` ahora centraliza mejor el fallback de `VITE_API_BASE_URL`
  - en builds no-dev sin env definida, deja warning explÃ­cito

### Backend
- `SecurityConfig` ahora acepta origins usados por `Capacitor`:
  - `http://localhost`
  - `https://localhost`
  - `capacitor://localhost`
  - `ionic://localhost`

### Salvedades
- `iOS` quedÃ³ preparado a nivel dependencias/scripts, pero no se generÃ³ ni validÃ³ build nativo local porque este entorno sigue siendo Windows.
- Push nativo todavÃ­a no estÃ¡ cableado al shell mobile; la infraestructura backend ya existe y queda para el siguiente slice.

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `193` tests green
- `npm run lint`
- `npm run build`
- `npx cap add android`
- `npx cap sync android`

## Update perfil jugador: upload real de foto

### 20. La foto del perfil ya no depende de URL manual
- Quedo implementado el upload real de foto para el perfil del jugador.
- El backend sigue siendo la verdad oficial del avatar.
- El frontend mantiene la misma experiencia general de `Editar perfil`, pero ahora permite seleccionar archivo, previsualizarlo y guardarlo como media gestionada por el sistema.

### Backend
- Nuevo contrato:
  - `POST /api/players/me/photo`
- Nuevo endpoint publico para servir fotos gestionadas:
  - `GET /api/player-profile-photos/{filename}`
- Reglas activas:
  - solo acepta `JPG`, `PNG` o `WEBP`
  - maximo `5 MB`
  - si el jugador reemplaza la foto, se limpia la foto gestionada anterior
  - si el jugador elimina la foto desde perfil, tambien se limpia el archivo gestionado anterior cuando corresponde
- Se agrego configuracion dedicada de storage para fotos de perfil y se explicitaron valores de test para no romper el boot del backend

### Frontend
- `Editar perfil` ya no depende solo de `photoUrl`
- Ahora permite:
  - seleccionar archivo local
  - ver preview antes de guardar
  - quitar la foto actual
- El cliente API ya soporta `multipart/form-data` sin forzar `Content-Type: application/json`
- Despues de guardar:
  - refresca el perfil oficial desde backend
  - evita dejar el avatar parcheado solo en estado local

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `184` tests green
- `npm run lint`
- `npm run build`

## Update player profile: alta oficial desde registro y render backend-driven

### 20. Perfil jugador alineado con datos oficiales
- El perfil de jugador ahora queda mucho mas completo desde el registro inicial, en vez de depender de defaults heredados del frontend.
- El backend sigue siendo el source of truth para nombre, foto, posicion, nivel declarado, ciudad, rating y club representado.

### Backend
- `RegisterRequest` para cuentas `PLAYER` ahora admite:
  - `photoUrl`
  - `preferredSide`
  - `declaredLevel`
  - `city`
  - `representedClubId`
- `AuthService` ahora persiste esos datos en `player_profiles` al crear la cuenta jugador.
- Se agrego `represented_club_id` a `player_profiles` con FK real hacia `clubs`.
- `GET /api/players/me` y `GET /api/players/{id}` ahora devuelven:
  - `representedClubId`
  - `representedClubName`

### Frontend
- `RegisterView` ahora pide para jugador:
  - ciudad
  - posicion
  - nivel declarado
  - club representado opcional
  - URL de foto opcional
- `buildFrontendUser` ahora hidrata avatar y metadata del jugador desde `PlayerProfileResponse`, no desde defaults del mock base.
- `ProfileView` ahora muestra en cards dedicadas:
  - posicion
  - nivel declarado
  - ciudad
  - club representado
- `PublicProfileView` ahora intenta leer el perfil oficial con `GET /api/players/{id}` cuando hay `backendPlayerProfileId`, para no quedarse solo con el snapshot parcial del match o ranking.

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `179` tests green
- `npm run lint`
- `npm run build`

## Update player profile: edicion backend-driven desde la pantalla de perfil

### 21. Editar perfil jugador
- Quedo implementada la edicion del perfil del jugador de punta a punta, manteniendo al backend como fuente de verdad.
- El frontend ya no necesita asumir metadata local para actualizar nombre, foto o club representado.

### Backend
- Se agrego `PUT /api/players/me`.
- El payload oficial permite actualizar:
  - `fullName`
  - `photoUrl`
  - `preferredSide`
  - `declaredLevel`
  - `city`
  - `representedClubId`
  - `bio`
- `PlayerProfileService` ahora resuelve y persiste el `representedClubId` oficial contra `clubs`.
- `PlayerProfileRepository` ya carga `representedClub` junto al perfil para evitar respuestas incompletas.

### Frontend
- `ProfileView` ahora expone `Editar perfil`.
- Se agrego `PlayerProfileEditView` como formulario dedicado para editar:
  - nombre
  - foto
  - posicion
  - nivel declarado
  - ciudad
  - club representado
  - bio
- Al guardar:
  - se llama a `PUT /api/players/me`
  - luego se rehidrata `currentUser` desde backend
  - se cierra el modal y se muestra feedback de exito

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `182` tests green
- `npm run lint`
- `npm run build`

## Update club booking modes: reservas directas, con confirmacion y sin reserva

### 20. Booking mode oficial por club
- Quedo implementado el modelo oficial de `3` modos de reserva para clubes:
  - `DIRECT`
  - `CONFIRMATION_REQUIRED`
  - `UNAVAILABLE`
- El backend ahora define el flujo oficial por club y el frontend solo lo refleja.
- Esto corrige el problema de asumir que todos los clubes integrados reservan con confirmacion inmediata.

### Backend
- `clubs` ahora persiste `booking_mode` mediante la migracion `V28__add_club_booking_modes.sql`.
- Se agrego el enum `ClubBookingMode` y se expone en:
  - `ClubResponse`
  - `CreateClubRequest`
  - `ClubBookingAgendaResponse`
- Backfill actual:
  - `Top Padel` queda `DIRECT`
  - `World Padel` queda `CONFIRMATION_REQUIRED`
  - clubes no integrados quedan `UNAVAILABLE`
- Las reservas publicas ya no crean siempre el mismo tipo de partido:
  - `DIRECT` crea el partido como `OPEN`
  - `CONFIRMATION_REQUIRED` crea el partido como `PENDING_CLUB_CONFIRMATION`
  - `UNAVAILABLE` rechaza la reserva desde backend
- La agenda publica y la agenda del club ahora distinguen `PENDING_CONFIRMATION`.
- Club management suma acciones oficiales para solicitudes pendientes:
  - `POST /api/clubs/me/management/booking-requests/{matchId}/approve`
  - `POST /api/clubs/me/management/booking-requests/{matchId}/reject`
- Aprobar una solicitud:
  - mueve el match a `OPEN` o `FULL`
  - convierte el slot en `RESERVED`
- Rechazar una solicitud:
  - mueve el match a `CANCELLED`
  - libera el slot
- Se agregaron notificaciones internas:
  - `CLUB_BOOKING_APPROVED`
  - `CLUB_BOOKING_REJECTED`
- Los invite links de partido ahora rechazan reservas pendientes de aprobacion.

### Frontend
- `ClubsBookingView` mantiene el flujo visible de clubes/reserva, pero ahora responde al `bookingMode` oficial:
  - reserva directa
  - solicitud de reserva
  - club visible sin reservas habilitadas
- La reserva con confirmacion ya no muestra mensaje falso de confirmacion inmediata ni intenta generar invite link en ese momento.
- `MatchCard` ya no trata un `pending_approval` como partido confirmado:
  - no permite compartir invite link
  - muestra `Cancelar solicitud` al creador
- `ClubAgendaView` ya muestra slots `Pendiente` y permite `Aprobar` o `Rechazar`.
- El inbox ya reconoce `CLUB_BOOKING_APPROVED` y `CLUB_BOOKING_REJECTED` como dominio `Reserva`.
- `MatchInvitePreviewPanel` reconoce `PENDING_CLUB_CONFIRMATION`.

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `179` tests green
- `npm run lint`
- `npm run build`

## Update frontend code health: borrado de legacy muerto en App.tsx

### 20. Limpieza estructural despues del sweep visible
- Despues del cleanup del path activo, se hizo un pass separado para borrar codigo muerto comprobado en `frontend/App.tsx`.
- El criterio fue no tocar ramas locales que todavia funcionan como compatibilidad, y si sacar todo lo que ya no tenia ninguna referencia en runtime.

### Frontend
- Se eliminaron datasets mock que ya no inicializaban estado ni alimentaban vistas activas:
  - mock de clubes legacy
  - mock de club rankings
  - `INITIAL_MATCHES`
  - `INITIAL_AGENDA`
  - `MOCK_FRIENDS`
- Se eliminaron vistas legacy no renderizadas:
  - `LegacyClubDashboardView`
  - `NationalRankingView` mock
  - `ClubsViewLegacy`

### Deuda que sigue viva a proposito
- Todavia quedan ramas locales de compatibilidad para matches o torneos no backend-driven.
- No se borraron en este pass porque siguen acopladas a estado en memoria y conviene retirarlas solo cuando se cierre ese path por alcance.

### Validacion
- `npm run lint`
- `npm run build`
- Ambas pasaron despues del cierre de este slice

## Update torneos: launch preview backend-driven

### 14. La preview de launch ya no se genera en frontend
- Nuevo endpoint autenticado:
  - `POST /api/tournaments/{id}/launch-preview`
- El backend ahora devuelve una preview oficial con:
  - grupos
  - fixture inicial
  - placeholders de playoffs cuando corresponden a reglas oficiales backend
- La pantalla `Lanzar Torneo` mantiene la UX base, pero ya no arma grupos ni partidos desde cliente.

### Backend
- `TournamentService` ahora comparte precondiciones entre preview y launch.
- Se agregaron DTOs dedicados de preview para no acoplar la UI al estado persistido.
- Se cerró una inconsistencia de eliminacion:
  - el launch ahora solo acepta `1`, `2`, `4` u `8` grupos
  - cada grupo debe poder tener al menos `2` equipos confirmados
- Esto evita previews y launches con configuraciones que el backend no podia sostener despues en playoffs.

### Frontend
- `LaunchTournamentView` ahora llama al backend para generar la preview oficial.
- El fallback de launch local quedo desactivado en runtime:
  - si un torneo no es backend-driven, el launch MVP ya no inventa fixtures locales
- La UI visible sigue mostrando:
  - grupos
  - partidos generados
  - playoffs preview
- Pero esos bloques ahora se hidratan desde backend para torneos reales.

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `150` tests green
- `npm run lint`
- `npm run build`
- Ambas pasaron despues del cierre de este slice

## Update torneos: cleanup fisico del launch local

### 15. El flujo visible de launch ya no arrastra codigo legacy activo
- `LaunchTournamentView` fue reescrito para quedarse solo con:
  - configuracion visible
  - preview oficial desde backend
  - confirmacion final contra backend
- Se removio del path activo:
  - el generador local de grupos
  - el generador local de fixture
  - el generador local de playoffs
  - la rama local inalcanzable dentro de `handleLaunchTournament`

### Frontend
- La UX visible de `Lanzar Torneo` se mantiene:
  - elegir formato
  - ajustar grupos/canchas cuando corresponde
  - ver grupos, partidos y playoffs preview
  - confirmar launch
- Pero ahora todo el flujo activo depende unicamente de:
  - `POST /api/tournaments/{id}/launch-preview`
  - `POST /api/tournaments/{id}/launch`

### Validacion
- `npm run lint`
- `npm run build`
- Ambas pasaron despues del cleanup fisico del flujo

## Update notificaciones: inbox interno ampliado para MVP

### 16. El inbox ya no vive solo de pending actions
- Hasta ahora `GET /api/notifications` solo devolvia tareas vivas de:
  - cargar resultado
  - confirmar resultado
- Eso dejaba afuera eventos ya cerrados del core, por ejemplo:
  - partido cancelado
  - resultado confirmado
  - torneo lanzado
  - verificacion aprobada o rechazada

### Backend
- Nueva migracion:
  - `V26__extend_player_notifications_for_event_inbox.sql`
- `player_notifications` ahora separa dos ciclos:
  - `managed_by_sync = true` para pending actions calculadas
  - `managed_by_sync = false` para eventos persistentes del dominio
- Se agrego `PlayerEventNotificationService` y se conecto a:
  - `MatchService`
  - `TournamentService`
  - `TournamentMatchService`
  - `ClubVerificationService`
- Eventos cubiertos en este slice:
  - `MATCH_FULL`
  - `MATCH_CANCELLED`
  - `MATCH_RESULT_CONFIRMED`
  - `MATCH_RESULT_REJECTED`
  - `TOURNAMENT_LAUNCHED`
  - `TOURNAMENT_RESULT_CONFIRMED`
  - `TOURNAMENT_RESULT_REJECTED`
  - `CLUB_VERIFICATION_APPROVED`
  - `CLUB_VERIFICATION_REJECTED`

### Frontend
- El inbox visible sigue siendo el mismo modal de `Notificaciones`.
- Ahora el frontend reconoce eventos no accionables y ya no asume que todo es:
  - `Cargar`
  - `Validar`
- Cuando corresponde:
  - abre estado del torneo
  - abre verificacion de categoria del jugador
  - o simplemente marca la notificacion como leida

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `153` tests green
- `npm run lint`
- `npm run build`
- Ambas pasaron despues del slice de notificaciones

## Update push externo: infraestructura backend-driven para Android/iOS/Web

### 17. El modelo de inbox ahora ya puede disparar delivery externo
- Se agrego infraestructura oficial para registrar instalaciones y empujar notificaciones fuera de la app sin romper el source of truth del backend.
- El inbox interno sigue siendo la verdad oficial.
- El push externo ahora queda colgado de notificaciones persistidas y del consentimiento operativo del usuario.

### Backend
- Nueva migracion:
  - `V27__add_push_device_registrations_and_deliveries.sql`
- Nuevas tablas:
  - `push_device_installations`
  - `push_notification_deliveries`
- Nuevos endpoints autenticados:
  - `GET /api/notifications/preferences`
  - `PUT /api/notifications/preferences`
  - `POST /api/notifications/devices/register`
  - `POST /api/notifications/devices/unregister`
- El backend ahora:
  - guarda instalaciones por usuario
  - permite actualizar consentimientos de tracking y notificaciones operativas
  - intenta delivery externo cada vez que una notificacion persistida se crea o vuelve a quedar activa/no leida
  - registra cada intento como `SENT`, `SKIPPED` o `FAILED`
- Reglas activas en este slice:
  - si `operational_notifications_enabled = false`, el backend no empuja y deja el delivery como `SKIPPED`
  - si no hay dispositivos activos, la notificacion queda solo en inbox interno
  - el proveedor actual es `log-only`, listo para enchufar FCM/APNs sin volver a cambiar contratos

### Frontend
- Se agregaron contratos typed en `backendApi.ts` para:
  - leer y actualizar preferencias
  - registrar y desregistrar dispositivos
- No se simulo token nativo en web:
  - no hay Capacitor
  - no hay FCM client
  - no hay APNs client
- Eso queda preparado para el wrapper mobile futuro sin contaminar la app web con fake push

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `166` tests green
- `npm run lint`
- `npm run build`

## Update match invite links: compartir partidos sociales por link oficial

### 18. Invitaciones por link antes del empaquetado mobile
- Quedo implementado el flujo MVP para invitar a otro jugador a un partido social mediante un link oficial del backend.
- El backend firma el token de invitacion y sigue siendo el source of truth del estado real del partido.
- El frontend ya no inventa links locales para este flujo.

### Backend
- Contrato nuevo:
  - `POST /api/matches/{id}/invite-link`
  - `GET /api/matches/invite?token=...`
- Regla actual:
  - solo un participante del partido puede generar el link
- El token:
  - va firmado con backend
  - expira segun configuracion
  - se resuelve a una preview liviana del partido
- La entrada final al partido sigue pasando por `POST /api/matches/{id}/join`, asi que el link nunca bypassa:
  - cupo
  - cancelacion
  - cierre del partido
  - proteccion contra duplicate join

### Frontend
- `MatchCard` ahora ofrece compartir link en partidos sociales backend-driven donde el usuario ya esta adentro y todavia quedan lugares.
- Al crear un partido desde `Clubs` o desde `Club View`, la app intenta generar el link oficial y lo deja listo para copiar/compartir sin salir del flujo.
- La app ahora detecta `?matchInvite=<token>` al cargar.
- Si el visitante no esta autenticado:
  - ve la preview del partido junto al login
- Si entra con cuenta jugador:
  - puede unirse desde esa preview
- Si entra con cuenta club:
  - la app aclara que el join requiere una cuenta jugador

### Validacion
- `.\mvnw.cmd test`
- Resultado final: `173` tests green
- `npm run lint`
- `npm run build`

## Update frontend MVP cleanup: torneos visibles sin affordances falsas

### 19. Limpieza final del path activo antes de empaquetado
- Quedo limpio el path visible de torneos sin cambiar la UX principal ni reabrir contratos backend.
- El objetivo de este slice fue sacar affordances engañosas, no hacer un refactor masivo de `App.tsx`.

### Frontend
- `CreateTournamentView` ya no copia un link falso desde cliente:
  - la card de invitacion por link se mantiene visible
  - ahora queda como placeholder honesto hasta que exista flujo oficial backend para torneos
- `CompetitionView` ya no usa wording asociado al listado estatico viejo:
  - el empty state ahora habla solo de torneos reales desde backend
  - `Ver todos` deja de comportarse como CTA vacia
- El path MVP activo de torneos queda alineado con backend para:
  - crear
  - previsualizar launch
  - lanzar
  - abrir estado
  - archivar

### Deuda explicita que sigue pendiente
- Siguen existiendo residuos legacy/mock grandes dentro de `frontend/App.tsx`:
  - `LegacyClubDashboardView`
  - `ClubsViewLegacy`
  - datasets viejos no usados en runtime activo
- Esa limpieza conviene hacerla en un pass dedicado de code health para no mezclar alcance funcional con borrado estructural.

### Validacion
- `npm run lint`
- `npm run build`
