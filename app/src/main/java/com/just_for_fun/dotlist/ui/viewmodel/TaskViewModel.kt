package com.just_for_fun.dotlist.ui.viewmodel

import androidx.lifecycle.*
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.just_for_fun.dotlist.data.local.entities.NoteFormattingEntity
import com.just_for_fun.dotlist.data.local.entities.Task
import com.just_for_fun.dotlist.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val allTasks: LiveData<List<Task>> = repository.getAllTasks().asLiveData()
    private val _taskLiveData = MutableLiveData<Task?>()
    val taskLiveData: LiveData<Task?> get() = _taskLiveData

    private val _formattingLiveData = MutableLiveData<List<NoteFormattingEntity>>()
    val formattingLiveData: LiveData<List<NoteFormattingEntity>> get() = _formattingLiveData

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            repository.getTaskById(taskId)
                .flowOn(Dispatchers.IO)
                .collect {task ->
                    _taskLiveData.postValue(task)
                }
        }
    }

    suspend fun insertTask(task: Task): Long = repository.insertTask(task)

    fun addTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(task)
        }
    }

    suspend fun deleteFormattingInRange(id: Long, selStart: Int, selEnd: Int) {
        repository.deleteFormattingInRange(id, selStart, selEnd)
    }

    suspend fun insertNoteFormatting(formatting: NoteFormattingEntity): Long {
        return repository.insertFormatting(formatting)
    }

    suspend fun getFormattingForTask(taskId: Long): List<NoteFormattingEntity> {
        return repository.getFormattingForTask(taskId)
    }
}