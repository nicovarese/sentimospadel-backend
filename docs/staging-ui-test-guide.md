# Staging UI Test Guide

## Objetivo

Esta guia deja por escrito que datos existen cuando levantas `staging` local y que pruebas de interfaz conviene hacer con esos datos.

## Como se cargan los datos

- `local` y `staging` ejecutan migraciones base y seeds.
- `production` ejecuta solo migraciones base, sin seeds.
- En `staging`, Flyway corre `classpath:db/migration,classpath:db/seed`.
- Los seeds se aplican una sola vez por base de datos. Si usas una base ya migrada, Flyway no vuelve a recrearlos.

## Recomendacion para pruebas estables

Si queres una demo reproducible y limpia, usa una base separada para staging, por ejemplo:

- base: `sentimospadel_staging`
- user: `sentimospadel_staging_user`

Si levantas `staging` apuntando a la misma base que `dev`, vas a ver los datos ya existentes de esa base. Eso puede servir para practicar el proceso, pero no garantiza un estado limpio.

## Credenciales seed

Todos los usuarios seed usan la misma password:

- `secret123`

Usuarios jugador seed:

- `nicolas.seed@sentimospadel.test`
- `martin.seed@sentimospadel.test`
- `felipe.seed@sentimospadel.test`
- `diego.seed@sentimospadel.test`
- `ana.seed@sentimospadel.test`
- `juan.seed@sentimospadel.test`
- `lucia.seed@sentimospadel.test`
- `paula.seed@sentimospadel.test`
- `alvaro.historia@sentimospadel.test`
- `bruno.historia@sentimospadel.test`
- `carla.historia@sentimospadel.test`
- `valentina.historia@sentimospadel.test`

Usuario admin de club:

- `club.admin@sentimospadel.test`

## Clubes seed

Estos clubes existen de base:

- `Top Padel`
- `World Padel`
- `Cordon Padel`
- `Boss`
- `Reducto`

Ademas, `Top Padel` ya trae:

- canchas seed
- agenda con un slot reservado hoy a las 19:00
- agenda con un slot bloqueado hoy a las 22:00
- actividad reciente de club

## Perfiles utiles para probar

### Nicolas

- email: `nicolas.seed@sentimospadel.test`
- password: `secret123`
- rol: jugador
- rating inicial seed: `3.84`
- categoria estimada: `Quinta`
- bueno para probar:
  - home de jugador
  - partidos abiertos
  - resultado pendiente de validacion
  - torneos abiertos
  - join / leave en torneos

### Diego

- email: `diego.seed@sentimospadel.test`
- password: `secret123`
- rol: jugador
- rating seed: `5.85`
- categoria estimada: `Segunda`
- verificacion: `PENDING`
- bueno para probar:
  - jugador con categoria alta pendiente de verificacion
  - creador de torneo en curso
  - envio de resultados

### Lucia

- email: `lucia.seed@sentimospadel.test`
- password: `secret123`
- verificacion: `PENDING`
- bueno para probar:
  - otro jugador con verificacion pendiente

### Felipe

- email: `felipe.seed@sentimospadel.test`
- password: `secret123`
- rating seed: `5.16`
- categoria estimada: `Tercera`
- bueno para probar:
  - perfil
  - historial de rating
  - ranking
  - ultimos resultados

### Club Admin

- email: `club.admin@sentimospadel.test`
- password: `secret123`
- rol: `ADMIN`
- club administrado: `Top Padel`
- bueno para probar:
  - dashboard de club
  - agenda del club
  - canchas
  - actividad de club

## Datos ya cargados para interfaz

### Partidos sociales

Partidos utiles ya sembrados:

