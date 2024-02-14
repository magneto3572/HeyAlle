package com.hey.alle.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hey.alle.domain.model.ScreenshotListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private var handler: CoroutineExceptionHandler,
) : ViewModel() {

    private var _getScreenshotList = MutableStateFlow(ScreenshotListState())
    val getScreenshotList: StateFlow<ScreenshotListState>
        get() = _getScreenshotList

    // This function will load the screenshot from the internal storage
    fun loadScreenShot(context : Context) = viewModelScope.launch(Dispatchers.IO + handler) {
        val screenshots = async(Dispatchers.IO + handler) { fetchScreenshots(context)  }
        // Display the screenshots on the UI thread

        screenshots.await().apply {
            _getScreenshotList.update { it.copy(
                isLoading = false,
                list = this
            ) }
        }
    }

    private fun fetchScreenshots(context: Context): List<Uri> {
        val screenshots = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%screenshot%'"
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath(id.toString()).build()
                screenshots.add(contentUri)
            }
        }
        return screenshots
    }
}