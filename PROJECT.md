<!-- generated-by: gsd-doc-writer -->
# SquareTool implementation reference

This document describes the implementation currently present in the repository. It is a source reference, not a roadmap or a statement that device-level release verification has completed.

## Product boundary

SquareTool is an offline visual planner for granny-square blankets and other square-grid projects. A user can define colored motif designs, place them manually or algorithmically, protect cells with locks, track physical completion, inspect calculated project summaries, and export a local plan.

Motifs are visual planning graphics. They are not stitch diagrams, and the app does not generate crochet instructions. Sync is not implemented. Monetization is not implemented. There is no account, backend, collaboration, analytics, advertising, AI, billing, or subscription code.

The supported grid range is 1 to 50 rows and 1 to 50 columns, with an effective maximum of 2,500 cells. The four concept images in the repository root are design references and are not packaged as runtime UI assets.

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

Onboarding is outside the navigation graph until completed. It contains three information pages and then offers either a new project or the real editable `Autumn Garden Blanket` sample.

The navigation host tracks an active project ID. If it is missing or invalid, the most recently opened project is selected. Opening Planner without any project sends the user to project creation.

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

`LayoutGenerator` is deterministic for the same snapshot, options, design order/weights, and seed. Supported modes are:

- weighted Random and apportioned Balanced Random;
- Checker (exactly two designs), Alternating Rows, Alternating Columns, and Diagonal;
- Horizontal and Vertical Stripes with a positive band width;
- left-to-right, right-to-left, top-to-bottom, and bottom-to-top Mirror;
- discrete Gradient Flow in five directions; and
- Radial distance bands.

Balanced generation calculates integer target counts from weights, accounts for preserved selected-design cells, fills a deterministic shuffled cell order, and optionally prefers candidates that do not match immediate orthogonal neighbors. Random and balanced modes can then perform a bounded deterministic local-swap pass; there is no unbounded backtracking. The generator sheet renders the complete proposed grid, reports per-design counts, orthogonal conflicts, and changed cells, and supports Regenerate before the user applies the result.

The project editor's `BALANCED` initial fill is deliberately simpler than Planner generation: it evenly cycles the selected, sorted design IDs across the new grid.

## Motif rendering

`MotifTemplateRegistry` defines ten code-owned templates: Classic Granny, Sunburst, Daisy, Flower Medallion, Solid Center, Star Bloom, Diamond Layers, Pinwheel, Corner Accent, and Simple Rounds. Each template declares its category, supported round range, geometry style, and normalized visual area weights.

`MotifGeometryPlanner` converts a `SquareDesignVisual` plus render configuration into a normalized `MotifRenderPlan`. Plans contain reusable rounded-square, circle, petal, diamond, arc, and polygon primitives plus selection, lock, and completion overlays. `MotifRenderer` draws that plan through Android Canvas; its `DrawScope` adapter uses the same implementation in Compose.

The renderer resolves automatic detail to a simplified path below 24 pixels. The Planner precomputes small and full plans for repeated visuals, culls cells outside the Canvas bounds, and uses the same deterministic geometry as project previews and exported files. No bitmap crochet photographs or remote images are used.

Template area weights are also the source for color-usage and per-color yarn estimates. With fewer active rounds, unused outer coverage is assigned to the outermost active color.

## Calculations and insights

All displayed project values are derived from Room-backed project, cell, design, round, and color data:

- Total squares: `rows * columns`.
- Progress: completed cells divided by all grid cells, rounded to the nearest whole percent; disabled tracking returns no progress model.
- Design distribution: assigned cells counted by design ID, with blank cells counted separately in the domain result.
- Color usage: each assigned design contributes its template's normalized round weights; repeated round colors combine before the project result is normalized to percentages.
- Finished dimensions: `columns * squareWidth + (columns - 1) * joiningGap` and the equivalent row/height formula. One inch is exactly 2.54 cm.
- Yarn: each assigned cell uses cell override, then design override, then project-global grams. The buffer is applied to base grams, equivalent skeins are `totalGrams / skeinWeightGrams`, and the recommendation is rounded up with `ceil`. Missing or invalid required inputs produce no estimate.

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

## Backup and restore

