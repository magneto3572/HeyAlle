package com.hey.alle.domain.model

import android.net.Uri

data class ScreenshotListState(
    val isLoading: Boolean = true,
    val list: List<Uri> = emptyList()
)