# SquareTool manual QA checklist

Use a clean install for first-run checks and a second run with existing data for persistence checks. Record the device/API, window size, theme, font scale, and app version with any defect.

## First launch and onboarding

- [ ] Fresh install opens without a crash and shows no account, network, permission, or paywall prompt.
- [ ] Skip, Back, and Next work on all three onboarding pages.
- [ ] Large font does not clip page titles, explanations, or actions.
- [ ] Create a new project opens the project flow.
- [ ] Explore a sample project creates the editable Autumn Garden Blanket once.
- [ ] Restart does not repeat completed onboarding.

## Empty state and project management

- [ ] With no projects, Home explains the app and offers Create Project and sample creation.
- [ ] Create a 1×1, 8×12, 20×20, and 50×50 project.
- [ ] Reject blank names, dimensions outside 1–50, more than 2,500 cells, and non-positive measurements.
- [ ] Optional width, height, joining gap, unit, tracking, palette, fill, notes, and yarn values save correctly.
- [ ] Open, rename, favorite, search, sort, duplicate, and delete a project.
- [ ] Duplicate receives new IDs while preserving visible content.
- [ ] Delete confirmation names the project and explains the result.
- [ ] Current project and recent projects never duplicate the same project on Home.
- [ ] Search matches project names and notes; no-result state is useful.

## Sample project

- [ ] Autumn Garden Blanket is 8 columns × 12 rows and contains 96 real cells.
- [ ] Exactly 69 cells are completed and compact progress displays 72%.
- [ ] Ten cells are locked and seven colors are in the project palette.
- [ ] Six or more editable production-model designs are used.
- [ ] Home, Planner, Insights, export, and backup derive values from the same stored data.

## Project editing and resizing

- [ ] Rename, dimensions, measurements, unit, joining gap, tracking, notes, palette, and yarn settings autosave.
- [ ] Enlarging preserves old cells in top-left coordinates and adds blank cells.
- [ ] Shrinking an empty edge applies without a misleading loss count.
- [ ] Shrinking populated bounds reports how many cells will be removed and requires confirmation.
- [ ] Canceling shrink leaves the project unchanged.
- [ ] Disabling tracking hides progress controls without deleting completion data; re-enabling restores it.
- [ ] Rotation or background/foreground during editing does not lose saved values.

## Colors and palettes

- [ ] Create, edit, duplicate, search, and inspect a saved color.
- [ ] Hex input accepts valid `#RRGGBB`/`#AARRGGBB` and rejects invalid text clearly.
- [ ] Hue, saturation, and lightness/value controls update the preview and hex value.
- [ ] Very light and very dark swatches retain a visible border and readable label.
- [ ] Yarn brand, line, shade, code, skein weight, length, unit, and notes persist.
- [ ] Create, duplicate, rename, search, add colors, remove colors, and reorder a palette.
- [ ] Apply a palette to a project and save a project palette into the Library.
- [ ] Deleting a used color is blocked or offers an explicit valid replacement flow.
- [ ] Library search matches color names, yarn metadata, shade metadata, and palette names.

## Square designs

- [ ] Squares uses two columns on a normal phone and at least 48dp favorite/overflow targets.
- [ ] Search and All, Favorites, Floral, Geometric, Simple, and Custom filters work.
- [ ] Create a design with every available motif template.
- [ ] Each template enforces its three-to-six-round limits.
- [ ] Reuse a color in multiple rounds and reorder supported rounds.
- [ ] Create a color inside the editor without losing the draft.
- [ ] Name, notes, favorite, category, rounds, and template persist after restart.
- [ ] Built-in designs cannot be destructively edited; duplicate creates an editable copy.
- [ ] Detail sheet expands/collapses and its Edit, Duplicate, Use in Project, Favorite, and Delete actions work.
- [ ] Deleting an in-use design is blocked or replaced explicitly.
- [ ] The visual-planning disclaimer is visible in the editor.

