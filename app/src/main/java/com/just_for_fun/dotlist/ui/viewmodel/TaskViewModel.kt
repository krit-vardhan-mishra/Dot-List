package com.just_for_fun.dotlist.ui.viewmodel

import androidx.lifecycle.*
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.just_for_fun.dotlist.data.local.entities.NoteFormattingEntity
import com.just_for_fun.dotlist.data.local.entities.Task
import com.just_for_fun.taskview.TaskDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class TaskViewModel(private val taskDatabase: TaskDatabase) : ViewModel() {

    val allTasks: LiveData<List<Task>> = taskDatabase.getAllTask().asLiveData()
    private val _taskLiveData = MutableLiveData<Task?>()
    val taskLiveData: LiveData<Task?> get() = _taskLiveData

    private val _formattingLiveData = MutableLiveData<List<NoteFormattingEntity>>()
    val formattingLiveData: LiveData<List<NoteFormattingEntity>> get() = _formattingLiveData

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            taskDatabase.getTaskById(taskId)
                .flowOn(Dispatchers.IO)
                .collect {task ->
                    _taskLiveData.postValue(task)
                }
        }
    }

    fun loadFormatting(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val formatting = taskDatabase.getFormattingForTask(taskId)
            _formattingLiveData.postValue(formatting)
        }
    }

    fun addFormatting(taskId: Long, formatting: NoteFormattingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = taskDatabase.insertNoteFormatting(formatting)
            loadFormatting(taskId)
        }
    }
}