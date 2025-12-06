package com.example.mindlog.features.auth.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.domain.usecase.CheckAutoLoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val checkAutoLoginUseCase: CheckAutoLoginUseCase
) : ViewModel() {

    private val _state = MutableLiveData<MainState>()
    val state: LiveData<MainState> get() = _state

    fun checkAutoLogin(hasCompletedTutorial: Boolean) {
        viewModelScope.launch {
            val result = checkAutoLoginUseCase(hasCompletedTutorial)
            when (result) {
                is CheckAutoLoginUseCase.Output.GoToLogin -> _state.postValue(MainState.GoLogin)
                is CheckAutoLoginUseCase.Output.GoToTutorial -> _state.postValue(MainState.GoTutorial)
                is CheckAutoLoginUseCase.Output.GoToHome -> _state.postValue(MainState.GoHome)
            }
        }
    }
}

sealed class MainState {
    object GoLogin : MainState()
    object GoTutorial : MainState()
    object GoHome : MainState()
}