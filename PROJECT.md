<!-- generated-by: gsd-doc-writer -->
# SquareTool implementation reference

This document describes the implementation currently present in the working tree. It is an implementation and review reference, not a roadmap, product promise, or statement that device-level release verification has completed.

## Authority and intended use

Use this file to orient code reviews, construct review questions, plan UI work, trace state and data ownership, and identify the smallest relevant validation surface. When this document and the checkout disagree, the current source, resources, Gradle configuration, exported Room schema, and executable tests are authoritative in that order.

The document intentionally distinguishes:

- implemented runtime behavior from manual QA expectations;
- source-level safeguards from device-tested behavior;
- local persistence from explicit export/share flows;
- code paths that support built-in records from records that are actually seeded;
- test source coverage from tests proven to have run in a particular environment.

`README.md` is the concise build and usage entry point. `QA_CHECKLIST.md` is a manual verification checklist and may describe checks still requiring execution; it is not runtime evidence. `REFERENCE_IMAGES.md` explains the non-runtime concept images.

## Product boundary

SquareTool is an offline visual planner for granny-square blankets and other square-grid projects. A user can define colored motif designs, place them manually or algorithmically, protect cells with locks, track physical completion, inspect calculated project summaries, and export a local plan.

Motifs are visual planning graphics. They are not stitch diagrams, and the app does not generate crochet instructions. Sync is not implemented. Monetization is not implemented. There is no account, backend, collaboration, analytics, advertising, AI, billing, or subscription code.

The supported grid range is 1 to 50 rows and 1 to 50 columns, with an effective maximum of 2,500 cells. The four concept images in the repository root are design references and are not packaged as runtime UI assets.

## Repository and build baseline

The project is a single-module native Android application.

| Concern | Current configuration |
| --- | --- |
| Root/module | Gradle root `SquareTool`; one required Android application module, `:app` |
| Identity | Namespace and application ID `com.finnvek.squaretool`; store-facing resource title `SquareTool` |
| App version | `versionCode = 1`, `versionName = 1.0.0` |
| Android SDK | `minSdk = 26`, `compileSdk = 37`, `targetSdk = 37` |
| Language/toolchain | Kotlin 2.4.10, Java/Kotlin JVM target 17, KSP 2.3.11 |
| Build tools | Android Gradle Plugin 9.3.1, Gradle wrapper 9.7.0 with a pinned distribution SHA-256 |
| UI | Jetpack Compose, Material 3, Navigation Compose, Compose BOM 2026.08.00 |
| Persistence | Room 2.8.4 with KSP and schema export; DataStore Preferences 1.2.1 |
| Async/serialization | Kotlin coroutines 1.11.0 and Kotlin serialization JSON 1.11.0 |
| Release build | R8 minification and resource shrinking enabled; no release signing configuration or signing material in the repository |
| Debug build | Unit-test coverage enabled; Android resources available to JVM tests |

Dependency repositories are restricted to Google Maven and Maven Central for project dependencies, with the Gradle Plugin Portal used for plugin resolution. Project-level repositories are rejected. Dependency locking is enabled for all configurations, and the checkout contains app and settings lockfiles plus Gradle dependency-verification metadata and a verification keyring.

The root build pins selected transitive versions for Logback, Netty 4.1, Apache Commons Lang, Apache HttpClient, and Bouncy Castle. Kotlin task caching is explicitly disabled in `gradle.properties`; the dependency-check suppression file documents the temporary Kotlin build-cache CVE rationale. Configuration cache and Gradle build cache remain enabled.

The release build is configured to shrink and optimize, but the repository does not define a publishing pipeline, Play configuration, release keystore, or signing block. A successful debug build or CodeQL build therefore does not prove a releasable signed artifact.

## Source ownership map

| Path | Primary responsibility |
| --- | --- |
| `app/src/main/java/com/finnvek/squaretool/app/` | Application entry point, manual dependency container, route registry, navigation host, onboarding gate |
| `app/src/main/java/com/finnvek/squaretool/data/local/` | Room entities, relations, DAOs, database version and creation |
| `app/src/main/java/com/finnvek/squaretool/data/repository/` | Transaction boundaries, searches, sample creation, settings persistence, backup mappings |
| `app/src/main/java/com/finnvek/squaretool/domain/model/` | Immutable grid, measurement, progress, usage, and yarn models |
| `app/src/main/java/com/finnvek/squaretool/domain/algorithm/` | Grid resizing, calculations, layout generation, and in-memory Planner history |
| `app/src/main/java/com/finnvek/squaretool/render/` | Motif catalog, normalized geometry plans, Android/Compose Canvas renderer |
| `app/src/main/java/com/finnvek/squaretool/ui/` | Shared Compose controls and feature UI/ViewModels grouped by screen |
| `app/src/main/java/com/finnvek/squaretool/export/` | Export snapshot, sizing/section policy, PDF/PNG rendering, cache sharing |
| `app/src/main/java/com/finnvek/squaretool/backup/` | Versioned DTOs, JSON codec, validator, full/project backup service |
| `app/src/main/res/values/` | English UI copy, launcher/splash colors, light theme resources |
| `app/src/main/res/values-night/` | Dark platform theme resources |
| `app/src/main/res/xml/` | Backup exclusions, data-transfer exclusions, FileProvider path |
| `app/schemas/` | Exported Room schema 1 |
| `app/src/test/` | JVM and Robolectric tests |
| `app/src/androidTest/` | Device/emulator Room, DataStore, export, navigation, and Compose tests |
| `config/` and `tools/` | Static-analysis policy and thin wrappers around the shared Android-check runtime |

## Runtime architecture

```text
MainActivity
  -> SquareToolApp (theme and onboarding gate)
     -> SquareToolNavigationHost
        -> feature routes and ViewModels
           -> SquareToolRepository / SettingsRepository
              -> Room DAOs / DataStore Preferences

Pure domain algorithms -> ViewModels and insights
Motif geometry plan -> screen Canvas, preview Canvas, PDF Canvas, PNG Canvas
Repository snapshot -> export writers or versioned backup DTOs
```

- `SquareToolApplication` creates one `AppContainer` with the Room database, `SquareToolRepository`, `SettingsRepository`, and `BackupService`. No dependency-injection framework is used.
- `MainActivity` installs the splash screen, enables edge-to-edge drawing, and hosts Compose.
- Feature ViewModels expose immutable state through `StateFlow`; routes collect with `collectAsStateWithLifecycle`.
- Room is the source of truth for projects and library content. DataStore is the source of truth for app preferences and onboarding state.
- Layout generation runs on `Dispatchers.Default`. PDF/PNG writing and file I/O run on `Dispatchers.IO`.
- UI copy is stored in Android resources; sample names and other seeded content are local data. The implementation currently ships English resources only.

### Startup and state ownership

1. `MainActivity` installs the Android 12-compatible splash screen, rejects obscured and partially obscured touch events, enables edge-to-edge layout, and passes the application-scoped `AppContainer` into Compose.
2. `SquareToolApp` collects `AppSettings`. It renders an empty surface while the first DataStore value is loading, shows onboarding until `onboardingCompleted` is true, and otherwise creates the navigation host.
3. The onboarding choice either marks onboarding complete and opens a new-project editor, or creates/returns the idempotent sample, marks it active, stores Planner as the last top-level destination, and completes onboarding.
4. `SquareToolNavigationHost` observes projects and owns the active project ID. A missing or deleted active ID falls back to the project with the greatest `lastOpenedAt`.
5. Feature routes create keyed ViewModels with small manual factories. UI state is normally a `StateFlow` collected with lifecycle awareness; repository flows remain the source of persisted truth.

