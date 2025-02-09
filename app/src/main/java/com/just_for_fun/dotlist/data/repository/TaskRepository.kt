package com.just_for_fun.dotlist.data.repository

import com.just_for_fun.dotlist.data.local.dao.NoteFormattingDao
import com.just_for_fun.dotlist.data.local.dao.TaskDao
import com.just_for_fun.dotlist.data.local.entities.NoteFormattingEntity
import com.just_for_fun.dotlist.data.local.entities.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao, private val formattingDao: NoteFormattingDao) {

    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    fun getTaskById(id: Long): Flow<Task?> = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    suspend fun getFormattingForTask(taskId: Long): List<NoteFormattingEntity> {
        return formattingDao.getFormattingForTask(taskId)
    }
    suspend fun insertFormatting(formatting: NoteFormattingEntity): Long {
        return formattingDao.insertFormatting(formatting)
    }
    suspend fun deleteFormattingInRange(taskId: Long, selStart: Int, selEnd: Int) {
        formattingDao.deleteFormattingInRange(taskId, selStart, selEnd)
    }
}
