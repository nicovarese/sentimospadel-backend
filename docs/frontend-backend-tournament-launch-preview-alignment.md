# Frontend / Backend Tournament Launch Preview Alignment

## Objetivo
- Mantener la UX actual de `Lanzar Torneo`
- Remover la generacion local del fixture y del bracket preliminar en frontend
- Hacer que la preview salga de backend con las mismas reglas oficiales que usa el launch real

## Hallazgos
- El backend ya es source of truth para:
  - validacion de launch
  - cantidad de canchas
  - cantidad de grupos en eliminacion
  - doble ronda en liga
  - generacion real de partidos
  - avance oficial del bracket en eliminacion
- El frontend todavia inventa en `LaunchTournamentView`:
  - grupos
  - group matches
  - playoffs preliminares
  - coordinacion automatica con restricciones que backend no consume

## Decision
- Agregar un endpoint autenticado de preview:
  - `POST /api/tournaments/{id}/launch-preview`
- Reutilizar el mismo request base del launch:
  - `availableCourts`
  - `numberOfGroups`
  - `leagueRounds`
  - `courtNames`
- El backend devuelve:
  - grupos preview
  - partidos preview del fixture inicial
  - playoffs placeholder solo cuando correspondan a reglas oficiales backend

## Criterio de done
- `LaunchTournamentView` deja de generar fixture en cliente para torneos backend
- La preview refleja solo reglas que backend soporta realmente
- El launch final sigue pegando al endpoint oficial existente
- Se remueven o neutralizan controles frontend que no tienen efecto real en backend

## Cleanup aplicado
- `LaunchTournamentView` ya no arrastra el generador legacy en `App.tsx`
- El launch final ya no conserva una rama local inalcanzable para crear partidos de torneo en frontend
- El modal mantiene la UX visible de preview y confirmacion, pero:
  - la preview sale solo de `POST /api/tournaments/{id}/launch-preview`
  - la confirmacion sale solo de `POST /api/tournaments/{id}/launch`
