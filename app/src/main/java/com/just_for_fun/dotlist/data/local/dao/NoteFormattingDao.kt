package com.just_for_fun.dotlist.data.local.dao

import androidx.room.*
import com.just_for_fun.dotlist.data.local.entities.NoteFormattingEntity

@Dao
interface NoteFormattingDao {
    @Query("SELECT * FROM note_formatting WHERE taskId = :taskId")
    suspend fun getFormattingForTask(taskId: Long): List<NoteFormattingEntity>

    @Insert
    suspend fun insertFormatting(formatting: NoteFormattingEntity): Long

    @Delete
    suspend fun deleteFormatting(formatting: NoteFormattingEntity)

    @Query("DELETE FROM note_formatting WHERE taskId = :taskId AND start >= :selStart AND `end` <= :selEnd")
    suspend fun deleteFormattingInRange(taskId: Long, selStart: Int, selEnd: Int)

}