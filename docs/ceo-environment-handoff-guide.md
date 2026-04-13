# CEO Environment Handoff Guide

Fecha: 2026-04-08

## Recomendacion corta

Para el CEO, el frontend principal debe levantarse como web apuntando a `staging`.

Usar Android Studio solo tiene sentido si queres probar especificamente:

- camara o galeria nativa
- comportamiento del webview
- deep links nativos
- packaging Android

Si lo que quiere es probar producto, flujos y estabilidad, conviene web `staging`.

## Esquema recomendado

- `backend staging` desplegado o corriendo en una maquina fija
- `frontend staging web` publicado con URL estable
- base de datos `staging` separada
- storage de fotos separado
- seeds demo estables

## Backend staging

Variables minimas:

```env
SPRING_PROFILES_ACTIVE=staging
DB_URL=jdbc:postgresql://<host>:5432/<db>?currentSchema=public
DB_USERNAME=<user>
DB_PASSWORD=<password>
JWT_SECRET=<secret>
EMAIL_VERIFICATION_BASE_URL=https://api-staging.example.com/api/auth/verify-email
EMAIL_VERIFICATION_LOGIN_URL=https://staging.example.com
EMAIL_VERIFICATION_FROM=no-reply@staging.example.com
MATCH_INVITATION_BASE_URL=https://staging.example.com
TOURNAMENT_INVITATION_BASE_URL=https://staging.example.com
PLAYER_PROFILE_PHOTO_STORAGE_PATH=/srv/sentimospadel/staging/player-profile-photos
PLAYER_PROFILE_PHOTO_PUBLIC_BASE_URL=https://api-staging.example.com/api/player-profile-photos
PUSH_NOTIFICATIONS_PROVIDER=log-only
PUSH_NOTIFICATIONS_LOG_PAYLOADS=true
```

Arranque local de staging:

```powershell
$env:SPRING_PROFILES_ACTIVE="staging"
$env:DB_URL="jdbc:postgresql://localhost:5432/sentimospadel_staging?currentSchema=public"
$env:DB_USERNAME="sentimospadel_staging_user"
$env:DB_PASSWORD="replace-me"
$env:JWT_SECRET="replace-me-with-a-real-staging-secret"
$env:EMAIL_VERIFICATION_BASE_URL="https://api-staging.example.com/api/auth/verify-email"
$env:EMAIL_VERIFICATION_LOGIN_URL="https://staging.example.com"
$env:EMAIL_VERIFICATION_FROM="no-reply@staging.example.com"
$env:MATCH_INVITATION_BASE_URL="https://staging.example.com"
$env:TOURNAMENT_INVITATION_BASE_URL="https://staging.example.com"
$env:PLAYER_PROFILE_PHOTO_STORAGE_PATH="C:\sentimospadel\staging\player-profile-photos"
$env:PLAYER_PROFILE_PHOTO_PUBLIC_BASE_URL="https://api-staging.example.com/api/player-profile-photos"
$env:PUSH_NOTIFICATIONS_PROVIDER="log-only"
$env:PUSH_NOTIFICATIONS_LOG_PAYLOADS="true"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

## Frontend staging web

Crear `frontend/.env.staging.local`:

```env
VITE_API_BASE_URL=https://api-staging.example.com
VITE_GEMINI_API_KEY=
```

Para probar localmente el web staging:

```powershell
npm ci
npm run dev:staging
```

Para generar build de staging:

```powershell
npm run build:staging
```

## Frontend Android

Usalo solo si queres validar Android nativo.

Crear `frontend/.env.staging.local` con la URL HTTPS de staging y luego:

```powershell
npm run cap:sync:android:staging
npx cap open android
```

Desde Android Studio:

1. esperar el sync de Gradle
2. elegir emulador o device
3. correr la app

## Decision practica para el CEO

Si hoy queres que el CEO pruebe negocio:

- levantarle `frontend staging web`
- dejar `Android Studio` para vos

Si manana queres demo mobile:

- mantener el mismo backend `staging`
- generar build Android desde ese mismo ambiente
