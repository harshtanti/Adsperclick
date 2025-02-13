package com.adsperclick.media.views.user.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor() :ViewModel() {
    var selectedTabPosition = 0
    var firstName:String?=null
    var lastName:String?=null
    var companyName:String?=null
    var gstNumber:String?=null
    var aadharNumber:String?=null
    var email:String?=null
    var password:String?=null
    var confirmPassword:String?=null
    var services:String?=null
    var serviceName:String?=null
}