## Programmatic motifs

- [ ] No reference screenshot or photorealistic crochet bitmap ships in the app.
- [ ] All ten templates look structurally distinct, not merely recolored.
- [ ] All configured round colors appear and repeat colors are supported.
- [ ] Tiny previews simplify detail but retain the major design and palette.
- [ ] Home, Planner, Squares, editor, Insights, PDF, and PNG use matching geometry.
- [ ] Motifs remain legible on light and dark surfaces and in high-resolution exports.

## Planner navigation and gestures

- [ ] Planner opens the last active project or a useful chooser when none exists.
- [ ] Canvas receives most of the phone screen and summary/inspector can collapse.
- [ ] One-finger selection and painting hit the intended cell at multiple zoom levels.
- [ ] Two-finger pan/zoom does not paint cells accidentally.
- [ ] Zoom buttons and Fit to Screen work on 1×1, 8×12, 20×20, and 50×50 grids.
- [ ] Panning remains stable near every edge; no coordinate drift after repeated zooming.
- [ ] Rotation keeps project data and returns to a usable viewport.
- [ ] Grid lines follow the project/app setting.

## Planner editing

- [ ] Select shows row, column, design, lock, and completion state.
- [ ] Paint assigns the active design with tap and drag.
- [ ] Drag visits each cell once and creates one undo history entry.
- [ ] Paint skips locked cells and explains why.
- [ ] Lock toggles independently from design and completion.
- [ ] Progress mode appears only when tracking is enabled and toggles completion.
- [ ] Selected, locked, and completed overlays remain distinguishable together.
- [ ] Optional haptics can be disabled and are never the only feedback.
- [ ] Clear Unlocked Cells reports the affected count, confirms, and preserves locks.
- [ ] Autosave survives app backgrounding, destination changes, and process recreation.

## Generators

For every mode, verify Preview, Regenerate, Apply, Cancel, deterministic seed, lock preservation, completed-cell preservation by default, one-step undo, and usable 50×50 performance.

- [ ] Random with equal and custom weights.
- [ ] Balanced Random reports counts and differs by at most one for equal feasible weights.
- [ ] Checker requires exactly two designs.
- [ ] Alternating Rows cycles the ordered designs.
- [ ] Alternating Columns cycles the ordered designs.
- [ ] Diagonal cycles using row plus column.
- [ ] Horizontal Stripes respects band width.
- [ ] Vertical Stripes respects band width.
- [ ] Mirror left-to-right, right-to-left, top-to-bottom, and bottom-to-top.
- [ ] Gradient Flow left-to-right, right-to-left, top-to-bottom, bottom-to-top, and diagonal.
- [ ] Radial uses center-distance bands.
- [ ] Avoid Identical Neighbors reduces conflicts and reports unavoidable repeats.
- [ ] All-locked and all-completed layouts finish without looping or crashing.
- [ ] Too few selected designs produces a clear validation message.
- [ ] Explicit overwrite-completed confirmation is required before changing completed cells.

## Undo and redo

- [ ] Undo/redo single paint, drag paint, lock, completion, clear, generator, and mirror.
- [ ] Generator and drag paint each undo in one step.
- [ ] Redo restores the exact reverted state.
- [ ] A new edit after undo clears the redo branch.
- [ ] At least 50 meaningful operations are retained in the session.
- [ ] Restart clears history without changing persisted project data.

## Insights and calculations

- [ ] Hero totals, design count, color count, completion, and yarn values match project data.
- [ ] Blank cells are excluded or labeled explicitly in distributions.
- [ ] Donut legend contains design names, counts, and percentages and has a TalkBack summary.
- [ ] Color usage combines repeated round colors and totals approximately 100%.
- [ ] More than seven colors produce an Other summary plus a full list.
- [ ] Dimensions use columns × square width and rows × square height plus joining gaps.
- [ ] Exact 2.54 cm/in conversion is used and formatting follows locale.
- [ ] Missing measurements show the configuration prompt instead of invented dimensions.
- [ ] Yarn estimate stays hidden until grams-per-square and skein weight are valid.
- [ ] Cell override wins over design override, which wins over project global grams.
- [ ] Buffer, equivalent skeins, and rounded-up recommendation are correct and labeled estimates.
- [ ] Phone insights stack vertically; wide windows use readable columns.

