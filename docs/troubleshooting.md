# Troubleshooting

## Unresolved reference `animateItemPlacement`
If you see the error `Unresolved reference: animateItemPlacement` when you try to import
`androidx.compose.foundation.animateItemPlacement`, change the import to one of the lazy
packages. For a `LazyColumn` or `LazyRow`, the correct import is
`androidx.compose.foundation.lazy.animateItemPlacement`. For lazy grids, use
`androidx.compose.foundation.lazy.grid.animateItemPlacement`.

Also make sure you are on a recent version of the Compose Foundation dependency (1.2.0 or
newer). The project already pulls the dependency through the Compose BOM, so syncing the
Gradle files after fixing the import is usually enough.
