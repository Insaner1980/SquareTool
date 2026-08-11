package com.finnvek.squaretool.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        SquareDesignEntity::class,
        SquareRoundEntity::class,
        ColorEntity::class,
        PaletteEntity::class,
        PaletteColorCrossRef::class,
        ProjectPaletteCrossRef::class,
        ProjectCellEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SquareToolDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    abstract fun projectCellDao(): ProjectCellDao

    abstract fun squareDesignDao(): SquareDesignDao

    abstract fun colorDao(): ColorDao

    abstract fun paletteDao(): PaletteDao

    abstract fun backupDao(): BackupDao

    companion object {
        private const val DATABASE_NAME = "squaretool.db"

        fun create(context: Context): SquareToolDatabase =
            Room
                .databaseBuilder(context.applicationContext, SquareToolDatabase::class.java, DATABASE_NAME)
                .build()
    }
}
