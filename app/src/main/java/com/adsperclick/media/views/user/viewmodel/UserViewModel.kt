package com.adsperclick.media.views.user.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@ActivityScoped
class UserViewModel @Inject constructor() :ViewModel() {
    var selectedTabPosition = 0
}