---
name: compose-ui
description: Best practices for building UI with Jetpack Compose per Android Architecture Recommendations. Unidirectional Data Flow (UDF), lifecycle-aware state collection, plain state holders for reusable UI, performance, and theming.
---

# Jetpack Compose Best Practices

Follow [Android Architecture Recommendations](https://developer.android.com/topic/architecture/recommendations) (strongly recommended for Compose).

## Instructions

### 1. Unidirectional Data Flow (UDF) — Strongly Recommended
*   **State flows down**: ViewModel exposes state; Composables receive it as parameters.
*   **Events flow up**: User actions go up as callbacks (e.g., `onClick`, `onValueChange`); ViewModel processes them and updates state.
*   **ViewModel Integration**: Screen-level Composables use `viewModel.uiState.collectAsStateWithLifecycle()` and pass state down. Never collect flows without lifecycle awareness.

### 2. State Hoisting
Make Composables **stateless** whenever possible by moving state to the caller.

*   **Pattern**:
    ```kotlin
    @Composable
    fun MyComponent(
        value: String,              // State flows down
        onValueChange: (String) -> Unit, // Events flow up
        modifier: Modifier = Modifier
    )
    ```
*   **ViewModel at screen level only**: Use ViewModels in screen composables or navigation destinations. For reusable UI components, use plain state holder classes—not ViewModels.

### 3. Lifecycle-Aware State Collection — Strongly Recommended
*   **Always** use `collectAsStateWithLifecycle()` when collecting `StateFlow` from ViewModel.
    ```kotlin
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ```
*   **Never** use raw `collectAsState()` or `collect` without lifecycle handling.

### 4. Modifiers
*   **Default Parameter**: Always provide a `modifier: Modifier = Modifier` as the first optional parameter.
*   **Application**: Apply this `modifier` to the *root* layout element of your Composable.
*   **Ordering matters**: `padding().clickable()` is different from `clickable().padding()`. Generally apply layout-affecting modifiers (like padding) *after* click listeners if you want the padding to be clickable.

### 5. Performance Optimization
*   **`remember`**: Use `remember { ... }` to cache expensive calculations across recompositions.
*   **`derivedStateOf`**: Use `derivedStateOf { ... }` when a state changes frequently (like scroll position) but the UI only needs to react to a threshold or summary (e.g., show "Jump to Top" button). This prevents unnecessary recompositions.
    ```kotlin
    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    ```
*   **Lambda Stability**: Prefer method references (e.g., `viewModel::onEvent`) or remembered lambdas to prevent unstable types from triggering recomposition of children.

### 6. Theming and Resources
*   Use `MaterialTheme.colorScheme` and `MaterialTheme.typography` instead of hardcoded colors or text styles.
*   Organize simple UI components into specific files (e.g., `DesignSystem.kt` or `Components.kt`) if they are shared across features.

### 7. Previews
*   Create a private preview function for every public Composable.
*   Use `@Preview(showBackground = true)` and include Light/Dark mode previews if applicable.
*   Pass dummy data (static) to the stateless Composable for the preview.
