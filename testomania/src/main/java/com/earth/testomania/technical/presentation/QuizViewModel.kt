package com.earth.testomania.technical.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earth.testomania.R
import com.earth.testomania.core.DataState
import com.earth.testomania.core.coroutines.defaultCoroutineExceptionHandler
import com.earth.testomania.technical.domain.model.SelectedAnswer
import com.earth.testomania.technical.domain.model.TechQuizWrapper
import com.earth.testomania.technical.domain.use_case.GetQuizListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val getQuizListUseCase: GetQuizListUseCase,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private var getQuizListJob: Job? = null

    private val _data = mutableStateListOf<TechQuizWrapper>()
    val data: List<TechQuizWrapper> = _data

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableSharedFlow<Int>()
    val error = _loading.asStateFlow()

    init {
        getQuizList()
    }

    private fun getQuizList() {
        getQuizListJob?.cancel()
        getQuizListJob = viewModelScope.launch(dispatcher + defaultCoroutineExceptionHandler) {
            getQuizListUseCase().catch {
                ensureActive()
                _error.emit(R.string.error_generic)
            }.collectLatest {
                ensureActive()
                when (it) {
                    is DataState.Loading -> _loading.value = true
                    is DataState.Success -> it.payload?.apply {
                        _data.addAll(this)
                    }
                    is DataState.Error -> _error.emit(R.string.error_load)
                }
            }
        }
    }

    fun saveAnswer(techQuizWrapper: TechQuizWrapper, selectedAnswerKey: String) {
        saveQuizPoint(techQuizWrapper, selectedAnswerKey)
        val index = _data.indexOf(techQuizWrapper)
        val newItem = _data.removeAt(index).let {
            it.copy(
                quiz = it.quiz,
                selectedAnswers = it.selectedAnswers.apply { add(SelectedAnswer(selectedAnswerKey, true)) }
            )
        }
        _data.add(index, newItem)
    }

    private fun saveQuizPoint(techQuizWrapper: TechQuizWrapper, selectedAnswerKey: String) {
        if (techQuizWrapper.quiz.hasMultiAnswer) return
        if (isCorrectAnswer(techQuizWrapper, selectedAnswerKey)) techQuizWrapper.point = 1
    }

    fun isCorrectAnswer(
        techQuizWrapper: TechQuizWrapper,
        possibleAnswerKey: String
    ) = techQuizWrapper.quiz.correctAnswers[possibleAnswerKey] ?: false

    fun wasSelected(techQuizWrapper: TechQuizWrapper, possibleAnswerKey: String) =
        techQuizWrapper.selectedAnswers.find { it.selectedKey == possibleAnswerKey } != null ||
                (techQuizWrapper.selectedAnswers.isNotEmpty() &&
                        !techQuizWrapper.quiz.hasMultiAnswer &&
                        isCorrectAnswer(
                            techQuizWrapper,
                            possibleAnswerKey
                        ))

    fun enableAnswerSelection(techQuizWrapper: TechQuizWrapper) =
        if (techQuizWrapper.quiz.hasMultiAnswer) !techQuizWrapper.multiSelectionWasDone.value else techQuizWrapper.selectedAnswers.isEmpty()

    fun multiSelectionWasDone(techQuizWrapper: TechQuizWrapper) {
        saveMultiSelectQuizPoint(techQuizWrapper)
        techQuizWrapper.multiSelectionWasDone.value = true
    }

    private fun saveMultiSelectQuizPoint(techQuizWrapper: TechQuizWrapper) {
        if (!techQuizWrapper.quiz.hasMultiAnswer) return
        techQuizWrapper.selectedAnswers.forEach {
            if (!isCorrectAnswer(techQuizWrapper, it.selectedKey)) return
        }
        techQuizWrapper.point = 1
    }

    fun getQuizResult() = data.sumOf {
        it.point
    }

}