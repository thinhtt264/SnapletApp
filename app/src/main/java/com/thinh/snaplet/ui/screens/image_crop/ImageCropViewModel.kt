package com.thinh.snaplet.ui.screens.image_crop

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class ImageCropViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val imageUri: String? = run {
        val encoded: String = savedStateHandle.get<String>("imageUri") ?: return@run null
        try {
            val decoded = Base64.decode(
                encoded.toByteArray(StandardCharsets.US_ASCII),
                Base64.URL_SAFE or Base64.NO_WRAP
            )
            String(decoded, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private val _uiState = MutableStateFlow(
        ImageCropUiState(imageUri = imageUri)
    )

    val uiState: StateFlow<ImageCropUiState> = _uiState.asStateFlow()
}
