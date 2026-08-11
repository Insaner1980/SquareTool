package com.finnvek.squaretool.backup

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupCodec {
    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = true
            ignoreUnknownKeys = true
            prettyPrint = true
        }

    fun encode(backup: SquareToolBackupDto): String = json.encodeToString(backup)

    fun decode(value: String): SquareToolBackupDto = json.decodeFromString(value)
}
