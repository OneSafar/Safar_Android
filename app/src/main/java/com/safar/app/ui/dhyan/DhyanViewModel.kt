package com.safar.app.ui.dhyan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.domain.repository.MehfilRepository
import com.safar.app.util.Resource
import com.safar.app.util.YoutubeUrls
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DhyanViewModel @Inject constructor(
    private val mehfilRepository: MehfilRepository,
) : ViewModel() {

    private val _meditationVideoUrl = MutableStateFlow(YoutubeUrls.DEFAULT_MEDITATION_VIDEO_URL)
    val meditationVideoUrl = _meditationVideoUrl.asStateFlow()

    private val _isLoadingVideo = MutableStateFlow(true)
    val isLoadingVideo = _isLoadingVideo.asStateFlow()

    private val _videoError = MutableStateFlow<String?>(null)
    val videoError = _videoError.asStateFlow()

    init {
        loadMeditationVideo()
    }

    fun loadMeditationVideo() {
        viewModelScope.launch {
            _isLoadingVideo.value = true
            _videoError.value = null
            when (val result = mehfilRepository.getMeditationVideoUrl()) {
                is Resource.Success -> {
                    _meditationVideoUrl.value = result.data
                    _isLoadingVideo.value = false
                }
                is Resource.Error -> {
                    _videoError.value = result.message
                    _isLoadingVideo.value = false
                }
                is Resource.Loading -> Unit
            }
        }
    }
}