The explicit backup format is pretty-printed JSON with `schemaVersion = 1`. Dedicated serializable DTOs cover projects, designs, rounds, colors, palettes, ordered palette links, project cells, and a selected subset of app settings. Room entities are not serialized directly. Unknown JSON keys are ignored for forward-tolerant decoding, but any schema version other than 1 is rejected.

`BackupValidator` checks IDs and duplicates, grid bounds, numeric ranges, unsigned ARGB values, round counts/order, palette order, cell coordinates, settings values, and every cross-reference. The restore screen decodes and validates the selected document, shows project/design/color/palette/cell counts, explains replacement, and requires confirmation.

After validation, the backed-up DataStore subset is applied and Room tables are cleared/repopulated in one transaction. A Room insertion failure rolls back the database replacement, and `BackupService` attempts to restore the previous settings before propagating the error. Room and DataStore cannot participate in one platform transaction, so the settings rollback is a compensating operation rather than a cross-store atomic commit.

## Accessibility, responsiveness, and theme

- The custom Planner Canvas exposes one concise description rather than thousands of invisible semantics nodes. Accessible Grid Mode instead renders visible cells as conventional semantic elements announcing row, column, design, lock, and completion state, with a normal inspector for editing.
- Insights charts have generated spoken summaries and adjacent text legends. Color swatches include names, major headings use heading semantics, important controls have test tags, and action controls generally enforce 48 dp or 56 dp minimum sizes.
- Squares uses two columns below 600 dp and three at wider widths. Primary navigation changes to a rail at 600 dp; Insights changes to paired columns at 720 dp.
- The app uses centralized Material 3 light/dark color schemes and does not opt into dynamic color. Display headings use the platform serif family; body/control text uses the platform sans-serif family.
- Spacing tokens are 4, 8, 12, 16, 20, 24, and 32 dp. Shape radii are 8, 12, 20, 26, and 28 dp. Motion duration constants are 150, 220, and 300 ms.
- `SquareToolApp` publishes the saved Reduce Motion preference through `LocalReduceMotion`. Home uses it to change the project-progress transition from 220 ms to 0 ms; Planner defines no custom nonessential moving animation that needs a separate reduced-motion branch.
- Brand anchors are olive and burnt orange over warm off-white/light surfaces or olive-charcoal/dark surfaces. Export output is always light and print-oriented, independent of app theme.

## Automated test surface

JVM test sources cover grid validation/resizing and units, progress/distribution/color/yarn calculations, all generator families and edge cases, Planner history, motif templates/geometry, viewport math, backup codec/validation, export policy/snapshots, route mapping, sample data, and pure UI model transformations.

Android test sources cover in-memory Room repository transactions and relationships, DataStore settings, actual PDF/PNG writing, top-level navigation components, onboarding, and focused Compose screens for Home, Projects, Planner, Squares, Library, Insights, Export, and Settings.

The executable commands are documented in `README.md`. The existence or compilation of `androidTest` sources is not evidence that connected instrumentation has run; that requires a device or emulator.

## Known limitations and verification boundaries

- Planner undo/redo is intentionally session-only. It is not persisted through process death or application restart.
- Room schema export exists, but schema version 1 has no migration objects yet. A future database version requires an explicit migration before release.
- Room replacement and DataStore preference restoration cannot share one atomic transaction. Room data is transactional and the service compensates a failed restore by rolling settings back on a best-effort basis.
- Release signing material and a release signing configuration are intentionally absent.
- Runtime launch, TalkBack behavior, large-font layout, rotation/process recreation, export appearance, and connected instrumentation still require device or emulator verification even when JVM tests and compilation pass.

## Existing extension seams

These are present implementation boundaries, not promised work:

- New motifs enter through `MotifTemplateRegistry`, `MotifGeometryStyle`, and `MotifGeometryPlanner`, with renderer/weight tests beside them.
- New generators enter through `LayoutMode`, `PlannerGeneratorMode`, and their explicit mapping in `PlannerViewModel`.
- Backup evolution is isolated behind the schema constant, DTOs, validator, entity mappers, and codec.
- Database evolution is anchored by the exported Room schema and database version.
- Additional export formats can consume `ProjectExportSnapshot` without exposing Room entities to the renderer.
