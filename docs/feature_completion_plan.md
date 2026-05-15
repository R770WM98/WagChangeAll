# Feature Completion Plan

This plan follows the current codebase after adding backup/restore and CSV export.

## Phase 1 - Data Safety and Portability (done)

- JSON backup export/import in app settings.
- CSV export for maintenance and fuel data.
- Repository-level snapshot contract for future sync and reports.

## Phase 2 - Reminder Actions and Automation

- Add reminder notification actions: `Snooze`, `Mark Done`.
- Auto-advance repeating reminders when marked done.
- Add reminder action handling receiver/service.

## Phase 3 - Dashboard Completion

- Add due timeline cards from reminder and maintenance sources.
- Add monthly cost trend card from maintenance + fuel entries.
- Add bike health score model and display card.

## Phase 4 - Maintenance and Parts Lifecycle (implemented)

- Add service templates (oil, brake fluid, CVT, tires).
- Add interval/warranty fields to part records.
- Add estimated remaining life calculations.

## Phase 5 - Fuel Analytics

- Add km/L trend chart and average metrics.
- Add cost-per-km and monthly fuel spend card.
- Add per-bike comparison view.

## Notes for implementation

- Keep using repository as feature boundary to avoid UI-layer data drift.
- Add focused unit tests for each phase before UI integration.
- Reuse settings panel for data and diagnostics tooling.
