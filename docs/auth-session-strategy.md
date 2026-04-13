# Auth session strategy

Fecha: 2026-04-13

## Decision

Para mobile y web se usa:

- access token JWT corto para llamadas API
- refresh token persistido server-side para renovar sesion
- rotacion de refresh token en cada refresh
- logout server-side revocando refresh token

## Endpoints

- `POST /api/auth/login`: devuelve `accessToken`, `refreshToken` y `refreshTokenExpiresAt`
- `POST /api/auth/refresh`: recibe `refreshToken`, rota el token y devuelve una nueva sesion
- `POST /api/auth/logout`: recibe `refreshToken` y lo revoca

## Configuracion

- `JWT_EXPIRATION_MS`: duracion del access token
- `AUTH_REFRESH_TOKEN_EXPIRATION`: duracion del refresh token, default `30d`

## Comportamiento esperado

- El frontend usa access token normalmente.
- Si una llamada autenticada recibe `401`, intenta renovar con refresh token una sola vez y reintenta la request original.
- Si refresh falla, limpia la sesion local.
- Logout revoca el refresh token en backend y limpia la sesion local.

## Pendiente posterior

- Mover almacenamiento mobile a secure storage nativo cuando se cierre packaging final.
- Agregar administracion de sesiones/dispositivos si el producto lo necesita.
- Definir politica de expiracion/revocacion mas estricta si se requiere por compliance.