Most feature ViewModels use `SharingStarted.WhileSubscribed(5_000)`. Draft editors hold unsaved fields in ViewModel state. Planner additionally stores its project ID, accessible-mode flag, and generator seed in `SavedStateHandle`; its undo/redo stacks remain ordinary memory.

## Navigation and screens

The phone shell has five stable top-level destinations. At 600 dp or wider, the bottom navigation changes to a navigation rail. Top-level navigation saves and restores destination state and stores the last selected route in DataStore.

| Destination | Current responsibility |
| --- | --- |
| Home | Current project, up to two recent projects, local project search, project actions, project preview, progress, and compact yarn/color summary |
| Planner | Active/most-recent project, Canvas grid, cell inspector, paint/select actions, locks, completion, generation, undo/redo, and accessible grid mode |
| Squares | Searchable/filterable square-design grid, details sheet, favorites, duplication, safe deletion, and square editor entry |
| Library | Searchable Colors and Palettes tabs, editor entry, palette application, and saving a project palette |
| Settings | Theme, units/defaults, Planner preferences, backup/restore entry, delete-all confirmation, accessibility shortcut, and About entry |

Secondary routes are defined centrally in `AppRoute`:

- All Projects: search, recent/alphabetic sort, favorites-only filter, open, inspect, rename, duplicate, edit, and delete.
- Project Editor: create or edit dimensions, measurements, tracking, palette, initial fill, yarn inputs, and notes; new projects inherit the preferred unit, default buffer, and default skein weight from Settings. Shrinking reports the number of discarded and assigned cells before confirmation.
- Project Insights: live project preview, design distribution, estimated color usage, dimensions, yarn estimate, progress, and export entries. It maps to Planner for primary-navigation selection.
- Export Project: PDF/PNG settings, save actions, share actions, and project-only JSON export.
- Square, Color, and Palette editors: create, edit when allowed, or duplicate into a new user-owned record.
- Accessible Planner: opens the Planner with the semantic grid enabled.
- Planner with Design: opens a project with a Squares-library design selected for painting. If no project exists, the navigation flow creates one first and then carries the selected design into Planner.
- Backup and Restore; About and Privacy.

The concrete route registry is:

| Route/pattern | Owner and arguments |
| --- | --- |
| `home`, `planner`, `squares`, `library`, `settings` | Five top-level destinations in `TopLevelDestination` |
| `projects` | All Projects |
| `project-editor/{projectId}` | Project editor; literal `new` means create |
| `insights/{projectId}` | Project Insights |
| `export/{projectId}` | Export Project |
| `accessible-planner/{projectId}` | Planner opened with semantic grid mode enabled |
| `planner-design/{projectId}/{designId}` | Planner opened with a requested paint design |
| `square-editor/{designId}/{duplicate}` | Square design editor; `new` and Boolean duplicate semantics |
| `color-editor/{colorId}/{duplicate}` | Color editor |
| `palette-editor/{paletteId}/{duplicate}/{projectId}` | Palette editor with optional active-project context; `none` means no project |
| `backup`, `about` | Backup/Restore and About/Privacy |

Insights and Planner-with-design map back to Planner for the selected top-level navigation state. Editor, export, backup, About, All Projects, and Accessible Planner hide the top-level bar/rail because they do not resolve to a top-level destination.

Onboarding is outside the navigation graph until completed. It contains three information pages and then offers either a new project or the real editable `Autumn Garden Blanket` sample.

The navigation host tracks an active project ID. If it is missing or invalid, the most recently opened project is selected. Opening Planner without any project sends the user to project creation.

Top-level navigation pops to the graph start destination with state saving, launches single-top, restores saved destination state, and writes the selected route to DataStore. Opening a project first updates its `lastOpenedAt`; failures stay on the current screen and surface a feature-specific notice.

## Screen-by-screen UI contract

### Onboarding

- Three scrollable information pages cover square design, blanket building, and project planning; a fourth choice page offers project creation or the sample.
- On page one, Skip jumps directly to the choice page. Later pages show Back. Primary actions are fixed at 56 dp height.
- The page index and onboarding action guard use saveable Compose state. Startup failures are shown through the onboarding Snackbar.

### Home

- The current card is the project with the greatest `lastOpenedAt`, breaking ties by `updatedAt`. Up to two other projects become the recent list.
- Search is local over the already observed project-card models and matches project name or notes, case-insensitively.
- Project actions are Planner, Insights, Edit, Duplicate, favorite toggle, and delete. Duplicate deliberately keeps the same name but receives a UUID, fresh timestamps, `favorite = false`, and `demoProject = false`.
- The current card combines a shared blanket preview, progress indicator when tracking is enabled, project dimensions/counts, and yarn/color summary. Reduce Motion changes the 220 ms progress animation to an immediate update.
- An empty database shows Create Project and Explore Sample Project actions.

### All Projects

- Search again matches name and notes. Favorites-only is independent of sorting.
- Recent sorting uses `updatedAt` descending with lowercase name as the tie-breaker; alphabetical sorting uses lowercase name.
- Each card exposes open, Insights, Edit, Rename, Duplicate, favorite, and Delete. Rename refuses a blank value. Delete, duplicate, and rename use explicit dialogs.
- Opening updates `lastOpenedAt`; duplication copies all cells and ordered project-palette links.

### Project Editor

- A new draft defaults to 8 rows by 12 columns, tracking enabled, blank initial fill, an empty name, empty measurements, default buffer and skein weight from Settings, and up to the first seven available colors.
- Automatic units resolve to inches for US, Liberia, and Myanmar locale country codes and centimeters elsewhere. Explicit Settings preference overrides locale.
- Editable fields are name, rows, columns, unit, optional square width/height and joining gap, tracking, project colors, initial fill for new projects, optional grams per square and skein weight, buffer percent, and notes.
- Initial fill modes are blank, one design, or a simple balanced cycle. The latter sorts selected design IDs and cycles them row-major; it is not the weighted Planner generator.
- Editing an existing project never reapplies initial fill. Shrinking is detected against stored cells and requires confirmation when any coordinates fall outside the new bounds; the dialog reports total discarded and assigned discarded cells.
- The live preview clamps invalid draft dimensions to 1..50 for display and uses the same project-preview renderer as cards.

### Planner

- The screen consists of a title bar, summary card, a Canvas or accessible grid that receives the remaining height, editing/tool controls, selected-cell inspector, and save status.
- Overflow actions own Edit Project, Insights, Export, and switching between visual and accessible grid modes.
- The summary reports dimensions, assigned count, lock count, and completion when tracking is enabled.
- Visual Canvas interaction and generator behavior are specified in the dedicated Planner sections below.

### Squares and Square Editor

- Squares uses a fixed two-column grid below 600 dp and three columns at 600 dp or wider.
- Search covers design name, note, and category after a 250 ms debounce. Filters are All, Favorites, Floral, Geometric, Simple, and Custom; category filters accept stored category text or the motif template category.
- Selecting a card opens details. Actions include favorite, Edit when user-owned, Duplicate, Use in Project, and safe Delete. If no project exists, Use in Project routes through project creation and preserves the requested design.
- A new editor starts on Classic Granny with that template's minimum three rounds, using the first available color ID when one exists.
- The editor supports template selection, ordered round colors, add/remove/reorder within the selected template's limits, name, notes, favorite, inline creation of a name/hex-only color, and a shared programmatic preview.
- Changing to a template with a smaller maximum round count requires confirmation before truncation. Changing to a larger minimum repeats the last retained color until the minimum is met.
- Editing a record marked built-in automatically becomes duplication into a new user-owned UUID; destructive deletion of built-in records is blocked.

