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
