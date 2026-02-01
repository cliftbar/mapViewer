package site.cliftbar.mapviewer.ui.viewmodels

import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.CoroutineContext

/**
 * A base [ScreenModel] that provides structured error handling and coroutine safety.
 */
abstract class BaseScreenModel : ScreenModel {
    private val _uiError = MutableStateFlow<UIError?>(null)

    /**
     * A [StateFlow] of the current [UIError], if any.
     */
    val uiError: StateFlow<UIError?> = _uiError.asStateFlow()

    /**
     * A [CoroutineExceptionHandler] that captures unexpected errors and updates [uiError].
     */
    protected val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }

    /**
     * Handles an error by updating the [uiError] state.
     * 
     * @param throwable The error to handle.
     */
    protected fun handleError(throwable: Throwable) {
        _uiError.value = UIError(
            message = throwable.message ?: "An unexpected error occurred",
            cause = throwable
        )
    }

    /**
     * Clears the current [UIError].
     */
    fun clearError() {
        _uiError.value = null
    }
}