### Library, Color Editor, and Palette Editor

- Library has Colors and Palettes tabs and a shared debounced query. Color search covers name, brand, line, shade name, and shade code; palette search covers name.
- Color cards expose Edit, Duplicate, and safe Delete. Deletion is blocked while referenced by square rounds, saved palettes, or project palettes, and the notice reports each count.
- Palette cards show up to eight swatches and, when there is an active project, can replace that project's ordered palette. The current project palette can also be saved as a new reusable palette.
- Color Editor accepts `#RRGGBB` or `#AARRGGBB`, provides synchronized hue/saturation/lightness controls, previews on explicit light and dark surfaces, and stores optional yarn brand, line, shade, code, weight, length/unit, and notes.
- Palette Editor requires a nonblank name and at least one unique color. Selection order is persisted through `displayOrder`; rows have explicit remove/up/down actions. An editor opened with project context can apply the draft order directly to that project.

### Insights

- The model combines live project, cells, designs/rounds, all colors, and the project palette.
- The hero contains the shared blanket preview plus total squares, distinct assigned designs, distinct calculated colors, progress, and yarn estimate when available.
- Distribution excludes blank cells from the percentage denominator but the domain result retains a separate blank count.
- Color usage is sorted descending. The first seven colors are shown individually and remaining colors are aggregated into Other in the summary; the underlying model retains all rows.
- Distribution uses a semantic Canvas donut plus text legend. Color usage, dimensions, yarn, and progress are conventional text/card content.
- Sections stack below 720 dp. At 720 dp or wider, distribution/color and dimensions/yarn are paired in two-column rows.
- All export/share buttons route to the single Export Project screen; Insights itself does not write files.

### Export Project

- Options are PDF paper size (Automatic/A4/Letter), row/column labels, legend, and transparent PNG background. The screen initializes labels and legend on and transparency off.
- Save PDF, Save PNG, and project JSON use `CreateDocument`. Share PDF and Share PNG create a cache file and launch an Android chooser. While an operation is running all format actions are disabled.
- Transparency affects PNG only. Paper size affects PDF only. Labels and legend are passed to both applicable exporters.
- Loading, missing-project, success, export failure, and share failure are explicit states/messages.

### Settings, Backup/Restore, and About

- Settings sections are Appearance, Units and defaults, Planner, Data and privacy, and About.
- Settings writes apply immediately to DataStore: System/Light/Dark theme, Reduce Motion, haptics, preferred units, default buffer, default skein weight, grid lines, layout-replacement confirmation, completed-cell preservation, and lock markers.
- The Accessible Grid action opens the current project in semantic mode or project creation when no project exists.
- Delete All Data requires confirmation, clears Room, clears DataStore, and returns the application to onboarding.
- Backup/Restore uses Android document contracts and an explicit replace confirmation with project/design/color/palette/cell counts.
- About displays `BuildConfig.VERSION_NAME`, the visual-planning disclaimer, the local/offline privacy statement, and a concise open-source dependency notice.

## Persistence model

`SquareToolDatabase` is Room schema version 1 (`squaretool.db`) with schema export enabled at `app/schemas/`.

| Table/model | Stored state and key relationships |
| --- | --- |
| `ProjectEntity` | ID, name, grid size, finished-square dimensions/unit, joining gap, tracking/favorite flags, notes, timestamps, generation seed, default design, yarn inputs, buffer, and sample flag |
| `SquareDesignEntity` | ID, name, motif template ID, note, favorite/built-in/category metadata, optional grams override, and timestamps |
| `SquareRoundEntity` | Composite key `(squareDesignId, roundIndex)` and referenced color; round indices are contiguous and zero-based |
| `ColorEntity` | ID, name, unsigned 32-bit ARGB, optional yarn identification/weight/length, notes, built-in flag, and timestamps |
| `PaletteEntity` | Named saved palette with built-in flag and timestamps |
| `PaletteColorCrossRef` | Ordered, unique color membership for a saved palette |
| `ProjectPaletteCrossRef` | Ordered, unique color membership for a project |
| `ProjectCellEntity` | Composite key `(projectId, rowIndex, columnIndex)`, optional design, lock/completion flags, and optional grams override |

Foreign keys cascade project-owned cells and palette links, set deleted default/cell design references to null, and restrict deletion of colors still referenced by rounds or palettes. The repository also checks design/color usage before UI-initiated deletion. User-created records use UUID strings; the optional sample uses stable IDs so creation is idempotent.

`SquareToolRepository` owns multi-table operations and validation. It fills every valid grid coordinate, preserves the top-left coordinate space on resize, creates new blank cells when growing, reports discarded cells when shrinking, and writes complete generated/edited layouts in a Room transaction. Project creation/editing, its initial layout, and project-palette membership are committed together through `saveProjectWithLayoutAndPalette`. Project duplication assigns a new project ID and copies cells and project-palette order.

Project search covers names and notes. Design search covers names, notes, and categories. Color search covers names plus yarn brand, line, shade name, and shade code; palette search covers palette names. Squares and Library apply a 250 ms query debounce.

`SettingsRepository` stores:

- onboarding completion and sample-offer/sample-created state;
- System/Light/Dark theme;
- Automatic/Centimeters/Inches preference;
- default joining/edging buffer and skein weight;
- haptics and reduce-motion choices;
- Planner grid-line, layout-confirmation, completed-cell-preservation, and lock-marker choices;
- last selected top-level destination.

Android cloud backup and device transfer are disabled both in the manifest and extraction/backup rules. Delete All Data clears Room tables and then resets DataStore, returning the app to onboarding.

### Database ordering and transaction boundaries

- Projects are observed in favorite-first, `lastOpenedAt`-descending order. Designs are favorite-first then case-insensitive name. Colors and palettes are built-in-first then case-insensitive name.
- Palette and project-palette order is data, not presentation-only state: both cross-reference tables require a unique `displayOrder` within the parent.
- Project cells are stored and read row-major by `rowIndex, columnIndex`. The in-memory `GridSnapshot` is always complete and row-major even if its factory receives a partial iterable.
- Creating a project through `saveProjectWithLayoutAndPalette` commits the project, complete initial cell set, and ordered project palette in one Room transaction.
- Editing dimensions preserves the upper-left coordinate system, deletes out-of-bounds cells, and creates blank unlocked/incomplete cells for new coordinates.
- Complete Planner replacement validates project ownership, bounds, exact cell count, and coordinate uniqueness, then deletes and reinserts the project cell set transactionally.
- Saving a design replaces all of its rounds in one transaction. Saving a palette replaces all ordered color links in one transaction.
- Applying a saved palette replaces a project's palette links. Saving a project palette into Library creates a separate palette and links; later project edits do not synchronize that saved palette.
- Project deletion relies on foreign-key cascades. Design and color deletion first rechecks reference counts in the same Room transaction used for the delete decision.
- Full restore clears and repopulates tables in foreign-key-safe order inside one Room transaction.

### Input and model validation