- `Top Padel - Cancha 1`, abierto, fecha futura `2030-04-10 20:00 UTC`, creador Nicolas
- `World Padel - Cancha 2`, abierto, fecha futura `2030-04-11 21:30 UTC`, con jugadores ya cargados
- `Cordon Padel - Cancha 1`, full, fecha futura `2030-04-12 19:00 UTC`
- `Boss - Cancha 1`, cancelado, fecha futura `2030-04-13 18:00 UTC`
- `Top Padel - Cancha 3`, `RESULT_PENDING`, usado para validacion de resultado
- `World Padel - Cancha QA Join`, caso QA para join a partido social
- `Top Padel - Cancha QA Submit`, caso QA para submit de resultado
- `Top Padel - Cancha QA Validacion`, caso QA para confirmacion / rechazo de resultado

### Torneos

Torneos abiertos:

- `Liga QA Abierta`
- `Eliminatoria QA Abierta`

Torneos en curso:

- `Liga QA En Curso`
- `Eliminatoria QA En Curso`

Los torneos en curso ya traen:

- inscriptos
- standings
- cruces generados
- un resultado confirmado
- un resultado pendiente

### Historial de rating

Felipe ya trae historial confirmado de varios partidos:

- `Historial Felipe 1`
- `Historial Felipe 2`
- `Historial Felipe 3`
- `Historial Felipe 4`

Esto sirve para validar:

- grafica de evolucion
- ultimos resultados
- ranking
- insights de perfil

## Pruebas recomendadas de interfaz

## Flujo 1: login jugador

Usar:

- `nicolas.seed@sentimospadel.test`
- `secret123`

Verificar:

- login correcto
- home carga sin errores
- agenda visible
- ranking y perfil accesibles

## Flujo 2: join de partido abierto

Usar:

- `ana.seed@sentimospadel.test`

Ir a:

- listado de partidos

Probar:

- abrir un partido futuro abierto
- unirse a un partido
- verificar cambio visual en cupos o estado

## Flujo 3: inbox / validacion de resultado

Usar:

- `nicolas.seed@sentimospadel.test`
- `ana.seed@sentimospadel.test`
- `felipe.seed@sentimospadel.test`

Probar:

- abrir notificaciones
- entrar a un partido con resultado pendiente
- validar o rechazar resultado si el frontend lo habilita para ese usuario

## Flujo 4: torneos abiertos

Usar:

- `nicolas.seed@sentimospadel.test`

Probar:

- abrir `Liga QA Abierta`
- abrir `Eliminatoria QA Abierta`
- revisar informacion general
- revisar inscriptos
- probar flujo de join / leave si aparece habilitado

## Flujo 5: torneos en curso

Usar:

- `diego.seed@sentimospadel.test`
- `nicolas.seed@sentimospadel.test`

Probar:

- abrir `Liga QA En Curso`
- abrir `Eliminatoria QA En Curso`
- revisar standings
- revisar cruces
- revisar partidos con resultado confirmado y pendiente

## Flujo 6: perfil y ranking

Usar:

- `felipe.seed@sentimospadel.test`

Probar:

- abrir perfil
- revisar historial de rating
- revisar ultimos resultados
- revisar ranking nacional
- revisar ranking de club si aparece

## Flujo 7: verificacion de categoria

Usar:

- `diego.seed@sentimospadel.test`
- `lucia.seed@sentimospadel.test`

Probar:

- abrir perfil
- revisar estado de verificacion
- verificar que el frontend muestre `pending` y no una categoria ya verificada

## Flujo 8: dashboard de club

Usar:

- `club.admin@sentimospadel.test`

Probar:

- login como admin
- apertura de dashboard de club
- agenda de `Top Padel`
- canchas y precios
- actividad de club
- slots reservado / bloqueado del dia actual

## Si no ves los datos seed

Revisar en este orden:

1. Que el backend este levantado con perfil `staging`.
2. Que `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` apunten a la base correcta.
3. Que la base no tenga un estado previo inesperado.
4. Que en la tabla `flyway_schema_history` aparezcan tambien las versiones seed.

## Nota operativa

Si queres una demo realmente estable para mostrar al CEO, lo mejor es:

- usar una base `staging` separada
- no mezclarla con tu base diaria de desarrollo
- no registrar usuarios nuevos en esa base durante la demo, salvo que quieras probar onboarding

