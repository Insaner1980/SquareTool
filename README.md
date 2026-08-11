<!-- generated-by: gsd-doc-writer -->
# SquareTool

SquareTool (store title: **SquareTool: Granny Square**) is a native Android app for planning granny-square blankets with reusable visual motifs, an editable grid, project calculations, and local export.

Square previews are visual planning graphics. SquareTool does not generate crochet instructions.

## What it does

- Creates and manages local blanket projects from 1 x 1 to 50 x 50 cells (2,500 cells maximum).
- Builds reusable square designs from ten programmatic motif templates and three to six saved colors.
- Manages named colors, optional yarn metadata, ordered palettes, and project palettes.
- Paints the Planner grid with Select, Paint, Lock, and Progress tools; preserves locked cells; optionally tracks completed cells; and keeps at least 50 in-memory undo/redo operations.
- Previews deterministic random, balanced, checker, alternating-row, alternating-column, diagonal, striped, mirrored, discrete-gradient, and radial layouts before applying them. Generation can preserve completed cells, reduce orthogonal repeats, use weights, and regenerate from a new seed.
- Calculates assigned-square distribution, visual color usage, finished dimensions, progress, and yarn estimates from saved project data.
- Saves PDF plans and high-resolution PNG images, shares temporary PDF/PNG files through the Android Sharesheet, exports a project-only JSON file, and exports or restores a full versioned JSON backup.
- Provides a three-page onboarding flow, an optional editable sample project, light/dark themes, optional haptics, a Reduce Motion setting that makes Home progress updates immediate, responsive bottom-bar/navigation-rail layouts, and an accessible semantic Planner grid.

## Privacy and offline use

SquareTool has no backend, account system, cloud sync, analytics, advertising, billing, monetization code, or runtime internet requirement. The manifest requests no network or broad storage permissions and disables cleartext traffic. Android cloud backup and device-transfer backup are also disabled.

Project data remains in the app's local Room database and preferences remain in DataStore. Data leaves the app only through an explicit user action: saving a document with Android's Storage Access Framework, sharing a generated file, or exporting a JSON backup. The first Gradle build may still need network access to download development dependencies.

## Technical stack

- Kotlin 2.4.10 and Java/Kotlin JVM target 17
- Android Gradle Plugin 9.3.1 and Gradle wrapper 9.7.0
- Minimum SDK 26; compile and target SDK 37
- Jetpack Compose with Material 3 and Navigation Compose
- Room 2.8.4 with KSP and exported schema version 1
- DataStore Preferences, coroutines, StateFlow, and Kotlin serialization
- Android `PdfDocument`, Storage Access Framework, `FileProvider`, and the Android Sharesheet
- Compose/Android Canvas drawing for motifs, charts, previews, and exports

The project is a single `app` module with a small manually constructed dependency container; it does not use a dependency-injection framework.

## Build

Prerequisites:

- JDK 17 or a newer JDK supported by the configured Android Gradle Plugin
- Android SDK Platform 37
- Android Studio or a command line Android SDK installation accepted by `local.properties`

From PowerShell in the repository root:

```powershell
.\gradlew.bat assembleDebug
```

For a clean debug build:

```powershell
.\gradlew.bat clean assembleDebug
```

The debug APK is written under `app\build\outputs\apk\debug\`.

## Tests and lint

Run JVM unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Run Android lint:

```powershell
.\gradlew.bat lintDebug
```

Compile the device-test sources without requiring a device:

```powershell
.\gradlew.bat compileDebugAndroidTestSources
```

Run Room and Compose instrumented tests only with a connected device or running emulator:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Instrumented tests are separate from `testDebugUnitTest` and require an Android device or emulator. Command documentation alone is not evidence that a connected test run has completed.

## Data storage

Room database `squaretool.db` stores projects, cells, square designs and rounds, colors, palettes, and their ordered cross-references. Foreign keys protect referenced colors and cascade or clear project/design references as appropriate. Project creation/editing with its initial layout and project palette, complete Planner layout replacement, grid resize, sample creation, and Room backup restore use database transactions; project deletion is one foreign-key-cascading SQL statement. The exported Room schema is in `app/schemas/`.

DataStore file `squaretool_settings` stores onboarding state, theme and unit preferences, Planner defaults, accessibility-related preferences, and the last top-level destination. Delete All Data clears both Room data and DataStore preferences after confirmation.

## Export, sharing, and backup

- PDF export produces a light, print-oriented overview and one or more paginated materials pages in A4 or Letter format. The materials output contains the optional design legend, project-palette and used-design colors, estimated color percentages, and yarn figures when configured. Large grids also receive 16-row by 12-column section pages.
- PNG export renders the blanket through the same motif renderer, with optional labels, a complete dynamically sized legend, and transparent background. The default long edge is 2,048 pixels, requests are clamped to 512-8,192 pixels, and the final ARGB allocation is capped at 16 million pixels.
- Saved files use a user-selected document destination and require no storage permission.
- Shared PDF and PNG files are created in the app cache and exposed through `FileProvider`; old share files are pruned when a new share file is created.
- Full JSON backup schema version 1 includes all Room data and the relevant app settings. The Export Project screen can instead create a self-contained project-only JSON file with the designs and colors it references. Restore validates schema, motif templates and round ranges, IDs, dimensions, colors, ordering, numeric values, and references before replacement. Room replacement is transactional; settings are restored first and rolled back to their previous values if the database step fails.

## Source map

| Path | Responsibility |
| --- | --- |
| `app/src/main/java/com/finnvek/squaretool/app/` | Application entry point, manual container, routes, and navigation host |
| `app/src/main/java/com/finnvek/squaretool/data/local/` | Room entities, DAOs, relations, and database |
| `app/src/main/java/com/finnvek/squaretool/data/repository/` | Transaction boundary, local search, settings, sample data, and backup mapping |
| `app/src/main/java/com/finnvek/squaretool/domain/model/` | Immutable grid and calculation models |
| `app/src/main/java/com/finnvek/squaretool/domain/algorithm/` | Grid math, layout generation, insights calculations, and Planner history |
| `app/src/main/java/com/finnvek/squaretool/render/` | Motif registry, normalized geometry plans, and shared Canvas renderer |
| `app/src/main/java/com/finnvek/squaretool/ui/` | Compose screens and ViewModels grouped by feature |
| `app/src/main/java/com/finnvek/squaretool/export/` | Export snapshots, rendering policy, PDF/PNG writers, and sharing |
| `app/src/main/java/com/finnvek/squaretool/backup/` | Versioned JSON DTOs, codec, validation, and backup service |
| `app/src/test/` | JVM unit tests |
| `app/src/androidTest/` | Room and Compose/device tests |

See [PROJECT.md](PROJECT.md) for the implementation reference and [QA_CHECKLIST.md](QA_CHECKLIST.md) for manual verification coverage. The four root reference images were design inputs only; see [REFERENCE_IMAGES.md](REFERENCE_IMAGES.md).
