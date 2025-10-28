# Recurring Lessons Feature Scope

## 1. Recurrence capabilities
- Support recurrence schedules: weekly, every N weeks (biweekly), and monthly-by-day-of-week.
- Allow series without an end date or terminating on a specific date.
- Permit selecting multiple weekdays per series.
- Enable editing operations at three levels: single occurrence, this-and-following occurrences, and entire series.
- Manage occurrence exceptions: cancellation or overriding a single instance's time, duration, notes, or price.

## 2. Data model updates
- Extend `Lesson` with nullable `seriesId` and boolean `isInstance` (defaults to `false`; virtual instances are not persisted).
- Introduce `RecurrenceRule` with fields: `id`, `baseLessonId`, `frequency` (`WEEKLY`, `BIWEEKLY`, `MONTHLY_BY_DOW`), `interval`, `daysOfWeek`, `startDateTime`, `untilDateTime` (nullable), and `timezone`.
- Introduce `RecurrenceException` with fields: `id`, `seriesId`, `originalDateTime`, `type` (`CANCELLED`, `OVERRIDDEN`), and optional overrides for start time, duration, notes, and price.
- Add indices on `RecurrenceRule.baseLessonId`, `RecurrenceException.seriesId + originalDateTime`, and `Lesson.startDateTime`.
- Provide Room migrations that add the new tables and columns without breaking existing data.

## 3. Data layer responsibilities
- DAO methods to read recurrence rules, exceptions, and base lessons.
- `CalendarRepository.getEventsInRange(start, end)` should combine:
  - Persisted one-off lessons within range.
  - Generated occurrences for each recurrence rule intersecting the range, applying related exceptions.
- Implement `expandSeries(rule, range)` to emit occurrences per frequency, interval, and weekdays.
- Implement `applyExceptions(instances, exceptions)` to cancel or override generated instances.
- Implement `detectConflicts(instance, existingLessonsInRange)` to surface time collisions for warnings.

## 4. Creating lessons
- Update the lesson form with a "Repeat" section supporting off/weekly/every N weeks/monthly-by-weekday options.
- When enabled, display weekday selection, interval input, and end-date picker.
- On save:
  - Create the base `Lesson` (temporarily leaving `seriesId` null).
  - If recurrence is on, create the `RecurrenceRule` and backfill the `Lesson.seriesId`.

## 5. Calendar presentation
- ViewModel queries `getEventsInRange` for daily/weekly views.
- Mark recurring instances in UI for differentiation.
- Lesson cards show recurrence marker and labels such as "every Wed, Fri" or "every 2 weeks".
- Limit generation window to ~12 weeks before/after the current range for performance.

## 6. Editing lessons
- Opening a recurring lesson presents options: single occurrence, this and following, or entire series.
- Single occurrence edits create a `RecurrenceException` (`OVERRIDDEN` or `CANCELLED`) keyed by the instance date.
- "This and following" edits:
  - Set the existing rule's `until` to the selected occurrence.
  - Create a new base `Lesson`, inheriting fields and applying updates.
  - Create a new `RecurrenceRule` starting at the selected occurrence.
- Whole-series edits update the base lesson and recurrence rule; keep non-conflicting exceptions intact.