## PDF, PNG, and sharing

- [ ] PDF destination uses the Storage Access Framework and canceling returns safely.
- [ ] PDF contains project metadata, dimensions when set, programmatic grid, legends, palette, estimates, completion, and disclaimer.
- [ ] Large-grid PDF includes a readable overview and enlarged sections.
- [ ] Print output is light-themed regardless of app theme.
- [ ] PNG saves blanket-only and blanket-plus-legend variants at the selected resolution.
- [ ] PNG aspect ratio is correct and very large requests are limited without OOM.
- [ ] Share PDF and Share Image create a real local file and open Android Sharesheet.
- [ ] Shared URI is a restricted FileProvider content URI; recipient can read it.
- [ ] Filenames are sanitized and contain project name, purpose, and date.
- [ ] Export failure shows an actionable error and leaves the app usable.

## Backup and restore

- [ ] Full JSON backup uses schema version 1 and contains all persistent app data intended by the format.
- [ ] Export destination uses SAF; canceling changes nothing.
- [ ] Restore rejects malformed JSON, invalid colors/dimensions, missing references, and future schema versions.
- [ ] Valid restore shows counts before an explicit replace confirmation.
- [ ] Canceling confirmation preserves existing data.
- [ ] A forced validation/restore failure leaves all existing Room data unchanged.
- [ ] Successful restore replaces data transactionally and launches into a usable state.
- [ ] Backup round-trip retains projects, cells, designs, rounds, colors, palettes, and relevant settings.

## Settings, privacy, and deletion

- [ ] Theme System, Light, and Dark apply and persist.
- [ ] Unit, buffer, skein weight, grid lines, confirmation, completed-cell, lock-marker, motion, and haptic defaults persist.
- [ ] Accessible Grid Mode shortcut opens the expected planner mode.
- [ ] About shows app/version, description, disclaimer, offline privacy statement, and licenses.
- [ ] No INTERNET, network-state, broad storage, camera, location, contacts, microphone, notification, billing, analytics, or ads permission/SDK is present.
- [ ] Delete All Data explains replacement/deletion, requires confirmation, and returns to first-run empty state.

## Accessibility and responsive behavior

- [ ] TalkBack traversal is logical on every primary and secondary screen.
- [ ] Icons, swatches, charts, errors, toggles, locks, completion, and selection have useful semantics.
- [ ] Accessible Grid Mode announces row, column, design, lock, and completion and provides large edit controls.
- [ ] Keyboard/D-pad focus remains visible and usable where supported.
- [ ] 200% font scale has no clipped actions or overlapping content.
- [ ] Light and dark themes meet readable contrast for text, controls, swatches, charts, and overlays.
- [ ] Reduce Motion removes nonessential scale/large canvas animation.
- [ ] Phone portrait, phone landscape, medium/foldable, and tablet windows remain usable.
- [ ] Compact windows use bottom navigation; wide windows use a navigation rail.
- [ ] Squares adapts from one/two columns at large font/phone to three or more on wide screens.

## Reliability and release smoke test

- [ ] Reopen every edited object after process death and device restart.
- [ ] Exercise long names, duplicate names, blank cells, missing optional settings, and empty search results.
- [ ] Verify database failure, canceled pickers, and invalid imports show errors without crashes.
- [ ] Run the complete automated verification documented in `README.md`.
- [ ] Install the resulting debug APK, launch it, and repeat the critical create → design → paint → lock → generate → undo → insights → export → backup flow.
