# CEO Local Setup and Demo Guide

Fecha: 2026-04-08

## Recomendacion corta

La mejor opcion para el CEO no es instalar todo localmente. Lo ideal es darle un ambiente `staging` ya desplegado.

Mientras eso no exista, este es el camino local.

## Que tiene que instalar

### Para probar web + backend local

- Git
- Java 21
- Node.js 22+
- PostgreSQL 16+

### Solo si quiere probar Android local

- Android Studio
- Android SDK

### Solo si quiere probar iOS local

- Mac
- Xcode
- CocoaPods

## 1. Clonar los dos repositorios

Debe tener ambos:

- `frontend`
- `backend`

## 2. Preparar PostgreSQL

Crear:

- database: `sentimospadel`
- user: `sentimospadel_user`
- password: `sentimospadel_local_password`

El backend usa por default:

```text
jdbc:postgresql://localhost:5432/sentimospadel?currentSchema=public
```

## 3. Levantar backend

Ir a `backend/` y correr:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Que deberia pasar:

- Flyway crea y migra el schema
- se cargan seeds QA
- backend queda en `http://localhost:8081`

Chequeo rapido:

- abrir `http://localhost:8081/api/health`

## 4. Levantar frontend web

Ir a `frontend/`.

Crear `.env.local` con:

```env
VITE_API_BASE_URL=http://localhost:8081
```

Luego correr:

```powershell
npm ci
npm run dev
```

La app queda disponible en el puerto que indique Vite, normalmente `http://localhost:5173`.

## 5. Cuentas seed para probar

Password comun:

- `Qa123456!`

Jugadores QA:

- `nicolas.seed@sentimospadel.test`
- `martin.seed@sentimospadel.test`
- `felipe.seed@sentimospadel.test`
- `diego.seed@sentimospadel.test`
- `ana.seed@sentimospadel.test`
- `juan.seed@sentimospadel.test`
- `lucia.seed@sentimospadel.test`
- `paula.seed@sentimospadel.test`

Admin de club:

- `club.admin@sentimospadel.test`

Clubes utiles:

- `Top Padel`
- `World Padel`

## 6. Flujos recomendados para demo

### Jugador

- login
- ver perfil
- editar perfil y subir foto
- crear partido
- invitar por link
- cargar y confirmar resultado
- ver rating history
- crear o sumarse a torneo

### Club admin

- login como `club.admin@sentimospadel.test`
- abrir `Club View`
- revisar dashboard
- gestionar canchas
- revisar agenda
- aprobar o rechazar verificaciones
- aprobar o rechazar reservas si el club esta en modo confirmacion

### Torneos

- crear `LEAGUE`
- crear `ELIMINATION`
- inscribir equipos por link o carga directa
- lanzar torneo
- cargar resultados
- revisar standings o playoffs

## 7. Si quiere probar Android

En `frontend/`:

```powershell
npm run build:mobile
npm run cap:sync:android
npm run cap:open:android
```

Para emulador Android conviene usar:

```env
VITE_API_BASE_URL=http://10.0.2.2:8081
```

## 8. Si quiere probar iOS

Tiene que hacerlo en una Mac.

En `frontend/`:

```powershell
npm run build:mobile
npm run cap:sync:ios
npm run cap:open:ios
```

Si backend y simulador corren en la misma Mac:

```env
VITE_API_BASE_URL=http://localhost:8081
```

## 9. Problemas comunes

- si el frontend no conecta: revisar `VITE_API_BASE_URL`
- si backend no arranca: revisar PostgreSQL y credenciales
- si email verification no manda mails: en local esta en modo `log-only`
- si iOS no compila: falta `CocoaPods` o signing en Xcode
- si Android no compila: falta SDK o sync de Gradle

## 10. Recomendacion final

Para el CEO, lo correcto es preparar un `staging` estable con:

- backend publicado
- frontend publicado o build mobile firmado
- base de datos separada
- seeds/demo data estables

Eso evita que tenga que instalar Java, Node, PostgreSQL y tooling nativo.
