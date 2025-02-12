package com.adsperclick.media.views.chat.viewmodel

import androidx.lifecycle.ViewModel
import com.adsperclick.media.data.dataModels.CommonData
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ActivityScoped
class ChatViewModel@Inject constructor() :ViewModel() {
    var selectedTabPosition = 0

    var employeeList = listOf(
        // Clients (20)
        CommonData(id = "21", name = "Alice White", tagName = "Client"),
        CommonData(id = "22", name = "Bob Green", tagName = "Client"),
        CommonData(id = "23", name = "Charlie Black", tagName = "Client"),
        CommonData(id = "24", name = "Daniel Harris", tagName = "Client"),
        CommonData(id = "25", name = "Eva Scott", tagName = "Client"),
        CommonData(id = "26", name = "Frank Adams", tagName = "Client"),
        CommonData(id = "27", name = "Grace Baker", tagName = "Client"),
        CommonData(id = "28", name = "Henry Carter", tagName = "Client"),
        CommonData(id = "29", name = "Isabella King", tagName = "Client"),
        CommonData(id = "30", name = "Jack Turner", tagName = "Client"),
        CommonData(id = "31", name = "Sophia Williams", tagName = "Client"),
        CommonData(id = "32", name = "Benjamin Miller", tagName = "Client"),
        CommonData(id = "33", name = "Lucas Evans", tagName = "Client"),
        CommonData(id = "34", name = "Emma Phillips", tagName = "Client"),
        CommonData(id = "35", name = "Oliver Lewis", tagName = "Client"),
        CommonData(id = "36", name = "Chloe Walker", tagName = "Client"),
        CommonData(id = "37", name = "Jack Hall", tagName = "Client"),
        CommonData(id = "38", name = "Amelia Allen", tagName = "Client"),
        CommonData(id = "39", name = "Noah Young", tagName = "Client"),
        CommonData(id = "40", name = "Ella King", tagName = "Client"),
    )

    var clientList = listOf(
        // Employees (20)
        CommonData(id = "1", name = "John Doe", tagName = "Employee"),
        CommonData(id = "2", name = "Jane Smith", tagName = "Employee"),
        CommonData(id = "3", name = "Robert Brown", tagName = "Employee"),
        CommonData(id = "4", name = "Emily Davis", tagName = "Employee"),
        CommonData(id = "5", name = "Michael Johnson", tagName = "Employee"),
        CommonData(id = "6", name = "Sarah Lee", tagName = "Employee"),
        CommonData(id = "7", name = "David Wilson", tagName = "Employee"),
        CommonData(id = "8", name = "Olivia Martinez", tagName = "Employee"),
        CommonData(id = "9", name = "James Anderson", tagName = "Employee"),
        CommonData(id = "10", name = "Sophia Thomas", tagName = "Employee"),
        CommonData(id = "11", name = "Daniel White", tagName = "Employee"),
        CommonData(id = "12", name = "Emma Black", tagName = "Employee"),
        CommonData(id = "13", name = "Liam Harris", tagName = "Employee"),
        CommonData(id = "14", name = "Noah King", tagName = "Employee"),
        CommonData(id = "15", name = "Isabella Scott", tagName = "Employee"),
        CommonData(id = "16", name = "Lucas Carter", tagName = "Employee"),
        CommonData(id = "17", name = "Mia Adams", tagName = "Employee"),
        CommonData(id = "18", name = "Ethan Clark", tagName = "Employee"),
        CommonData(id = "19", name = "Charlotte Baker", tagName = "Employee"),
        CommonData(id = "20", name = "Mason Turner", tagName = "Employee")
    )

    fun resetSelection() {
        employeeList.forEach { it.isSelected = false }
        clientList.forEach { it.isSelected = false }
    }
}