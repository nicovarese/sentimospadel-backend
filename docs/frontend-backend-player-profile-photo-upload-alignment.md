# Frontend / Backend Player Profile Photo Upload Alignment

## Objetivo

Permitir que la foto del perfil jugador se cargue como archivo real sin volver a mover la verdad oficial al frontend.

## Source of truth

El backend debe seguir siendo la fuente oficial para:

- validar tipo y tamano de archivo
- guardar la foto gestionada
- exponer la URL publica resultante
- limpiar la foto gestionada anterior cuando el jugador la reemplaza o la elimina

El frontend solo debe:

- dejar seleccionar la imagen
- subir el archivo con `multipart/form-data`
- refrescar el perfil oficial despues del guardado

## Decision de contrato

- Se agrega `POST /api/players/me/photo`
- El endpoint recibe `file` multipart
- El backend acepta:
  - JPG
  - PNG
  - WEBP
- Tamano maximo:
  - `5 MB`
- La respuesta devuelve el `PlayerProfileResponse` oficial actualizado
- La foto queda servida publicamente bajo `/api/player-profile-photos/{filename}`

## Decision de frontend

- `Editar perfil` mantiene la UX existente del avatar
- El usuario ahora puede:
  - seleccionar archivo
  - previsualizarlo antes de guardar
  - quitar la foto actual
- Al guardar:
  - primero actualiza el resto del perfil oficial
  - despues sube la foto si hay un archivo nuevo
  - finalmente rehidrata `currentUser` desde backend
