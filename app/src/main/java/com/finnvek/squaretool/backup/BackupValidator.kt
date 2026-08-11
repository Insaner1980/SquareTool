package com.finnvek.squaretool.backup

import com.finnvek.squaretool.render.MotifTemplateRegistry

object BackupValidator {
    fun validate(backup: SquareToolBackupDto): BackupValidationResult {
        val errors = mutableListOf<BackupValidationError>()

        fun error(
            code: BackupValidationCode,
            path: String,
            detail: String,
        ) {
            errors += BackupValidationError(code, path, detail)
        }

        if (backup.schemaVersion != CURRENT_BACKUP_SCHEMA_VERSION) {
            error(
                BackupValidationCode.UNSUPPORTED_SCHEMA,
                "schemaVersion",
                "Expected $CURRENT_BACKUP_SCHEMA_VERSION but was ${backup.schemaVersion}",
            )
        }

        validateIds("projects", backup.projects.map { it.id }, errors)
        validateIds("squareDesigns", backup.squareDesigns.map { it.id }, errors)
        validateIds("colors", backup.colors.map { it.id }, errors)
        validateIds("palettes", backup.palettes.map { it.id }, errors)

        val projectIds = backup.projects.mapTo(mutableSetOf()) { it.id }
        val designIds = backup.squareDesigns.mapTo(mutableSetOf()) { it.id }
        val colorIds = backup.colors.mapTo(mutableSetOf()) { it.id }
        val paletteIds = backup.palettes.mapTo(mutableSetOf()) { it.id }

        backup.projects.forEachIndexed { index, project ->
            val path = "projects[$index]"
            if (project.rowCount !in 1..50 || project.columnCount !in 1..50 ||
                project.rowCount.toLong() * project.columnCount > 2_500
            ) {
                error(
                    BackupValidationCode.INVALID_DIMENSIONS,
                    path,
                    "Project dimensions must be within 1..50 and contain at most 2500 cells",
                )
            }
            validateOptionalPositive(project.squareWidthValue, "$path.squareWidthValue", errors)
            validateOptionalPositive(project.squareHeightValue, "$path.squareHeightValue", errors)
            validateOptionalNonNegative(project.joiningGapValue, "$path.joiningGapValue", errors)
            validateOptionalPositive(project.globalGramsPerSquare, "$path.globalGramsPerSquare", errors)
            validateOptionalPositive(project.skeinWeightGrams, "$path.skeinWeightGrams", errors)
            if (!project.joiningAndEdgingBufferPercent.isFinite() ||
                project.joiningAndEdgingBufferPercent !in 0.0..100.0
            ) {
                error(
                    BackupValidationCode.INVALID_NUMBER,
                    "$path.joiningAndEdgingBufferPercent",
                    "Buffer percent must be between 0 and 100",
                )
            }
            if (project.defaultSquareDesignId != null && project.defaultSquareDesignId !in designIds) {
                error(
                    BackupValidationCode.MISSING_REFERENCE,
                    "$path.defaultSquareDesignId",
                    "Referenced square design does not exist",
                )
            }
        }

        backup.colors.forEachIndexed { index, color ->
            val path = "colors[$index]"
            if (color.argb !in 0L..0xFFFF_FFFFL) {
                error(BackupValidationCode.INVALID_COLOR, "$path.argb", "ARGB must be an unsigned 32-bit value")
            }
            validateOptionalPositive(color.skeinWeightGrams, "$path.skeinWeightGrams", errors)
            validateOptionalPositive(color.yarnLength, "$path.yarnLength", errors)
        }

        backup.squareDesigns.forEachIndexed { index, design ->
            validateOptionalPositive(
                design.gramsPerSquareOverride,
                "squareDesigns[$index].gramsPerSquareOverride",
                errors,
            )
        }

        validateRounds(backup, designIds, colorIds, errors)
        validatePaletteColors(backup, paletteIds, colorIds, errors)
        validateProjectPalettes(backup, projectIds, colorIds, errors)
        validateCells(backup, projectIds, designIds, errors)
        backup.settings?.let { validateSettings(it, errors) }

        return BackupValidationResult(errors)
    }

    fun requireValid(backup: SquareToolBackupDto) {
        val result = validate(backup)
        if (!result.isValid) throw InvalidBackupException(result.errors)
    }

