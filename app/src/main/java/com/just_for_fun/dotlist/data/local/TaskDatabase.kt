package com.just_for_fun.taskview

import android.content.Context
import com.just_for_fun.dotlist.data.local.AppDatabase
import com.just_for_fun.dotlist.data.local.entities.NoteFormattingEntity
import com.just_for_fun.dotlist.data.local.entities.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TaskDatabase(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val taskDao = db.taskDao()
    private val formattingDao = db.noteFormattingDao()

    fun getAllTask(): Flow<List<Task>> {
        return taskDao.getAllTasks()
    }

    fun getTaskById(id: Long): Flow<Task?> {
        return taskDao.getTaskById(id)
    }

    fun getTask(): Flow<Task?> {
        return taskDao.getTask()
    }

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun insertNoteFormatting(formatting: NoteFormattingEntity): Long {
        return formattingDao.insertFormatting(formatting)
    }

    suspend fun getFormattingForTask(taskId: Long): List<NoteFormattingEntity> {
        return formattingDao.getFormattingForTask(taskId)
    }

    suspend fun deleteNoteFormatting(formatting: NoteFormattingEntity) {
        formattingDao.deleteFormatting(formatting)
    }

    suspend fun deleteFormattingInRange(id: Long, selStart: Int, selEnd: Int) {
        formattingDao.deleteFormattingInRange(id, selStart, selEnd)
    }

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TaskDatabase(context).also { INSTANCE = it }
            }
        }
    }
}