| Surface | Enforced constraints |
| --- | --- |
| Grid | Rows and columns 1..50; at most 2,500 cells; non-negative coordinates; no duplicate supplied coordinates |
| Project | Nonblank ID/name; optional width/height finite and greater than zero; optional gap finite and non-negative; buffer finite in 0..100 |
| Project cells | Correct project ID, in bounds, optional grams override finite and greater than zero |
| Complete layout | Exactly `rows * columns` cells and no duplicate coordinates |
| Design rounds | 3..6 at repository level, same design ID, contiguous zero-based indices; UI additionally enforces the selected template's narrower min/max |
| Color | Nonblank ID/name and unsigned 32-bit ARGB at repository level; editor validates hex and positive optional weight/length |
| Palette | Non-negative, unique display order and unique color IDs |
| Project draft | Nonblank name; dimensions 1..50; positive optional measurements/yarn values; non-negative optional gap; required designs for filled initialization; buffer 0..100 |
| Backup | Schema, IDs, duplicate keys, dimensions, ARGB, numeric ranges, motif IDs and round ranges, ordering, coordinates, settings enums/ranges, and every stored reference |

Room foreign keys remain the final integrity guard for referenced IDs. There is no independent domain service layer between ViewModels and repositories; review mutations at both the draft/UI validation and repository/DAO boundary.

### Settings defaults and backup subset

Fresh DataStore defaults are:

| Setting | Default |
| --- | --- |
| Onboarding completed | false |
| Theme | System |
| Preferred unit | Automatic |
| Joining/edging buffer | 10% |
| Skein weight | 100 g |
| Haptics | enabled |
| Reduce Motion | disabled |
| Planner grid lines | shown |
| Confirm destructive generation | enabled |
| Preserve completed cells | enabled |
| Lock markers | shown |
| Last top-level destination | Home |
| Sample offered/created | false |

The full JSON backup includes the theme, unit preference, numeric defaults, haptics, Reduce Motion, and four Planner preferences. It intentionally does not serialize onboarding completion, last navigation destination, or the sample-offered/sample-created flags.

### Optional sample data

No colors, designs, palettes, or projects are automatically seeded on a blank install. Choosing the sample creates data with stable IDs, and a later request returns the existing `demoProject` instead of duplicating it.

The sample is `Autumn Garden Blanket`: 12 rows by 8 columns, 96 assigned cells, 69 completed cells, 10 locked cells, seven project colors, six editable designs, an editable `Autumn Garden` palette, 8-inch squares, tracking enabled, 23.2 grams per square, 100-gram skeins, and a 10% buffer. The designs are Sunburst, Olive Bloom, Harvest Star, Soft Daisy, Maple Mist, and Woodland Petal. Sample records are marked `builtIn = false`; they use the same editable production models as user-created content.

## Planner state and generation

`GridSnapshot` is a complete immutable row-major grid. Missing cells supplied to its factory become blank cells; duplicate or out-of-range coordinates fail validation. `PlannerViewModel` converts between Room cells and this domain model.

Planner behavior:

- The primary Canvas supports hit testing, one-finger selection/pan, one-finger drag painting in Paint mode, two-finger pan/zoom, zoom buttons, and fit-to-screen. Scale is constrained to 0.35x through 6x relative to the fitted grid.
- Paint, Lock, and Progress modes support touch/drag editing and group one drag into one history entry. Painting skips locked cells; Progress is available only when tracking is enabled.
- Lock and completion are independent. Completion actions are hidden when tracking is disabled.
- Clear preserves locked cells. Generators always preserve locked target cells and preserve completed cells unless explicitly allowed to overwrite them.
- A conflated save channel persists the latest complete grid snapshot and exposes saving/failure state.
- `PlannerHistory` stores at least 50 operations. A new operation clears redo history; a generator application is one operation.
- Planner applies the saved grid-line, lock-marker, completed-cell-preservation, haptic, and destructive-generation-confirmation preferences. Lock marker visibility changes presentation, not the stored lock state.

### Canvas interaction and persistence

- The fitted viewport derives a cell size and offsets from canvas dimensions and grid dimensions. Recomposition for a different row/column count resets it to fit.
- A one-finger tap in Select mode selects a cell. Moving more than 8 dp changes the gesture into panning and suppresses selection.
- Paint, Lock, and Progress start a drag operation on first contact. Each coordinate is applied at most once per drag; the entire drag is recorded as one history entry.
- A second pointer cancels any in-progress edit drag before two-finger pan/zoom begins, preventing a transform gesture from committing a partial paint stroke.
- Zoom buttons multiply scale by 0.8 or 1.25 around the canvas center. Gesture and button zoom use the 0.35..6.0 scale bounds. Fit reconstructs the fitted viewport.
- Rendering culls off-screen cells and caches small/full `MotifRenderPlan` instances by visual and overlay key for the current frame state.
- The custom Canvas intentionally exposes one semantic description. Accessible mode replaces it with individually described cell controls announcing one-based row/column, design/blank state, lock state, and completion state.
- Optional haptic feedback uses `TextHandleMove` after edit actions. Haptics are supplemental; all edits also change visible/stateful content.
- The save channel is conflated: the consumer persists complete snapshots serially and may skip intermediate pending snapshots in favor of the newest state. UI exposes `isSaving` and `saveFailed`.
- `expectedDatabaseSnapshot` prevents an older observed Room value from overwriting a newer optimistic Planner snapshot while the corresponding save is pending.

`LayoutGenerator` is deterministic for the same snapshot, options, design order/weights, and seed. Supported modes are:

- weighted Random and apportioned Balanced Random;
- Checker (exactly two designs), Alternating Rows, Alternating Columns, and Diagonal;
- Horizontal and Vertical Stripes with a positive band width;
- left-to-right, right-to-left, top-to-bottom, and bottom-to-top Mirror;
- discrete Gradient Flow in five directions; and
- Radial distance bands.

Balanced generation calculates integer target counts from weights, accounts for preserved selected-design cells, fills a deterministic shuffled cell order, and optionally prefers candidates that do not match immediate orthogonal neighbors. Random and balanced modes can then perform a bounded deterministic local-swap pass; there is no unbounded backtracking. The generator sheet renders the complete proposed grid, reports per-design counts, orthogonal conflicts, and changed cells, and supports Regenerate before the user applies the result.

The project editor's `BALANCED` initial fill is deliberately simpler than Planner generation: it evenly cycles the selected, sorted design IDs across the new grid.

### Generator configuration and exact semantics

