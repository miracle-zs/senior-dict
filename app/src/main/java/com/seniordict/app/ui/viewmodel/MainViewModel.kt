package com.seniordict.app.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seniordict.app.data.db.CharacterEntity
import com.seniordict.app.data.db.FavoriteItem
import com.seniordict.app.data.db.HistoryItem
import com.seniordict.app.data.db.SeniorDictDatabase
import com.seniordict.app.data.preferences.SeniorPreferences
import com.seniordict.app.data.repository.DictRepository
import com.seniordict.app.engine.audio.AudioSpeaker
import com.seniordict.app.engine.ocr.OcrCharacter
import com.seniordict.app.engine.ocr.OcrPageResult
import com.seniordict.app.engine.ocr.PpOcrEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val isFrozen: Boolean = false,
    val capturedBitmap: Bitmap? = null,
    val ocrResult: OcrPageResult? = null,
    val selectedOcrChar: OcrCharacter? = null,
    val selectedEntity: CharacterEntity? = null,
    val isSelectedCharFavorite: Boolean = false,
    val isDetailSheetVisible: Boolean = false,
    val isSplitDialogVisible: Boolean = false,
    val isHandwritingVisible: Boolean = false,
    val isHistorySheetVisible: Boolean = false,
    val isSettingsDialogVisible: Boolean = false,
    val isTorchOn: Boolean = false,
    val splitCandidates: List<CharacterEntity> = emptyList(),
    val isLoading: Boolean = false,
    val toastMessage: String? = null,
    val speechRate: Float = SeniorPreferences.RATE_MODERATE,
    val isMnemonicEnabled: Boolean = true,
    val isHapticEnabled: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DictRepository
    private val preferences: SeniorPreferences
    private val audioSpeaker: AudioSpeaker
    private val ocrEngine: PpOcrEngine

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val favorites: StateFlow<List<FavoriteItem>>
    val history: StateFlow<List<HistoryItem>>

    init {
        val db = SeniorDictDatabase.getDatabase(application)
        repository = DictRepository(db.dictDao())
        preferences = SeniorPreferences(application)
        audioSpeaker = AudioSpeaker(application, preferences)
        ocrEngine = PpOcrEngine(application)

        _uiState.value = _uiState.value.copy(
            speechRate = preferences.speechRate,
            isMnemonicEnabled = preferences.isMnemonicEnabled,
            isHapticEnabled = preferences.isHapticEnabled
        )

        favorites = repository.getAllFavoritesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        history = repository.getRecentHistoryFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    /**
     * Called when mom hits the camera shutter button or selects an image from gallery
     */
    fun onPhotoCaptured(bitmap: Bitmap, source: String = "CAMERA") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isFrozen = true,
                capturedBitmap = bitmap,
                isLoading = true
            )

            // Process OCR using PP-OCRv6 engine
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

            val isFav = repository.isFavorite(entity.char)
            repository.recordHistory(entity.char, "CAMERA")

            _uiState.value = _uiState.value.copy(
                selectedOcrChar = ocrChar,
                selectedEntity = entity,
                isSelectedCharFavorite = isFav,
                isDetailSheetVisible = true
            )

            // Speak immediately
            audioSpeaker.speakCharacter(entity.char, entity.pinyin, entity.examples)
        }
    }

    fun onDirectLookup(char: String, source: String = "MANUAL") {
        viewModelScope.launch {
            val entity = repository.lookupCharacter(char) ?: return@launch
            val isFav = repository.isFavorite(entity.char)
            repository.recordHistory(entity.char, source)

            _uiState.value = _uiState.value.copy(
                selectedEntity = entity,
                isSelectedCharFavorite = isFav,
                isDetailSheetVisible = true,
                isSplitDialogVisible = false,
                isHandwritingVisible = false
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

    /**
     * Real offline handwriting recognition: rasterizes canvas strokes to bitmap,
     * feeds into PP-OCR recognition, and finds candidate character entities in DB!
     */
    suspend fun recognizeHandwriting(bitmap: Bitmap): List<CharacterEntity> {
        val ocrResult = ocrEngine.processImage(bitmap)
        val candidateChars = ocrResult.characters.map { it.char }.distinct()
        val entities = mutableListOf<CharacterEntity>()

        for (c in candidateChars) {
            val e = repository.lookupCharacter(c)
            if (e != null) {
                entities.add(e)
            }
        }
        return entities
    }

    fun toggleFavorite(char: String) {
        viewModelScope.launch {
            val newFavState = repository.toggleFavorite(char)
            _uiState.value = _uiState.value.copy(isSelectedCharFavorite = newFavState)
            audioSpeaker.triggerHapticFeedback()
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun speakCurrentCharacter() {
        val entity = _uiState.value.selectedEntity ?: return
        audioSpeaker.speakCharacter(entity.char, entity.pinyin, entity.examples)
    }

    fun speakCharacterByInfo(char: String, pinyin: String?, examples: String?) {
        audioSpeaker.speakCharacter(char, pinyin ?: "", examples)
    }

    fun toggleTorch(turnOn: Boolean) {
        _uiState.value = _uiState.value.copy(isTorchOn = turnOn)
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

    fun setSpeechRate(rate: Float) {
        preferences.speechRate = rate
        audioSpeaker.applySpeechRate(rate)
        _uiState.value = _uiState.value.copy(speechRate = rate)
    }

    fun setMnemonicEnabled(enabled: Boolean) {
        preferences.isMnemonicEnabled = enabled
        _uiState.value = _uiState.value.copy(isMnemonicEnabled = enabled)
    }

    fun setHapticEnabled(enabled: Boolean) {
        preferences.isHapticEnabled = enabled
        _uiState.value = _uiState.value.copy(isHapticEnabled = enabled)
    }

    // Dialog & Sheet Visibility Toggles
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

    fun openHistorySheet() {
        _uiState.value = _uiState.value.copy(isHistorySheetVisible = true)
    }

    fun closeHistorySheet() {
        _uiState.value = _uiState.value.copy(isHistorySheetVisible = false)
    }

    fun openSettingsDialog() {
        _uiState.value = _uiState.value.copy(isSettingsDialogVisible = true)
    }

    fun closeSettingsDialog() {
        _uiState.value = _uiState.value.copy(isSettingsDialogVisible = false)
    }

    fun dismissDetailSheet() {
        _uiState.value = _uiState.value.copy(isDetailSheetVisible = false)
    }

    override fun onCleared() {
        super.onCleared()
        audioSpeaker.shutdown()
        ocrEngine.close()
    }
}
