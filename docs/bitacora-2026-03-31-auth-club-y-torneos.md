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
