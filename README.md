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


