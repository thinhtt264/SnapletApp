package com.thinh.snaplet.ui.common

import android.content.Context
import androidx.annotation.StringRes

sealed class UiText {
    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText()

    data class DynamicString(
        val value: String
    ) : UiText()

    fun asString(context: Context): String =
        when (this) {
            is DynamicString -> value
            is StringResource ->
                if (args.isEmpty())
                    context.getString(resId)
                else
                    context.getString(resId, *args.toTypedArray())
        }
}