- The sheet starts in Balanced Random. Its seed initializes from the project's stored `generationSeed`, unless a seed has already been restored from `SavedStateHandle`.
- Included designs retain user-controlled order and weights while the ViewModel lives. Weights are clamped to 0.25..20.0. Stripe width is clamped to 1..50.
- Seed must parse as a signed `Long`. Regenerate increments the current seed by one and generates again.
- Random chooses by positive finite weights. Balanced Random uses largest-remainder apportionment against eligible plus preserved selected-design cells, then assigns a deterministic shuffled coordinate order.
- Duplicate design IDs in a generation request are combined by summing their valid positive weights. Blank IDs and non-positive/non-finite weights are ignored.
- Checker rejects any selection count other than exactly two. Mirror modes need no selected design because they copy existing assignments from one half to the other.
- Alternating Rows uses `row % designCount`; Alternating Columns uses `column % designCount`; Diagonal uses `(row + column) % designCount`.
- Stripe modes divide the row or column by the positive band width before cycling designs.
- Gradients convert horizontal, vertical, reversed, or averaged diagonal normalized position to a discrete design index. Radial converts normalized distance from grid center.
- Locked target cells are always excluded. Completed target cells are excluded unless overwrite is enabled. Mirror reads the source assignment even when the source itself is locked or completed; only target eligibility controls whether a copy occurs.
- Avoid-neighbors first excludes immediate assigned orthogonal neighbor IDs when possible. Random and Balanced Random then run a deterministic local swap improvement with four default passes, clamped to at most 20 passes and 20,000 attempts.
- Preview reports complete design counts, orthogonal equal-design edge conflicts, and changed cells. No-change, invalid seed, missing selection, and checker-count errors are explicit states.
- Applying a preview records one `Generate` history entry, saves the full snapshot, and updates the project's generation seed asynchronously. When confirmation is enabled, replacement is confirmed only if at least one previously assigned cell changes design.
- Clear Unlocked removes both design and completion from every unlocked cell, preserves all locked content, reports the affected count, requires confirmation, and records one undoable operation.

## Motif rendering

`MotifTemplateRegistry` defines ten code-owned templates: Classic Granny, Sunburst, Daisy, Flower Medallion, Solid Center, Star Bloom, Diamond Layers, Pinwheel, Corner Accent, and Simple Rounds. Each template declares its category, supported round range, geometry style, and normalized visual area weights.

`MotifGeometryPlanner` converts a `SquareDesignVisual` plus render configuration into a normalized `MotifRenderPlan`. Plans contain reusable rounded-square, circle, petal, diamond, arc, and polygon primitives plus selection, lock, and completion overlays. `MotifRenderer` draws that plan through Android Canvas; its `DrawScope` adapter uses the same implementation in Compose.

The renderer resolves automatic detail to a simplified path below 24 pixels. The Planner precomputes small and full plans for repeated visuals, culls cells outside the Canvas bounds, and uses the same deterministic geometry as project previews and exported files. No bitmap crochet photographs or remote images are used.

Template area weights are also the source for color-usage and per-color yarn estimates. With fewer active rounds, unused outer coverage is assigned to the outermost active color.

The code-owned template catalog is:

| ID | Display name | Category | Rounds | Base inner-to-outer area weights |
| --- | --- | --- | --- | --- |
| `classic_granny` | Classic Granny | Classic | 3..6 | 0.10, 0.14, 0.18, 0.20, 0.18, 0.20 |
| `sunburst` | Sunburst | Floral | 4..6 | 0.12, 0.17, 0.20, 0.19, 0.16, 0.16 |
| `daisy` | Daisy | Floral | 4..5 | 0.10, 0.18, 0.25, 0.24, 0.23 |
| `flower_medallion` | Flower Medallion | Floral | 4..6 | 0.08, 0.14, 0.18, 0.20, 0.19, 0.21 |
| `solid_center` | Solid Center | Simple | 3..5 | 0.32, 0.22, 0.18, 0.15, 0.13 |
| `star_bloom` | Star Bloom | Floral | 4..6 | 0.12, 0.16, 0.19, 0.20, 0.17, 0.16 |
| `diamond_layers` | Diamond Layers | Geometric | 3..6 | 0.08, 0.13, 0.16, 0.18, 0.21, 0.24 |
| `pinwheel` | Pinwheel | Geometric | 3..5 | 0.13, 0.19, 0.23, 0.23, 0.22 |
| `corner_accent` | Corner Accent | Geometric | 3..6 | 0.09, 0.14, 0.17, 0.20, 0.19, 0.21 |
| `simple_rounds` | Simple Rounds | Simple | 3..6 | 0.07, 0.13, 0.17, 0.21, 0.20, 0.22 |

Every geometry plan is normalized to a square and references round colors by index. Full geometry uses template-specific combinations of layers, petals, circles, stars/polygons, diamonds, arcs, and corner accents. Small detail substitutes simpler concentric circles or rounded layers where appropriate. Plans can add a completion wash/check, lock badge, and orange selection border; hiding a lock or completion marker removes only the overlay, not stored state.

## Calculations and insights

All displayed project values are derived from Room-backed project, cell, design, round, and color data:

- Total squares: `rows * columns`.
- Progress: completed cells divided by all grid cells, rounded to the nearest whole percent; disabled tracking returns no progress model.
- Design distribution: assigned cells counted by design ID, with blank cells counted separately in the domain result.
- Color usage: each assigned design contributes its template's normalized round weights; repeated round colors combine before the project result is normalized to percentages.
- Finished dimensions: `columns * squareWidth + (columns - 1) * joiningGap` and the equivalent row/height formula. One inch is exactly 2.54 cm.
- Yarn: each assigned cell uses cell override, then design override, then project-global grams. The buffer is applied to base grams, equivalent skeins are `totalGrams / skeinWeightGrams`, and the recommendation is rounded up with `ceil`. Missing or invalid required inputs produce no estimate.

Additional calculation boundaries:

- Unassigned cells contribute neither design/color usage nor yarn grams.
- A cell whose design is missing from the supplied design-profile map is excluded from color usage. Yarn may still use the cell override or global grams, but it cannot attribute grams to round colors without a valid profile.
- Round area weights must be non-negative, finite, and have a positive finite sum; they are normalized before aggregation.
- Repeated color IDs in one design and across designs are combined in the same color total.
- Design percentages in Insights use assigned designs as the denominator, not total grid cells. The summary count still exposes total squares separately.
- Per-color equivalent skeins divide calculated color grams by the project's single configured skein weight. Individual color `skeinWeightGrams` metadata is exported/display metadata and is not substituted into the project yarn calculation.
- Measurement formatting is locale-sensitive at presentation time, but stored numeric values are unit-tagged project values; changing the unit selection does not itself convert entered draft numbers.

Insights presents a semantic Canvas donut plus a text legend, weighted color rows, optional per-color grams/skeins, dimensions, yarn, and optional completion. Phone sections stack; at 720 dp and wider, paired sections use two columns. When there are more than seven used colors, the least significant entries are summarized as Other.

## Export and sharing

`ExportSnapshotFactory` resolves the selected project, cells, used valid designs, project-palette colors, and every color referenced by those designs into an immutable `ProjectExportSnapshot`. Missing color data is rendered with a stable neutral fallback instead of crashing.

- `ProjectPdfExporter` uses Android `PdfDocument`, chooses A4 or Letter from the option/locale, rotates wide projects to landscape, and writes a light print-oriented overview plus as many materials pages as needed. The optional legend is not silently truncated; color rows include calculated usage and configured yarn details. Grids larger than 16 rows or 12 columns also receive labeled 16 x 12 section pages.
- `ProjectPngExporter` writes an ARGB bitmap with optional labels, a complete dynamically sized legend, and transparent background. The UI uses a 2,048-pixel requested long edge; the exporter clamps requests to 512-8,192 and caps the final allocation at 16 million pixels (about 64 MiB for ARGB pixel storage).
- Both writers use `ProjectPlanRenderer` and the shared motif renderer and run off the main thread.
- Save actions use `ActivityResultContracts.CreateDocument`; no storage permission is requested.
- `ShareFileManager` creates sanitized, dated cache files under `shared_exports`, exposes only that directory through `FileProvider`, grants temporary read access, and launches `ACTION_SEND`. Files older than seven days are removed opportunistically when another share file is created.
- The Export Project screen creates a project-only JSON backup containing the selected project plus the designs, rounds, colors, palette links, and cells it needs. Full-app backup remains under Settings.

