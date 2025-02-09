package com.just_for_fun.dotlist.data.local.entities

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task (
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title")val title: String,
    @ColumnInfo(name = "notes")val notes: String,
    @ColumnInfo(name = "isChecked")val isChecked: Boolean,
    @ColumnInfo(name = "attachment")val attachment: String?,
    @ColumnInfo(name = "isExpanded")val isExpanded: Boolean = false
) {
    fun toUri(): Uri? = attachment?.let { Uri.parse(it) }
}