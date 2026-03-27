# Bitácora Técnica - 2026-03-27

## Contexto del bloque de trabajo

Objetivo general del día:
- seguir sacando verdad local/mock del frontend
- pasar la mayor cantidad posible de pantallas visibles a backend-driven
- mantener la UI actual intacta
- dejar el proyecto más cerca de QA e2e real

Durante este bloque se trabajó sobre dos líneas grandes:
- desmockeo de lecturas y flujos visibles del frontend
- modelado nuevo en backend cuando la pantalla ya existía pero no había dominio real suficiente

## 1. Seed y base real para QA

Se consolidó el uso de Flyway para dejar una base local útil sin depender de cargar todo desde la UI.

Migraciones relevantes ya agregadas en este bloque de trabajo:
- `V11__seed_initial_clubs.sql`
- `V12__seed_qa_players_surveys_and_matches.sql`
- `V13__add_pending_actions_and_notifications.sql`
- `V14__seed_manual_qa_tournaments_and_result_tasks.sql`
- `V15__seed_felipe_rating_history_matches.sql`
- `V16__add_coaches.sql`
- `V17__add_club_management_foundations.sql`

Qué deja esto disponible localmente:
- catálogo real de clubes seeded desde DB
- usuarios seed con onboarding completo
- partidos sociales en estados útiles para QA
- historial real de rating para Felipe
- coaches reales
- admin de club seeded con club gestionado real

Usuarios seed clave:
- jugadores QA: varios `*.seed@sentimospadel.test`
- password común: `Qa123456!`
- admin club:
  - `club.admin@sentimospadel.test`
  - password: `Qa123456!`

## 2. Match result flow más realista

Se cerró el circuito de resultados con backend como fuente de verdad.

Cambios principales:
- elegibilidad de resultado basada en fin real del partido:
  - `scheduledAt + 90 minutos`
- ya no se considera correcto pedir resultado solo porque el social está `FULL`
- ya no se considera correcto pedir resultado solo porque un tournament match está `SCHEDULED`

Backend:
- se agregó política explícita de elegibilidad
- se agregaron `pending actions`
- se agregaron notificaciones persistentes `UNREAD/READ`

Endpoints relevantes:
- `GET /api/players/me/pending-actions`
- `GET /api/notifications`
- `POST /api/notifications/{id}/read`

Flujo resultante:
- el backend detecta cuándo corresponde pedir resultado
- genera la tarea pendiente
- genera/sincroniza notificación persistente
- el frontend reutiliza la UX ya existente de `ResultInputCard`
- submit / confirm / reject sigue unificado para social y torneo

## 3. Frontend real para ranking, rating history y match history

Se reemplazó el path oficial mock por backend real en:
- ranking nacional
- historial de rating
- historial de partidos
- agenda del usuario
- pendientes y notificaciones

Importante:
- varias constantes legacy siguen existiendo en `App.tsx`
- pero dejaron de ser la fuente oficial en los flujos principales integrados

## 4. Perfil: top partners, top rivals y club rankings

Estas tres pantallas seguían visibles en frontend pero eran mock.

Backend:
- se agregó `PlayerInsightService`
- se agregaron DTOs específicos
- se derivan desde data ya real:
  - social matches confirmados
  - participants
  - rating history
  - clubs

Endpoints nuevos:
- `GET /api/players/me/top-partners`
- `GET /api/players/me/top-rivals`
- `GET /api/players/me/club-rankings`

Frontend:
- `ProfileView`
- `TopPartnersView`
- `TopRivalsView`
- `ClubRankingsView`

Quedaron conectadas a backend sin cambiar la visual base.

## 5. Coaches

La pantalla de coaches ya existía visualmente pero era mock.

Backend:
- nuevo módulo `coach`
- entidad `Coach`
- endpoint público:
  - `GET /api/coaches`
- seed inicial con coaches reales de prueba

Frontend:
- `CoachesView` pasó a cargar desde backend
- se mantuvo la misma estructura visual de cards

## 6. Club admin context y Club View real

Este fue el bloque nuevo más grande del cierre.

### Problema detectado

El frontend ya tenía:
- `ClubDashboardView`
- `ClubUsersView`
- `ClubAgendaView`

