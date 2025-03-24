package com.adsperclick.media.views.homeActivity

import androidx.lifecycle.ViewModel
import com.adsperclick.media.data.dataModels.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SharedHomeViewModel @Inject constructor(): ViewModel()  {

    var userData : User?= null

    var idOfGroupToOpen : String?=null

    var lastSeenTimeForEachUserEachGroup : Map<String, MutableMap<String, Long?>>? =null
}

