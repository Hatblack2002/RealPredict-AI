package com.realpredict.common

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Proveedor de tiempo abstracto. Inyectable para pruebas deterministas.
 * Usa [java.time.Clock] para soportar zonas horarias y reloj fijo en tests.
 */
interface TimeProvider {
    fun now(): Instant
    fun nowIso(): String
    fun zoneId(): ZoneId
}

class DefaultTimeProvider(
    private val clock: Clock = Clock.systemUTC()
) : TimeProvider {
    override fun now(): Instant = Instant.now(clock)
    override fun nowIso(): String = DateTimeFormatter.ISO_INSTANT.format(now())
    override fun zoneId(): ZoneId = clock.zone
}

/**
 * Generador de identificadores únicos.
 * Las API Keys NUNCA deben generarse aquí — deben provenir del usuario.
 */
interface IdGenerator {
    fun newId(prefix: String = ""): String
}

class UuidIdGenerator : IdGenerator {
    override fun newId(prefix: String): String =
        if (prefix.isEmpty()) UUID.randomUUID().toString()
        else "${prefix}_${UUID.randomUUID()}"
}
