package com.realpredict.common

/**
 * Resultado de operación estándar para todo el motor.
 * Permite propagar errores sin excepciones, manteniendo trazabilidad.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw error.asException()
    }

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
    }
}

sealed class AppError(open val message: String) {
    data class Validation(override val message: String, val field: String? = null) : AppError(message)
    data class NotFound(override val message: String) : AppError(message)
    data class Conflict(override val message: String) : AppError(message)
    data class Io(override val message: String, val cause: Throwable? = null) : AppError(message)
    data class Crypto(override val message: String, val cause: Throwable? = null) : AppError(message)
    data class Network(override val message: String, val httpCode: Int? = null) : AppError(message)
    data class Auth(override val message: String) : AppError(message)
    data class Quota(override val message: String) : AppError(message)
    data class AiResponse(override val message: String, val raw: String? = null) : AppError(message)
    data class DataIntegrity(override val message: String) : AppError(message)
    data class Unknown(override val message: String, val cause: Throwable? = null) : AppError(message)

    fun asException(): Exception = when (this) {
        is Validation -> IllegalArgumentException("[$field] $message")
        is NotFound -> NoSuchElementException(message)
        is Conflict -> IllegalStateException(message)
        is Io -> java.io.IOException(message, cause)
        is Crypto -> SecurityException(message, cause)
        is Network -> RuntimeException("HTTP ${httpCode ?: "?"}: $message")
        is Auth -> SecurityException("AUTH: $message")
        is Quota -> RuntimeException("QUOTA: $message")
        is AiResponse -> RuntimeException("AI: $message")
        is DataIntegrity -> IllegalStateException("INTEGRITY: $message")
        is Unknown -> RuntimeException(message, cause)
    }
}
