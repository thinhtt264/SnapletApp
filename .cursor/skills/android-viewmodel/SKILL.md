---
name: android-viewmodel
description: Best practices for implementing Android ViewModels, StateFlow for UI state, SharedFlow for one-off events, and handling ApiResult from repository/API (fold vs onSuccess/onFailure).
---

# Android ViewModel & State Management

## Instructions

Use `ViewModel` to hold state and business logic. It must outlive configuration changes.

### 1. UI State (StateFlow)
*   **What**: Represents the persistent state of the UI (e.g., `Loading`, `Success(data)`, `Error`).
*   **Type**: `StateFlow<UiState>`.
*   **Initialization**: Must have an initial value.
*   **Exposure**: Expose as a read-only `StateFlow` backing a private `MutableStateFlow`.
    ```kotlin
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    ```
*   **Updates**: Update state using `.update { oldState -> ... }` for thread safety.

### 2. One-Off Events (SharedFlow)
*   **What**: Transient events like "Show Toast", "Navigate to Screen", "Show Snackbar".
*   **Type**: `SharedFlow<UiEvent>`.
*   **Configuration**: Must use `replay = 0` to prevent events from re-triggering on screen rotation.
    ```kotlin
    private val _uiEvent = MutableSharedFlow<UiEvent>(replay = 0)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()
    ```
*   **Sending**: Use `.emit(event)` (suspend) or `.tryEmit(event)`.

### 3. Collecting in UI
*   **Compose**: Use `collectAsStateWithLifecycle()` for `StateFlow`.
    ```kotlin
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ```
    For `SharedFlow`, use `LaunchedEffect` with `LocalLifecycleOwner`.
*   **Views (XML)**: Use `repeatOnLifecycle(Lifecycle.State.STARTED)` within a coroutine.

### 4. Scope
*   Use `viewModelScope` for all coroutines started by the ViewModel.
*   Ideally, specific operations should be delegated to UseCases or Repositories.

### 5. Handling ApiResult from repository (API calls)

When the ViewModel calls a repository and gets `ApiResult<T>`, consume it as follows. See also **.cursor/rules/viewmodel-apiresult.mdc** (applies when editing `*ViewModel.kt`).

**Imports:** `fold` via `com.thinh.snaplet.utils.network.ApiResult`; `onSuccess`/`onFailure` via `com.thinh.snaplet.utils.network.onSuccess` and `com.thinh.snaplet.utils.network.onFailure`.

*   **Use `fold`** when building or returning state (both success and failure must be handled). Example: `_uiState.value = result.fold(onSuccess = { UiState.Success(it) }, onFailure = { UiState.Error(it.message) })`.
*   **Use `onSuccess` / `onFailure`** only for side effects (logging, snackbar, analytics); do not use them to build or return state.
*   **Never** derive UI state from `onSuccess`/`onFailure`; use **`fold`** for compiler-safe branching.
