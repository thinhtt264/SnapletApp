package com.thinh.snaplet.ui.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class OverlayViewModel @Inject constructor() : ViewModel() {

    private val _overlayState = MutableStateFlow<OverlayState>(OverlayState.None)
    val overlayState: StateFlow<OverlayState> = _overlayState.asStateFlow()

    init {
        OverlayEventBus.events.onEach { event ->
            when (event) {
                is OverlayEvent.ShowBottomSheet -> _overlayState.update {
                    OverlayState.Visible.BottomSheet(event.content)
                }

                is OverlayEvent.ShowModal -> _overlayState.update {
                    OverlayState.Visible.Modal(event.content)
                }

                is OverlayEvent.Dismiss -> _overlayState.update { OverlayState.None }
            }
        }.launchIn(viewModelScope)
    }

    fun dismiss() {
        OverlayEventBus.dismiss()
    }
}