The PDF disclaimer identifies the result as a visual layout plan rather than crochet instructions.

### Export snapshot and file policy

- Snapshot creation includes only designs referenced by current project cells. It includes project-palette colors first, then any colors referenced by used design rounds, preserving first-seen order.
- Unknown motif IDs or unsupported round counts cause that design to be omitted from the export design list. Missing color records inside an otherwise valid design use neutral ARGB `0xFF8C8C80` for motif rendering.
- Legend codes are deterministic spreadsheet-style labels: A..Z, AA, AB, and so on, based on export design order.
- PDF Automatic uses Letter for US/Canada locales and A4 elsewhere. Portrait sizes are 612 x 792 points for Letter and 595 x 842 for A4; projects wider than `rows * 1.15` rotate the page.
- PDF page 1 is the overview. Materials rows are paginated without silently dropping legend/color rows. Project notes on the overview are wrapped to at most three lines.
- A grid larger than 16 rows or 12 columns receives additional row-major 16 x 12 section pages after the materials pages.
- PNG defaults to a 2,048-pixel long edge at exporter level. Requests are clamped to 512..8,192, preserve the project aspect ratio, and are reduced again when blanket plus legend would exceed 16,000,000 pixels.
- PNG uses two legend columns at blanket width 1,000 pixels or greater and one below that. The bitmap is recycled in `finally` after compression.
- Export filenames replace non-alphanumeric characters with underscores, collapse repeats, trim edges, and fall back to `SquareTool_project`. UI filenames add purpose and current date.
- Shared files live only in `cacheDir/shared_exports`. `contentUri` canonicalizes and requires the file to remain below that directory. The chooser intent grants temporary read access and sets matching `ClipData`.
- Cache files older than seven days are deleted opportunistically when another shared file is created; there is no scheduled cleanup worker.

## Backup and restore

The explicit backup format is pretty-printed JSON with `schemaVersion = 1`. Dedicated serializable DTOs cover projects, designs, rounds, colors, palettes, ordered palette links, project cells, and a selected subset of app settings. Room entities are not serialized directly. Unknown JSON keys are ignored for forward-tolerant decoding, but any schema version other than 1 is rejected.

`BackupValidator` checks IDs and duplicates, grid bounds, numeric ranges, unsigned ARGB values, round counts/order, palette order, cell coordinates, settings values, and every cross-reference. The restore screen decodes and validates the selected document, shows project/design/color/palette/cell counts, explains replacement, and requires confirmation.

After validation, the backed-up DataStore subset is applied and Room tables are cleared/repopulated in one transaction. A Room insertion failure rolls back the database replacement, and `BackupService` attempts to restore the previous settings before propagating the error. Room and DataStore cannot participate in one platform transaction, so the settings rollback is a compensating operation rather than a cross-store atomic commit.

The project-only backup filter keeps:

- exactly the selected project;
- that project's cells and ordered project-palette links;
- the project's default design plus every cell-assigned design;
- rounds belonging to those designs;
- colors referenced by those rounds or the project palette.

It removes reusable palette records, palette-color links, all other projects/content, and app settings. Its result is self-contained for the retained references and remains schema version 1.

## Accessibility, responsiveness, and theme

- The custom Planner Canvas exposes one concise description rather than thousands of invisible semantics nodes. Accessible Grid Mode instead renders visible cells as conventional semantic elements announcing row, column, design, lock, and completion state, with a normal inspector for editing.
- Insights charts have generated spoken summaries and adjacent text legends. Color swatches include names, major headings use heading semantics, important controls have test tags, and action controls generally enforce 48 dp or 56 dp minimum sizes.
- Squares uses two columns below 600 dp and three at wider widths. Primary navigation changes to a rail at 600 dp; Insights changes to paired columns at 720 dp.
- The app uses centralized Material 3 light/dark color schemes and does not opt into dynamic color. Display headings use the platform serif family; body/control text uses the platform sans-serif family.
- Spacing tokens are 4, 8, 12, 16, 20, 24, and 32 dp. Shape radii are 8, 12, 20, 26, and 28 dp. Motion duration constants are 150, 220, and 300 ms.
- `SquareToolApp` publishes the saved Reduce Motion preference through `LocalReduceMotion`. Home uses it to change the project-progress transition from 220 ms to 0 ms; Planner defines no custom nonessential moving animation that needs a separate reduced-motion branch.
- Brand anchors are olive and burnt orange over warm off-white/light surfaces or olive-charcoal/dark surfaces. Export output is always light and print-oriented, independent of app theme.

### Design tokens

| Token group | Current values |
| --- | --- |
| Spacing | 4, 8, 12, 16, 20, 24, 32 dp |
| Shape radii | 8, 12, 20, 26, 28 dp for Material extra-small through extra-large |
| Motion | Quick 150 ms, Standard 220 ms, Deliberate 300 ms |
| Display large | Serif semibold, 36/44 sp |
| Headline large | Serif semibold, 32/40 sp |
| Headline medium | Serif semibold, 28/36 sp |
| Title large | Sans-serif semibold, 22/28 sp |
| Title medium | Sans-serif semibold, 18/24 sp |
| Body large | Sans-serif, 16/24 sp |
| Body medium | Sans-serif, 14/20 sp |
| Label large/medium | Sans-serif medium, 15/20 and 13/18 sp |

The principal light scheme uses olive `#526A1D`, burnt orange `#A84013`, warm background `#FBF8F1`, and surface `#FFFDF7`. The dark scheme uses olive `#8BA44A`, orange `#FFB58C`, background `#1E1E12`, and surface `#292A1C`. `SquareToolPalette` separately exposes motif/sample accents (olive, orange, cream, mustard, sage, blush, chocolate). Dynamic color is not enabled.

The shared editor action row uses equal-width Cancel and Save controls with a 56 dp minimum height. Major standalone actions generally use 56 dp minimum height, while chips/icon controls commonly use 48 dp minimum sizing. These are recurring source patterns, not a proof that every control on every device meets an accessibility target.

### UI state, errors, and process behavior

- Screens explicitly model loading and missing-record states where asynchronous lookup can fail.
- Feature write failures become enum/sealed notices and are rendered as Snackbar or inline text/dialog state. There is no global event bus or shared error service.
- Navigation arguments use internal route strings, not external deep links. Only the launcher activity is exported.
- Home, Projects, Squares, Library, Insights, Settings, and Planner observe live flows. Export takes an immutable snapshot once when its route loads; subsequent database edits do not mutate that already loaded export screen.
- Editor drafts are not autosaved. Project, square, color, and palette changes persist only after their Save action succeeds.
- Unsaved editor drafts are kept in ViewModel/Compose state, not serialized to Room or a backup. Ordinary configuration changes normally retain the ViewModel, but process death before Save is not guaranteed to restore the draft.
- Planner edits are optimistic and asynchronously persisted as complete snapshots. Its selected coordinate, tool, include/weight ordering, and undo/redo history are not persisted across process death.
- `rememberSaveable` is used for navigation handoff IDs, onboarding page, selected dialogs, and selected Planner route flags where present; it does not make domain edits durable.

## Automated test surface

The current checkout contains 73 main Kotlin files, 27 JVM test-source Kotlin files, and 16 Android test-source Kotlin files. Source inspection finds 181 `@Test` annotations under `app/src/test` and 40 under `app/src/androidTest`. These counts describe the current source inventory, not the most recent executed result.

