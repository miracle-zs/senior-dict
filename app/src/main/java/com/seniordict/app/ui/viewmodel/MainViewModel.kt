package com.seniordict.app.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seniordict.app.data.db.CharacterEntity
import com.seniordict.app.data.db.SeniorDictDatabase
import com.seniordict.app.data.repository.DictRepository
import com.seniordict.app.engine.audio.AudioSpeaker
import com.seniordict.app.engine.ocr.OcrCharacter
import com.seniordict.app.engine.ocr.OcrLine
import com.seniordict.app.engine.ocr.OcrPageResult
import com.seniordict.app.engine.ocr.PpOcrEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val isFrozen: Boolean = false,
    val capturedBitmap: Bitmap? = null,
    val ocrResult: OcrPageResult? = null,
    val selectedOcrChar: OcrCharacter? = null,
    val selectedEntity: CharacterEntity? = null,
    val isDetailSheetVisible: Boolean = false,
    val isSplitDialogVisible: Boolean = false,
    val isHandwritingVisible: Boolean = false,
    val splitCandidates: List<CharacterEntity> = emptyList(),
    val isLoading: Boolean = false,
    val toastMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DictRepository
    private val audioSpeaker: AudioSpeaker
    private val ocrEngine: PpOcrEngine

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        val db = SeniorDictDatabase.getDatabase(application)
        repository = DictRepository(db.dictDao())
        audioSpeaker = AudioSpeaker(application)
        ocrEngine = PpOcrEngine(application)
    }

    /**
     * Called when mom hits the big camera shutter button
     */
    fun onPhotoCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isFrozen = true,
                capturedBitmap = bitmap,
                isLoading = true
            )

            // 1. Process OCR using real PP-OCRv6 Medium on-device engine
            val ocr = ocrEngine.processImage(bitmap)

            _uiState.value = _uiState.value.copy(
                ocrResult = ocr,
                isLoading = false
            )
        }
    }

    /**
     * Called when mom taps on any highlighted character box on the screen
     */
    fun onCharacterTapped(ocrChar: OcrCharacter) {
        viewModelScope.launch {
            audioSpeaker.triggerHapticFeedback()

            val entity = repository.lookupCharacter(ocrChar.char) ?: CharacterEntity(
                char = ocrChar.char,
                pinyin = "",
                pinyinRaw = "",
                audioKey = "",
                radicals = "部首",
                strokeCount = 0,
                definition = "生僻或未收录汉字",
                examples = null
            )

            _uiState.value = _uiState.value.copy(
                selectedOcrChar = ocrChar,
                selectedEntity = entity,
                isDetailSheetVisible = true
            )

            // Speak immediately
            audioSpeaker.speakCharacter(entity.char, entity.pinyin, entity.examples)
        }
    }

    fun onDirectLookup(char: String) {
        viewModelScope.launch {
            val entity = repository.lookupCharacter(char) ?: return@launch
            _uiState.value = _uiState.value.copy(
                selectedEntity = entity,
                isDetailSheetVisible = true,
                isSplitDialogVisible = false
            )
            audioSpeaker.speakCharacter(entity.char, entity.pinyin, entity.examples)
        }
    }

    fun onSplitQuery(query: String) {
        viewModelScope.launch {
            val results = repository.naturalLanguageSplitQuery(query)
            _uiState.value = _uiState.value.copy(splitCandidates = results)
        }
    }

    fun speakCurrentCharacter() {
        val entity = _uiState.value.selectedEntity ?: return
        audioSpeaker.speakCharacter(entity.char, entity.pinyin, entity.examples)
    }

    fun unfreezeCamera() {
        _uiState.value = _uiState.value.copy(
            isFrozen = false,
            capturedBitmap = null,
            ocrResult = null,
            selectedOcrChar = null,
            selectedEntity = null,
            isDetailSheetVisible = false
        )
    }

    fun openSplitDialog() {
        _uiState.value = _uiState.value.copy(isSplitDialogVisible = true)
    }

    fun closeSplitDialog() {
        _uiState.value = _uiState.value.copy(isSplitDialogVisible = false)
    }

    fun openHandwriting() {
        _uiState.value = _uiState.value.copy(isHandwritingVisible = true)
    }

    fun closeHandwriting() {
        _uiState.value = _uiState.value.copy(isHandwritingVisible = false)
    }

    fun dismissDetailSheet() {
        _uiState.value = _uiState.value.copy(isDetailSheetVisible = false)
    }

    override fun onCleared() {
        super.onCleared()
        audioSpeaker.destroy()
        ocrEngine.close()
    }
}
