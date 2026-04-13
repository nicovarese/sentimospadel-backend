# Auth rate limiting

Fecha: 2026-04-13

## Alcance

El backend aplica rate limiting in-memory sobre endpoints publicos de autenticacion:

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/verify-email/resend`

Cuando se supera el limite, responde `429 Too Many Requests` con header `Retry-After`.

## Configuracion

Variables disponibles:

- `AUTH_RATE_LIMIT_ENABLED`
- `AUTH_RATE_LIMIT_LOGIN_MAX_ATTEMPTS`
- `AUTH_RATE_LIMIT_LOGIN_WINDOW`
- `AUTH_RATE_LIMIT_REGISTER_MAX_ATTEMPTS`
- `AUTH_RATE_LIMIT_REGISTER_WINDOW`
- `AUTH_RATE_LIMIT_RESEND_VERIFICATION_MAX_ATTEMPTS`
- `AUTH_RATE_LIMIT_RESEND_VERIFICATION_WINDOW`

Defaults:

- login: 5 intentos cada 10 minutos
- register: 3 intentos cada 1 hora
- resend verification: 3 intentos cada 15 minutos

## Limitacion conocida

El limiter actual vive en memoria de la instancia. Sirve para MVP, local, staging y una instancia productiva simple.

Si production corre multiples instancias, hay que moverlo a Redis, gateway/WAF o un rate limiter compartido para que el limite sea global.
