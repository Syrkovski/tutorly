package com.tutorly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.repo.UserProfileRepository
import com.tutorly.models.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    userProfileRepository: UserProfileRepository
) : ViewModel() {

    val profile: StateFlow<UserProfile> = userProfileRepository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserProfile()
    )
}
