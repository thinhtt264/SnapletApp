package com.thinh.snaplet.ui.app

sealed interface AppState {
    object Loading : AppState
    object ForceUpdate : AppState
}