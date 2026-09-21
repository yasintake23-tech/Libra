package com.libra.app.core.result

/**
 * Unified application error model to prevent raw exceptions from leaking into UI layers.
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class Network(
        override val message: String = "Network connection failed. Please check your connection.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Auth(
        override val message: String = "Authentication failed.",
        val code: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Database(
        override val message: String = "Database operation failed.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Storage(
        override val message: String = "Storage upload or download failed.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Validation(
        override val message: String
    ) : AppError(message)

    data class NotFound(
        override val message: String = "Requested resource was not found."
    ) : AppError(message)

    data class AiService(
        override val message: String = "AI generation service error.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Unknown(
        override val message: String = "An unexpected error occurred.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}