### JVM/Robolectric coverage by owner

| Area | Principal test classes and focus |
| --- | --- |
| Domain | `GridCalculationsTest`, `ProgressInsightsTest`, `LayoutGeneratorTest`, `PlannerHistoryTest` |
| Repository | `SquareToolRepositoryRobolectricTest`, grid snapshot mapping, backup mappers, sample factory |
| Backup | Codec, schema and reference validation, project filtering, settings rollback |
| Render | Template catalog/ranges/weights and deterministic geometry plans |
| Export | Bitmap/section policy, legend codes/material summaries, color selection/fallback snapshot |
| App | Route-to-destination mapping and obscured-touch flag rejection |
| Pure UI logic | Home selection/search; Projects models/drafts; Squares filters/drafts; Library hex/HSL/drafts; Insights model; Planner decision logic, detail choice, and viewport; localized Settings number parsing |

The debug JVM test task instruments `SquareToolRepository*.class` offline with JaCoCo before `testDebugUnitTest`, because ordinary Robolectric execution would otherwise miss the desired repository coverage. The root Sonar task depends on `:app:assembleDebug` and `:app:createDebugUnitTestCoverageReport`.

### Device/emulator coverage by owner

| Area | Principal test classes and focus |
| --- | --- |
| Startup/security | Launcher activity smoke test |
| Persistence | In-memory Room repository behavior and DataStore settings |
| Export | Real Android PDF/PNG writing |
| Navigation/onboarding | Top-level bar and onboarding choices |
| Feature UI | Home, Projects/card/editor persistence, Planner, Squares/Library, Insights, Export, Settings |

`compileDebugAndroidTestSources` proves only compilation of the instrumentation sources. `connectedDebugAndroidTest` requires a connected authorized device or running emulator and is the relevant execution command. Neither source presence nor a prior run should be treated as current device evidence without the command output/artifact.

### Build and verification commands

The concise executable commands are kept in `README.md`:

| Purpose | Command |
| --- | --- |
| Debug APK | `.\gradlew.bat assembleDebug` |
| Clean debug APK | `.\gradlew.bat clean assembleDebug` |
| JVM/Robolectric tests | `.\gradlew.bat testDebugUnitTest` |
| Debug Android lint | `.\gradlew.bat lintDebug` |
| Compile instrumentation sources | `.\gradlew.bat compileDebugAndroidTestSources` |
| Run instrumentation | `.\gradlew.bat connectedDebugAndroidTest` |

The documentation task itself should normally be validated with source review, a secrets scan of the resulting document, and `git diff --check`; Gradle execution does not fact-check prose.

## Static analysis, dependency security, and CI

The app module configures ktlint, Detekt, Compose Rules, Compose Stability Analyzer, Android security lint checks, Android lint, OWASP Dependency-Check, and SonarQube/SonarCloud integration.

- ktlint uses engine 1.8.0, Android mode, plain/checkstyle reporters, and fails on findings.
- Detekt builds on defaults, uses `config/detekt/detekt.yml`, runs in parallel, and emits checkstyle plus SARIF. Compose function naming and `LocalReduceMotion` are explicitly configured.
- Android lint aborts on errors and checks release builds. `ObsoleteSdkInt` is narrowly ignored for the API-26 adaptive icon path.
- Dependency-Check scans debug and release runtime classpaths, produces HTML/JSON/SARIF under `reports`, fails at CVSS 7 by default, fails on unused suppressions, disables OSS Index, and supports environment-controlled NVD settings.
- The three dependency suppressions are time-bounded and explain AndroidX SQLite CPE confusion, the Kotlin 2.4.10 build-cache CVE context, and Compose Stability Analyzer/GitHub CPE confusion.
- The custom Semgrep rules target backup/cleartext/exported-component/FileProvider scope, literal signing credentials/secrets, weak cryptography, unsafe WebView settings, and sensitive Android logging.
- `config/check-exceptions.json` contains expiring exact-context MobSF exceptions for ViewModel/SavedState keys, deterministic visual-layout `Random`, and source-manifest targetSdk inference. They are not blanket rule suppressions.
- `sonar-project.properties` targets SonarCloud project `Insaner1980_SquareTool`. The custom `tools/sonar.ps1` supports `-PlanOnly` and refuses the external upload without `-AllowExternalUpload` plus a configured token.
- `.deepsec/deepsec.config.ts` prioritizes manifest/FileProvider, backup, export, Room/repository, and Settings paths. Its npm scripts separate scan, process/revalidate, export, and combined report operations.

Most scripts under `tools/` are thin PowerShell delegates to `C:\Dev\Android-check\tools\InvokeProjectCheck.ps1` with project ID `squaretool`. Their command mappings are:

| Wrapper | Shared check command |
| --- | --- |
| `ac.ps1` | `android-check` |
| `bc.ps1` | `build-check` |
| `tc.ps1` | `test-check` |
| `lc.ps1` | `lint-check` |
| `cr.ps1` | `compose-rules` |
| `cs.ps1` | `compose-stability` |
| `pc.ps1` | `pmd-check` |
| `sc.ps1` | `security-check` |
| `ss.ps1` | `secret-scan` |
| `ms.ps1` | `mobsf-scan` |
| `os.ps1` | `osv-scan` |
| `dc.ps1` | `dependency-check` |
| `ga.ps1` | `google-android-security` |
| `ql.ps1` | `codeql-check` |
| `db.ps1` | `dependabot-check` |
| `ds.ps1` | `deep-sec` |

`config/android-check.json` declares debug/release variants, main/test/androidTest source sets, debug build/test tasks, connected device tests, ktlint/Detekt/lint/stability/dependency tasks, debug/release dependency graphs, and the custom Semgrep configuration.

GitHub has weekly Dependabot entries for Gradle and GitHub Actions. The CodeQL workflow runs for pushes and pull requests targeting `main`, uses pinned action SHAs, Temurin 17, manual Java/Kotlin build mode, and `:app:assembleDebug`. It does not run the JVM or connected test suites.

## Runtime privacy and security boundary

- The manifest declares no `uses-permission` entries. In particular, there is no INTERNET, network-state, broad storage, camera, microphone, contacts, location, notification, or billing permission.
- The runtime dependency graph contains no HTTP client, WebView, analytics, advertising, billing, account, or cloud SDK.
- `usesCleartextTraffic` is false. There are no application network endpoints or remote image loaders.
- The only exported component is the launcher `MainActivity`, required by its MAIN/LAUNCHER intent filter.
- `MainActivity` sets `filterTouchesWhenObscured` and independently rejects both fully and partially obscured motion events in `dispatchTouchEvent`.
- The `FileProvider` is non-exported, allows temporary URI grants, and exposes only `cache/shared_exports/`; it does not expose root, files, external storage, or the whole cache directory.
- Android automatic cloud backup and device transfer exclude root, database, shared preferences, and files, and `allowBackup` is false.
- User-selected save/restore destinations use Storage Access Framework contracts, so the app requests no broad storage permission.
- Room and DataStore are app-private. Explicit PDF/PNG/JSON save or Android sharing is the only implemented data egress.
- Source search finds no Android logging calls. Export/restore failures may display exception messages in local UI Snackbars but are not logged or uploaded.
- There is no encryption layer above Android app-private storage. Local confidentiality therefore relies on Android sandbox/device security and the user's choices when exporting or sharing.

## Known limitations and verification boundaries

