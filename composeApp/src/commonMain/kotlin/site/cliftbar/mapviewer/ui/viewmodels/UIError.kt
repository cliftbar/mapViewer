package site.cliftbar.mapviewer.ui.viewmodels

/**
 * Represents a user-facing error that occurred in the UI or underlying layers.
 * 
 * @property message A human-readable message describing the error.
 * @property cause The underlying [Throwable] that caused this error, if any.
 */
data class UIError(
    val message: String,
    val cause: Throwable? = null
)
