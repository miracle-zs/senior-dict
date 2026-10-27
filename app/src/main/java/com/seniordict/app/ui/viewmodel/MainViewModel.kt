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

enum class AppScreenMode {
    HOME,
    CAMERA
}

data class DailyWord(
    val char: String,
    val pinyin: String,
    val blessing: String,
    val lifeMeaning: String
)

data class MainUiState(
    val currentScreen: AppScreenMode = AppScreenMode.HOME,
    val isFrozen: Boolean = false,
    val capturedBitmap: Bitmap? = null,
    val ocrResult: OcrPageResult? = null,
    val selectedOcrChar: OcrCharacter? = null,
    val selectedEntity: CharacterEntity? = null,
    val selectedCharStructure: String? = null,
    val isSelectedCharFavorite: Boolean = false,
    val isDetailSheetVisible: Boolean = false,
    val isSplitDialogVisible: Boolean = false,
    val isHandwritingVisible: Boolean = false,
    val isHistorySheetVisible: Boolean = false,
    val isSettingsDialogVisible: Boolean = false,
    val isTorchOn: Boolean = false,
    val splitCandidates: List<CharacterEntity> = emptyList(),
    val searchSuggestions: List<CharacterEntity> = emptyList(),
    val isContinuousReading: Boolean = false,
    val isLoading: Boolean = false,
    val toastMessage: String? = null,
    val currentCaptureSource: String = "CAMERA",
    val speechRate: Float = SeniorPreferences.RATE_MODERATE,
    val isMnemonicEnabled: Boolean = true,
    val isHapticEnabled: Boolean = true,
    val searchQuery: String = "",
    val dailyWord: DailyWord? = null,
    val dailyEntity: CharacterEntity? = null
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


    private val dailyWords = listOf(
        DailyWord("福", "fú", "福星高照 · 幸福安康", "福气安康，全家和睦幸福，福泽绵长。"),
        DailyWord("康", "kāng", "身心康健 · 松柏长青", "身体硬朗无病痛，精神矍铄有活力。"),
        DailyWord("安", "ān", "岁岁平安 · 宁静致远", "出入平安，居室安宁，心中踏实。"),
        DailyWord("寿", "shòu", "福寿双全 · 健康长寿", "寿命绵长，晚年闲适有福气。"),
        DailyWord("和", "hé", "家和万事兴 · 和气致祥", "家庭和美，邻里和睦，心平气和。"),
        DailyWord("乐", "lè", "知足常乐 · 笑口常开", "笑对生活，天天开心无烦恼。"),
        DailyWord("顺", "shùn", "万事顺意 · 从容顺遂", "事情顺利，儿孙省心，日子顺畅。"),
        DailyWord("泰", "tài", "天地泰然 · 稳如泰山", "心态平稳，安享晚年祥和清福。"),
        DailyWord("春", "chūn", "润物万新 · 春风拂面", "生机勃勃，心态年轻，朝气常在。"),
        DailyWord("健", "jiàn", "天行健也 · 步履矫健", "筋骨强健，步履从容，身心轻快。"),
        DailyWord("喜", "xǐ", "喜上眉梢 · 阖家欢喜", "常有喜事，心中愉悦，笑逐颜开。"),
        DailyWord("祥", "xiáng", "吉祥如意 · 瑞气祥云", "祥瑞满门，吉庆常伴，福禄双全。")
    )

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

        // Initialize Daily Auspicious Character for seniors
        val dayIndex = (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)) % dailyWords.size
        val today = dailyWords[dayIndex]
        _uiState.value = _uiState.value.copy(dailyWord = today)

        viewModelScope.launch {
            val entity = repository.lookupCharacter(today.char)
            if (entity != null) {
                _uiState.value = _uiState.value.copy(dailyEntity = entity)
            }
        }
    }


    /**
     * Called when mom hits the camera shutter button or selects an image from gallery
     */
    fun onPhotoCaptured(bitmap: Bitmap, source: String = "CAMERA") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isFrozen = true,
                capturedBitmap = bitmap,
                currentCaptureSource = source,
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

            val structure = repository.lookupComponentDisplay(entity.char)
            val isFav = repository.isFavorite(entity.char)
            repository.recordHistory(entity.char, _uiState.value.currentCaptureSource)

            _uiState.value = _uiState.value.copy(
                selectedOcrChar = ocrChar,
                selectedEntity = entity,
                selectedCharStructure = structure,
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
            val structure = repository.lookupComponentDisplay(entity.char)
            val isFav = repository.isFavorite(entity.char)
            repository.recordHistory(entity.char, source)

            _uiState.value = _uiState.value.copy(
                selectedEntity = entity,
                selectedCharStructure = structure,
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
        val candidateChars = if (ocrResult.characters.isNotEmpty()) {
            ocrResult.characters.map { it.char }
        } else {
            ocrResult.lines.flatMap { it.text.map { c -> c.toString() } }.filter { it.isNotBlank() }
        }.distinct()

        val entities = mutableListOf<CharacterEntity>()

        for (c in candidateChars) {
            val e = repository.lookupCharacter(c) ?: CharacterEntity(
                char = c,
                pinyin = "",
                pinyinRaw = "",
                audioKey = "",
                radicals = "部首",
                strokeCount = 0,
                definition = "生僻或未收录汉字",
                examples = null
            )
            entities.add(e)
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

    /**
     * Speak an everyday vocabulary word out loud with haptic feedback
     */
    fun speakWord(word: String) {
        audioSpeaker.speakText(word)
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
        stopContinuousReading()
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

    // Screen Mode Toggles (Home vs Camera)
    fun openCamera() {
        _uiState.value = _uiState.value.copy(currentScreen = AppScreenMode.CAMERA)
    }

    fun closeCamera() {
        unfreezeCamera()
        _uiState.value = _uiState.value.copy(currentScreen = AppScreenMode.HOME)
    }

    private var searchDebounceJob: kotlinx.coroutines.Job? = null
    private var readingFavoritesJob: kotlinx.coroutines.Job? = null

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchDebounceJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchSuggestions = emptyList())
            return
        }
        searchDebounceJob = viewModelScope.launch {
            kotlinx.coroutines.delay(180)
            val suggestions = repository.searchLiveSuggestions(query)
            _uiState.value = _uiState.value.copy(searchSuggestions = suggestions)
        }
    }

    fun speakAllScannedText() {
        val chars = _uiState.value.ocrResult?.characters?.map { it.char } ?: emptyList()
        if (chars.isNotEmpty()) {
            val text = chars.joinToString("")
            audioSpeaker.speakText(text)
        }
    }

    fun toggleContinuousReadingFavorites(favoritesList: List<FavoriteItem>) {
        if (readingFavoritesJob?.isActive == true) {
            readingFavoritesJob?.cancel()
            audioSpeaker.stop()
            _uiState.value = _uiState.value.copy(isContinuousReading = false)
        } else {
            if (favoritesList.isEmpty()) return
            _uiState.value = _uiState.value.copy(isContinuousReading = true)
            readingFavoritesJob = viewModelScope.launch {
                try {
                    for (item in favoritesList) {
                        audioSpeaker.speakCharacter(item.char, item.pinyin ?: "", item.examples)
                        kotlinx.coroutines.delay(2200)
                    }
                } finally {
                    _uiState.value = _uiState.value.copy(isContinuousReading = false)
                }
            }
        }
    }

    fun stopContinuousReading() {
        if (readingFavoritesJob?.isActive == true) {
            readingFavoritesJob?.cancel()
            audioSpeaker.stop()
            _uiState.value = _uiState.value.copy(isContinuousReading = false)
        }
    }

    fun selectScannedCharacter(charStr: String) {
        val ocrChar = _uiState.value.ocrResult?.characters?.find { it.char == charStr }
        if (ocrChar != null) {
            onCharacterTapped(ocrChar)
        } else {
            onDirectLookup(charStr, source = "CONTEXT_SWITCH")
        }
    }

    fun submitSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            val entity = repository.lookupCharacter(q)
            if (entity != null) {
                onDirectLookup(entity.char, source = "SEARCH")
                return@launch
            }

            // If user entered multiple Chinese characters (e.g. "中药" or "幸福")
            val chineseChars = q.filter { (it in '\u4e00'..'\u9fa5') || (it in '\u3400'..'\u4dbf') }.map { it.toString() }
            if (chineseChars.isNotEmpty()) {
                val firstChar = chineseChars.first()
                val firstEntity = repository.lookupCharacter(firstChar)
                if (firstEntity != null) {
                    onDirectLookup(firstChar, source = "SEARCH")
                    return@launch
                }
            }

            val pinyinMatches = repository.searchByPinyin(q)
            if (pinyinMatches.isNotEmpty()) {
                onDirectLookup(pinyinMatches.first().char, source = "SEARCH")
                return@launch
            }
            val splitMatches = repository.naturalLanguageSplitQuery(q)
            if (splitMatches.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    splitCandidates = splitMatches,
                    isSplitDialogVisible = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    toastMessage = "未找到“$q”，可尝试手写或拆字查字"
                )
            }
        }
    }

    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        audioSpeaker.shutdown()
        ocrEngine.close()
    }
}
