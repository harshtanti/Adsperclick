package com.adsperclick.media.views.setting.repository

import com.adsperclick.media.api.ApiService
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import javax.inject.Inject

class SettingRepository @Inject constructor(
    private val apiService: ApiService,
    private val db: FirebaseFirestore
) {
    suspend fun updateUser(userId:String,phoneNumber:String?, file: File?) = apiService.updateUser(userId,phoneNumber,file)
}