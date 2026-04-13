# Frontend / Backend Player Profile Registration Alignment

## Estado actual

- `player_profiles` ya persiste `fullName`, `photoUrl`, `preferredSide`, `declaredLevel`, `city`, `currentRating` y estado competitivo.
- El registro de jugador hoy solo carga `fullName`, `email`, `phone` y `password`.
- El perfil mostrado en frontend sigue mezclando datos oficiales del backend con defaults heredados del mock base.
- No existe un campo oficial para `club al que representa`.

## Gap respecto al producto

El perfil de jugador debe mostrar, desde backend como source of truth:

- nombre
- foto
- posicion
- nivel declarado
- ciudad
- rating
- club representado opcional

Para que eso sea consistente:

1. El registro de jugador debe capturar esos datos iniciales.
2. `player_profiles` debe persistir el club representado de forma oficial.
3. `GET /api/players/me` y `GET /api/players/{id}` deben devolver esos campos.
4. El frontend debe hidratar el perfil desde esos datos oficiales y dejar de depender de defaults mock para avatar / display metadata.

## Decision de implementacion

- `photoUrl`: opcional en registro.
- `preferredSide`: obligatorio para cuenta jugador.
- `declaredLevel`: obligatorio para cuenta jugador.
- `city`: obligatorio para cuenta jugador.
- `representedClubId`: opcional para cuenta jugador.

## Impacto esperado

- Nuevas cuentas jugador nacen con perfil oficial mucho mas completo.
- El perfil propio queda alineado con backend sin inventar metadata local.
- El club representado pasa a ser un dato de perfil real, distinto de rankings o historico de partidos.