Pero todo eso vivía de mock y el backend no sabía:
- qué usuario administra qué club
- qué canchas tiene un club
- qué slots operativos existen
- cómo persistir bloqueos o reservas manuales

### Decisión de modelado

Se eligió un primer modelo chico y explícito:
- un usuario admin puede gestionar un único club
- eso se persiste como `users.managed_club_id`

No se abrió todavía:
- multi-club admin
- pagos reales
- dashboard financiero completo

### Persistencia nueva

En `V17__add_club_management_foundations.sql` se agregaron:
- `users.managed_club_id`
- `club_courts`
- `club_agenda_slot_overrides`
- `club_activity_logs`

También se seedearon:
- canchas para los clubes
- overrides de agenda iniciales
- actividad reciente inicial
- admin de club:
  - `club.admin@sentimospadel.test`
  - club gestionado: `Top Padel`

### Backend club management

Nuevo servicio principal:
- `ClubManagementService`

Nuevos endpoints:
- `GET /api/clubs/me/management/dashboard`
- `GET /api/clubs/me/management/users`
- `GET /api/clubs/me/management/agenda?date=YYYY-MM-DD`
- `POST /api/clubs/me/management/agenda/slot-action`
- `POST /api/clubs/me/management/quick-actions`

Qué resuelven:
- summary cards del panel de club
- actividad reciente
- top users del club
- métricas de usuarios
- agenda por cancha
- acciones reservar / bloquear / liberar
- acciones rápidas del panel

### Métrica de ingresos

Todavía no existe modelo de payments.

Entonces el dashboard usa un proxy operacional:
- ingreso estimado = slots ocupados * tarifa de cancha * 1.5 horas

Se dejó así a propósito:
- suficiente para mantener la pantalla real
- sin inventar un subsistema de pagos todavía

### Frontend Club View

Se mantuvo la UI actual.

Se rewiring real en:
- `ClubDashboardView`
- `ClubUsersView`
- `ClubAgendaView`

Sin cambios de layout, navegación o jerarquía visual.

Comportamiento actual:
- si el usuario administra un club, la vista es real
- si no administra ninguno, la vista muestra estado limpio de no disponibilidad
- ya no muestra datos inventados

## 7. QA / estabilidad

Verificaciones hechas durante este bloque:

Backend:
- `.\mvnw.cmd test`
- resultado final: OK, suite pasando
- además se validó arranque real con Spring Boot y `GET /api/health`

Frontend:
- `npm run lint`
- `npm run build`
- resultado final: OK

Nota conocida:
- `vite build` dentro del sandbox sigue pudiendo dar `spawn EPERM`
- cuando pasó, se rerun fuera del sandbox y el bundle fue exitoso
- no fue un bug de la app

## 8. Qué quedó realmente backend-driven después de este bloque

Quedó real:
- auth
- onboarding
- social matches
- result submit / confirm / reject
- pending actions
- notifications persistentes
- ranking nacional
- rating history
- player match history
- top partners
- top rivals
- club rankings
- coaches
- clubs catalog
- tournament `LEAGUE`
- club admin dashboard
- club users
- club agenda

## 9. Qué sigue pendiente para mañana

Lo grande que todavía sigue pendiente de verdad:
- tournament modes beyond `LEAGUE`
  - `ELIMINATION`
  - `AMERICANO`
  - `AMERICANO dinámico`
- extras premium/demo todavía visibles
- decidir si `Club View` se oculta o no para usuarios no-admin
- si se sigue con club operations:
  - pagos reales
  - revenue real
  - reservas más ricas

## 10. Recomendación para retomar mañana

Orden sugerido:
1. revisar qué pantallas visibles siguen mock/demo después de este cierre
2. atacar torneos fuera de `LEAGUE`, porque es el bloque visible más grande que todavía no quedó real
3. evitar abrir pagos reales si antes no se define producto mínimo de reserva/cobro

## 11. Estado de cierre

El proyecto quedó bastante más cerca de QA real:
- el core ya es mayormente backend-driven
- el Club View dejó de ser mock
- la base local ya tiene seeds suficientes para probar sin cargar todo a mano

Esto deja una base razonable para continuar mañana sin reconstruir contexto desde chat o commits.