- Planner undo/redo is intentionally session-only. It is not persisted through process death or application restart.
- Room schema export exists, but schema version 1 has no migration objects yet. A future database version requires an explicit migration before release.
- Room replacement and DataStore preference restoration cannot share one atomic transaction. Room data is transactional and the service compensates a failed restore by rolling settings back on a best-effort basis.
- Release signing material and a release signing configuration are intentionally absent.
- Runtime launch, TalkBack behavior, large-font layout, rotation/process recreation, export appearance, and connected instrumentation still require device or emulator verification even when JVM tests and compilation pass.
- Editor drafts are not saved across process death. Planner history, selected cell/tool, generator include/weight ordering, and pending optimistic edits have no explicit process-death restoration contract.
- Planner persistence runs in `viewModelScope` through a conflated channel. There is no separate application-close flush API; lifecycle/process-kill behavior must be verified on device.
- Backup import reads the selected stream fully with `bufferedReader().readText()` before decoding. The implementation has no explicit import byte limit or streaming parser.
- Backup validation checks supplied cells for duplicates, bounds, and references but does not require every project to supply exactly `rows * columns` persisted cells. `GridSnapshot.of` fills missing coordinates in memory when such data is later observed.
- Backup validation does not currently reject blank project/design/color/palette display names. Repository-driven interactive creation does reject the key blank names, but restore inserts validated DTOs directly.
- Project numeric drafts explicitly require finite values. Color Editor's optional weight/length check requires a parsed value greater than zero but does not separately call `isFinite`; backup validation is stricter.
- Export snapshot creation omits used designs whose motif ID is unknown or whose round count is unsupported. Their cells remain in the snapshot but render without a valid design visual/legend entry.
- Share-cache cleanup is age-based and opportunistic, and individual delete return values are ignored. There is no WorkManager cleanup schedule.
- Reduce Motion currently changes Home's progress animation. It is published app-wide, but other screens do not currently define custom nonessential transitions that consume it.
- Only English resources are present. `supportsRtl` is true at the application level, but localized copy and full RTL layout verification are absent.
- Code supports records marked `builtIn`, but a blank install seeds none. The optional sample is editable and explicitly not built-in.
- The manual QA checklist includes intended verification scenarios; unchecked or aspirational wording there is not proof that the implementation behaves that way.

## Code-review and UI-review question map

Good review questions should name the invariant, follow the complete ownership path, and request evidence at the boundary where failure would be visible. Use this map to choose that path.

| Review topic | Start here | Follow through | Evidence that matches the claim |
| --- | --- | --- | --- |
| Startup/onboarding | `SquareToolApp.kt` | `SettingsRepository`, `AppContainer.createSampleProject` | State-transition tests plus fresh-install runtime check |
| Active project/navigation | `AppNavigation.kt`, `AppRoute.kt` | project flows, `markProjectOpened`, DataStore last destination | Route unit tests and navigation/device tests |
| Project form correctness | `ProjectModels.kt` | `ProjectEditorViewModel`, repository validation/transaction | Draft unit tests, repository tests, focused Compose persistence test |
| Grid integrity/resizing | `GridModels.kt`, `GridCalculations.kt` | repository `syncGrid` and complete replacement | Domain and Robolectric/Room tests |
| Planner edit loss/races | `PlannerViewModel.kt` | history, conflated save channel, repository replacement, lifecycle | ViewModel/repository tests plus background/process device scenarios |
| Gesture correctness | `PlannerScreen.kt` Canvas pointer input | `PlannerViewport.kt`, drag grouping in ViewModel | Viewport unit tests and real multi-touch device testing |
| Generator invariants | `LayoutGenerator.kt` | Planner mode mapping, confirmation and preview UI | Deterministic unit tests for every mode and 50 x 50 performance check |
| Motif consistency | `MotifTemplate.kt`, `MotifGeometry.kt` | `MotifRenderer`, project previews, Planner, exporters | Registry/geometry tests plus visual comparison across screens/files |
| Calculation accuracy | `ProjectCalculations.kt` | Insights models and export materials summary | Pure unit tests with missing/override/repeated-color cases |
| Design/color deletion | feature ViewModel | repository usage counts, DAO FKs and transactions | Repository/Room tests including concurrent/reference cases |
| Backup trust boundary | `BackupRestoreScreen.kt` | codec, validator, mappers, service rollback, DAO restore order | Malformed/oversized/partial/cross-reference tests and failure injection |
| Export memory/layout | Export screen | snapshot factory, policy, PDF/PNG exporters | Policy unit tests, Android writer tests, device inspection for max grids |
| Share URI exposure | `ShareFileManager.kt` | manifest provider and `file_paths.xml` | Canonical-path unit review and receiving-app device test |
| Privacy/no-network claim | manifest and app dependencies | source search for clients/logging, explicit egress routes | Manifest/dependency inspection; do not infer from UI copy alone |
| Responsive layout | navigation scaffold and each screen's constraints | fixed breakpoints, scroll containers, control min sizes | Phone/landscape/foldable/tablet and large-font screenshots/tests |
| Accessibility | semantics at each custom drawing/control boundary | resources, accessible Planner, focus/touch targets | Semantics tests plus TalkBack/keyboard/device QA |
| Theme/motion | `ui/theme/`, `SquareToolApp.kt` | token consumers and `LocalReduceMotion` | Light/dark/contrast checks and reduced-motion runtime observation |
| Release readiness | `app/build.gradle.kts`, CI and wrappers | signing, R8 result, lint/security reports, device QA | Signed release artifact and completed current reports; debug build is insufficient |

For UI changes, identify all five layers that may need synchronized edits:

1. resource copy and accessibility labels;
2. Composable layout/semantics/test tags;
3. immutable UI state and user-event callbacks;
4. ViewModel transition and failure behavior;
5. repository/domain persistence or calculation impact.

For data-model changes, identify all eight propagation surfaces:

1. Room entity and exported schema/migration;
2. DAO query/order/index behavior;
3. repository transaction and validation;
4. backup DTO, mapper, validator, and project-only filter;
5. ViewModel/UI draft and editor copy;
6. calculations, previews, and exports;
7. sample data;
8. JVM, Room, Compose/device, and manual QA evidence.

For a review finding, distinguish a proven current failure from a hardening opportunity. State the exact input/state sequence, the violated invariant, the owning code, and the smallest regression test that would fail before the fix.

## Existing extension seams

These are present implementation boundaries, not promised work:

- New motifs enter through `MotifTemplateRegistry`, `MotifGeometryStyle`, and `MotifGeometryPlanner`, with renderer/weight tests beside them.
- New generators enter through `LayoutMode`, `PlannerGeneratorMode`, and their explicit mapping in `PlannerViewModel`.
- Backup evolution is isolated behind the schema constant, DTOs, validator, entity mappers, and codec.
- Database evolution is anchored by the exported Room schema and database version.
- Additional export formats can consume `ProjectExportSnapshot` without exposing Room entities to the renderer.
- New top-level destinations require synchronized changes to `TopLevelDestination`, route registration, selected-destination mapping, navigation bar/rail tests, last-destination persistence, and responsive shell behavior.
- New persistent preferences require a DataStore key/default, repository setter/mapping, Settings state/UI, backup-subset decision, and tests.
- New project fields require Room schema evolution, entity/DAO/repository paths, project draft/editor, backup DTO/mappers/validator, sample/export/calculation decisions, and migration evidence.
