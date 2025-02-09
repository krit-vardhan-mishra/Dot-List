package com.just_for_fun.dotlist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.just_for_fun.taskview.TaskDatabase

class TaskViewModelFactory(private val taskDatabase: TaskDatabase): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom((TaskViewModel::class.java))) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(taskDatabase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}