    private fun validateIds(
        path: String,
        ids: List<String>,
        errors: MutableList<BackupValidationError>,
    ) {
        ids.forEachIndexed { index, id ->
            if (id.isBlank()) {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.INVALID_ID,
                        "$path[$index].id",
                        "ID must not be blank",
                    )
            }
        }
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { id ->
            errors +=
                BackupValidationError(
                    BackupValidationCode.DUPLICATE_ID,
                    path,
                    "Duplicate ID: $id",
                )
        }
    }

    private fun validateRounds(
        backup: SquareToolBackupDto,
        designIds: Set<String>,
        colorIds: Set<String>,
        errors: MutableList<BackupValidationError>,
    ) {
        val duplicateKeys =
            backup.squareRounds
                .groupingBy { it.squareDesignId to it.roundIndex }
                .eachCount()
                .filterValues { it > 1 }
        duplicateKeys.keys.forEach { (designId, roundIndex) ->
            errors +=
                BackupValidationError(
                    BackupValidationCode.DUPLICATE_ID,
                    "squareRounds",
                    "Duplicate round $roundIndex for design $designId",
                )
        }

        backup.squareRounds.forEachIndexed { index, round ->
            val path = "squareRounds[$index]"
            if (round.squareDesignId !in designIds) {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.MISSING_REFERENCE,
                        "$path.squareDesignId",
                        "Referenced square design does not exist",
                    )
            }
            if (round.colorId !in colorIds) {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.MISSING_REFERENCE,
                        "$path.colorId",
                        "Referenced color does not exist",
                    )
            }
        }

        backup.squareDesigns.forEachIndexed { index, design ->
            val indices =
                backup.squareRounds
                    .asSequence()
                    .filter { it.squareDesignId == design.id }
                    .map { it.roundIndex }
                    .sorted()
                    .toList()
            val template = MotifTemplateRegistry.find(design.motifTemplateId)
            if (template == null) {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.INVALID_ID,
                        "squareDesigns[$index].motifTemplateId",
                        "Unknown motif template: ${design.motifTemplateId}",
                    )
            }
            if (indices != indices.indices.toList() ||
                (template != null && !template.supportsRoundCount(indices.size))
            ) {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.INVALID_ROUNDS,
                        "squareDesigns[$index]",
                        "Rounds must be contiguous, zero-based, and supported by the motif template",
                    )
            }
        }
    }

    private fun validatePaletteColors(
        backup: SquareToolBackupDto,
        paletteIds: Set<String>,
        colorIds: Set<String>,
        errors: MutableList<BackupValidationError>,
    ) {
        validateUniqueOrder(
            "paletteColors",
            backup.paletteColors.map { Triple(it.paletteId, it.colorId, it.displayOrder) },
            errors,
        )
        backup.paletteColors.forEachIndexed { index, value ->
            if (value.paletteId !in paletteIds) {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.MISSING_REFERENCE,
                        "paletteColors[$index].paletteId",
                        "Referenced palette does not exist",
                    )
            }
            if (value.colorId !in colorIds) {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.MISSING_REFERENCE,
                        "paletteColors[$index].colorId",
                        "Referenced color does not exist",
                    )
            }
        }
    }

    private fun validateProjectPalettes(
        backup: SquareToolBackupDto,
        projectIds: Set<String>,
        colorIds: Set<String>,
        errors: MutableList<BackupValidationError>,
    ) {
        validateUniqueOrder(
            "projectPalettes",
            backup.projectPalettes.map { Triple(it.projectId, it.colorId, it.displayOrder) },
            errors,
        )
        backup.projectPalettes.forEachIndexed { index, value ->
            if (value.projectId !in projectIds) {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.MISSING_REFERENCE,
                        "projectPalettes[$index].projectId",
                        "Referenced project does not exist",
                    )
            }
            if (value.colorId !in colorIds) {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.MISSING_REFERENCE,
                        "projectPalettes[$index].colorId",
                        "Referenced color does not exist",
                    )
            }
        }
    }

    private fun validateUniqueOrder(
        path: String,
        values: List<Triple<String, String, Int>>,
        errors: MutableList<BackupValidationError>,
    ) {
        values.forEachIndexed { index, (_, _, order) ->
            if (order < 0) {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.INVALID_ORDER,
                        "$path[$index].displayOrder",
                        "Display order must not be negative",
                    )
            }
        }
        values.groupingBy { it.first to it.second }.eachCount().filterValues { it > 1 }.keys.forEach {
            errors +=
                BackupValidationError(
                    BackupValidationCode.DUPLICATE_ID,
                    path,
                    "Duplicate relationship: ${it.first}/${it.second}",
                )
        }
        values.groupingBy { it.first to it.third }.eachCount().filterValues { it > 1 }.keys.forEach {
            errors +=
                BackupValidationError(
                    BackupValidationCode.INVALID_ORDER,
                    path,
                    "Duplicate display order ${it.second} for ${it.first}",
                )
        }
    }

    private fun validateCells(
        backup: SquareToolBackupDto,
        projectIds: Set<String>,
        designIds: Set<String>,
        errors: MutableList<BackupValidationError>,
    ) {
        val projects = backup.projects.associateBy { it.id }
        backup.projectCells
            .groupingBy { Triple(it.projectId, it.rowIndex, it.columnIndex) }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.DUPLICATE_ID,
                        "projectCells",
                        "Duplicate cell ${it.first}/${it.second}/${it.third}",
                    )
            }
        backup.projectCells.forEachIndexed { index, cell ->
            val path = "projectCells[$index]"
            if (cell.projectId !in projectIds) {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.MISSING_REFERENCE,
                        "$path.projectId",
                        "Referenced project does not exist",
                    )
            } else {
                val project = projects.getValue(cell.projectId)
                if (cell.rowIndex !in 0 until project.rowCount ||
                    cell.columnIndex !in 0 until project.columnCount
                ) {
                    errors +=
                        BackupValidationError(
                            BackupValidationCode.INVALID_CELL,
                            path,
                            "Cell coordinate is outside project dimensions",
                        )
                }
            }
            if (cell.squareDesignId != null && cell.squareDesignId !in designIds) {
                errors +=
                    BackupValidationError(
                        BackupValidationCode.MISSING_REFERENCE,
                        "$path.squareDesignId",
                        "Referenced square design does not exist",
                    )
            }
            validateOptionalPositive(cell.gramsPerSquareOverride, "$path.gramsPerSquareOverride", errors)
        }
    }

    private fun validateSettings(
        settings: BackupSettingsDto,
        errors: MutableList<BackupValidationError>,
    ) {
        if (settings.theme.lowercase() !in setOf("system", "light", "dark")) {
            errors +=
                BackupValidationError(
                    BackupValidationCode.INVALID_ID,
                    "settings.theme",
                    "Unknown theme",
                )
        }
        if (settings.preferredMeasurementUnit.lowercase() !in
            setOf("automatic", "centimeters", "inches")
        ) {
            errors +=
                BackupValidationError(
                    BackupValidationCode.INVALID_ID,
                    "settings.preferredMeasurementUnit",
                    "Unknown measurement unit",
                )
        }
        if (!settings.defaultJoiningAndEdgingBufferPercent.isFinite() ||
            settings.defaultJoiningAndEdgingBufferPercent !in 0.0..100.0
        ) {
            errors +=
                BackupValidationError(
                    BackupValidationCode.INVALID_NUMBER,
                    "settings.defaultJoiningAndEdgingBufferPercent",
                    "Buffer percent must be between 0 and 100",
                )
        }
        validatePositive(
            settings.defaultSkeinWeightGrams,
            "settings.defaultSkeinWeightGrams",
            errors,
        )
    }

    private fun validateOptionalPositive(
        value: Double?,
        path: String,
        errors: MutableList<BackupValidationError>,
    ) {
        value?.let { validatePositive(it, path, errors) }
    }

    private fun validatePositive(
        value: Double,
        path: String,
        errors: MutableList<BackupValidationError>,
    ) {
        if (!value.isFinite() || value <= 0.0) {
            errors +=
                BackupValidationError(
                    BackupValidationCode.INVALID_NUMBER,
                    path,
                    "Value must be finite and greater than zero",
                )
        }
    }

    private fun validateOptionalNonNegative(
        value: Double?,
        path: String,
        errors: MutableList<BackupValidationError>,
    ) {
        if (value != null && (!value.isFinite() || value < 0.0)) {
            errors +=
                BackupValidationError(
                    BackupValidationCode.INVALID_NUMBER,
                    path,
                    "Value must be finite and non-negative",
                )
        }
    }
}
