# WagChangeAll App Structure

This project now includes a proposal-based app implementation for the `WagChangeAll` motorcycle maintenance app.

## Feature-to-Screen Mapping

- Maintenance Log -> `feature/maintenance/MaintenanceLogScreen.kt`
- Odometer Tracking -> `feature/odometer/OdometerTrackingScreen.kt`
- Service Reminders -> `feature/reminders/ServiceRemindersScreen.kt`
- Fuel Tracker -> `feature/fuel/FuelTrackerScreen.kt`
- Predictive Rules -> `feature/predictive/PredictiveRulesScreen.kt`
- Mechanic Summary -> `feature/mechanic/MechanicSummaryScreen.kt`
- Dashboard (overview) -> `feature/dashboard/DashboardScreen.kt`

## Data Portability (Completed)

- Backup export/import (JSON) from app settings.
- Maintenance export (CSV) from app settings.
- Fuel export (CSV) from app settings.
- Repository snapshot contract for future sync and reporting features.

## New Machine Setup

To open and build this project on another computer, create a machine-specific `local.properties` file and point it to that computer's Android SDK.

### Steps

1. Install Android Studio on the new machine.
2. Make sure the Android SDK is installed.
3. Copy `local.properties.example` to `local.properties` in the project root.
4. Update `sdk.dir` in `local.properties` to the SDK path on that machine.
5. Open the project in Android Studio and let Gradle sync.

### Example

```properties
sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

### Notes

- `local.properties` should not be committed to Git.
- This repo does not require the old desktop `.jdk` folder anymore.

## Package Layout

- `core/navigation` - routes + nav graph
- `core/ui` - shared UI templates
- `feature/*` - screen-level feature modules
- `domain/model` - core data models
- `data/local` - Room entities and DAOs
- `data/repository` - repository contract and implementations
- `settings` - app settings and backup transfer services

## Entry Point

- `MainActivity.kt` launches `WagChangeAllApp()` in `App.kt`.

## Roadmap

- Next phases are tracked in `docs/feature_completion_plan.md`.
