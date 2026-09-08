package com.safarparmar.app.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.repository.AuthRepository
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject

private const val MAX_AVATAR_UPLOAD_BYTES = 5L * 1024L * 1024L

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStore: SafarDataStore,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val r = authRepository.getMe()) {
                is Resource.Success -> {
                    val p = r.data
                    _uiState.update { it.copy(isLoading = false, userName = p.name, userEmail = p.email, userAvatar = p.avatar, examType = p.examType ?: "", preparationStage = p.preparationStage ?: "", gender = p.gender ?: "", editName = p.name, editExamType = p.examType ?: "", editStage = p.preparationStage ?: "", editGender = p.gender ?: "") }
                }
                is Resource.Error -> {
                    val name = dataStore.userName.first() ?: ""
                    val avatar = dataStore.userAvatar.first()
                    _uiState.update { it.copy(isLoading = false, userName = name, userAvatar = avatar, editName = name) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.ShowLogoutDialog  -> _uiState.update { it.copy(showLogoutDialog = true) }
            is ProfileEvent.DismissLogoutDialog -> _uiState.update { it.copy(showLogoutDialog = false) }
            is ProfileEvent.ShowDeleteAccountDialog -> _uiState.update { it.copy(showDeleteAccountDialog = true, deleteAccountError = null) }
            is ProfileEvent.DismissDeleteAccountDialog -> _uiState.update { it.copy(showDeleteAccountDialog = false, deleteAccountError = null) }
            is ProfileEvent.DeleteAccount -> handleDeleteAccount(event.password)
            is ProfileEvent.ClearError        -> _uiState.update { it.copy(error = null) }
            is ProfileEvent.Logout            -> handleLogout()
            is ProfileEvent.SaveProfile       -> saveProfile()
            is ProfileEvent.ClearAvatarUploadSuccess -> _uiState.update { it.copy(avatarUploadSuccess = false) }
            is ProfileEvent.UploadAvatar      -> uploadAvatar(event.uri)
            is ProfileEvent.UpdateName        -> _uiState.update { it.copy(editName = event.name, nameError = null) }
            is ProfileEvent.UpdateExamType    -> _uiState.update { it.copy(editExamType = event.exam) }
            is ProfileEvent.UpdateStage       -> _uiState.update { it.copy(editStage = event.stage) }
            is ProfileEvent.UpdateGender      -> _uiState.update { it.copy(editGender = event.gender) }
        }
    }

    private fun saveProfile() {
        val s = _uiState.value
        if (s.editName.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required", error = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, nameError = null) }
            when (val r = authRepository.updateProfile(s.editName.ifBlank { null }, s.editExamType.ifBlank { null }, s.editStage.ifBlank { null }, s.editGender.ifBlank { null }, null)) {
                is Resource.Success -> _uiState.update { it.copy(isSaving = false, saveSuccess = true, userName = r.data.name, userAvatar = r.data.avatar ?: it.userAvatar, examType = r.data.examType ?: "", preparationStage = r.data.preparationStage ?: "", gender = r.data.gender ?: "") }
                is Resource.Error   -> _uiState.update { it.copy(isSaving = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        if (_uiState.value.isAvatarUploading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAvatarUploading = true, error = null, avatarUploadSuccess = false) }
            val avatarPart = withContext(Dispatchers.IO) {
                runCatching { buildAvatarPart(uri) }.getOrNull()
            }

            if (avatarPart == null) {
                _uiState.update {
                    it.copy(
                        isAvatarUploading = false,
                        error = "Could not prepare this image. Please choose another photo.",
                    )
                }
                return@launch
            }

            when (val result = authRepository.uploadAvatar(avatarPart)) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isAvatarUploading = false,
                        userAvatar = result.data,
                        avatarUploadSuccess = true,
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(
                        isAvatarUploading = false,
                        error = result.message.ifBlank { "Could not upload profile photo" },
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun buildAvatarPart(uri: Uri): MultipartBody.Part? {
        val bitmap = decodeAvatarBitmap(uri) ?: return null
        val bytes = try {
            val scaledBitmap = scaleAvatarBitmap(bitmap, maxSide = 1024)
            try {
                ByteArrayOutputStream().use { output ->
                    var compressed = false
                    for (quality in listOf(88, 76, 64)) {
                        output.reset()
                        compressed = scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                        if (compressed && output.size().toLong() <= MAX_AVATAR_UPLOAD_BYTES) break
                    }
                    if (!compressed || output.size() == 0 || output.size().toLong() > MAX_AVATAR_UPLOAD_BYTES) return null
                    output.toByteArray()
                }
            } finally {
                if (scaledBitmap !== bitmap) scaledBitmap.recycle()
            }
        } finally {
            bitmap.recycle()
        }

        val baseName = getDisplayName(uri)
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: "avatar"
        val fileName = "$baseName.jpg"
        val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("avatar", fileName, body)
    }

    private fun decodeAvatarBitmap(uri: Uri): Bitmap? {
        val resolver = appContext.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = (1024f / maxOf(info.size.width, info.size.height)).coerceAtMost(1f)
                decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            if (options.outWidth <= 0 || options.outHeight <= 0) return null
            var sample = 1
            while (maxOf(options.outWidth, options.outHeight) / (sample * 2) >= 1024) sample *= 2
            options.inJustDecodeBounds = false
            options.inSampleSize = sample
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
            // BitmapFactory does not apply camera EXIF orientation on API 26–27.
            try {
                val exif = resolver.openInputStream(uri)?.use { android.media.ExifInterface(it) }
                val orientation = exif?.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
                val matrix = android.graphics.Matrix().apply {
                    when (orientation) {
                        android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                        android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                        android.media.ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                        android.media.ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
                    }
                }
                if (matrix.isIdentity) bitmap else {
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                        if (it !== bitmap) bitmap.recycle()
                    }
                }
            } catch (exception: Exception) {
                bitmap.recycle()
                throw exception
            }
        }
    }

    private fun scaleAvatarBitmap(bitmap: Bitmap, maxSide: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longestSide = maxOf(width, height)
        if (longestSide <= maxSide || longestSide <= 0) return bitmap

        val scale = maxSide.toFloat() / longestSide.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun getDisplayName(uri: Uri): String? {
        return appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
            ?.takeIf { it.isNotBlank() }
    }

    private fun handleLogout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, showLogoutDialog = false) }
            authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false) }
        }
    }

    private fun handleDeleteAccount(password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true, deleteAccountError = null) }
            when (val r = authRepository.deleteAccount(password)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isDeletingAccount = false, showDeleteAccountDialog = false) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isDeletingAccount = false, deleteAccountError = r.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, showLogoutDialog = false) }
            authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false) }
            onDone()
        }
    }
}
