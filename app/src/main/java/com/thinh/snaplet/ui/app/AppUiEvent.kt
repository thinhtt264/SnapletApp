package com.thinh.snaplet.ui.app

sealed interface AppUiEvent {
    data object NavigateToAuthGraph : AppUiEvent

    data object NavigateToHomeGraph : AppUiEvent
}
