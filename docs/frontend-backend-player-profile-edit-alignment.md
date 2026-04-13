# Frontend / Backend Player Profile Edit Alignment

## Objetivo

Permitir que el jugador edite su perfil sin volver a mover la verdad oficial al frontend.

## Source of truth

El backend debe seguir siendo la fuente oficial para:

- nombre del jugador
- foto
- posicion preferida
- nivel declarado
- ciudad
- club representado
- bio

El frontend solo debe:

- mostrar esos datos
- enviar la edicion
- refrescar el perfil oficial despues del guardado

## Decision de contrato

- Se agrega `PUT /api/players/me`
- El payload incluye:
  - `fullName`
  - `photoUrl`
  - `preferredSide`
  - `declaredLevel`
  - `city`
  - `representedClubId`
  - `bio`
- `representedClubId` es opcional y puede quedar en `null`
- el resto de los campos principales del perfil jugador se validan como requeridos

## Decision de frontend

- La pantalla `Perfil` expone una accion `Editar perfil`
- El formulario trabaja con el estado actual del jugador autenticado
- Al guardar:
  - pega al backend
  - rehidrata `currentUser` desde `GET /api/players/me`
  - evita parchear manualmente el estado local con reglas duplicadas
