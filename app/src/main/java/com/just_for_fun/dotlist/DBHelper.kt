package com.just_for_fun.dotlist

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Database Helper Class
class DBHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(TABLE_CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COLUMN_NOTES TEXT")
            db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COLUMN_TIMESTAMP INTEGER DEFAULT ${System.currentTimeMillis()}")
            db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COLUMN_POSITION INTEGER DEFAULT 0")
        }
    }

    // Insert Task
    fun insertTask(task: Task): Long {
        var db: SQLiteDatabase? = null
        try {
            db = writableDatabase
            val values = ContentValues()

            values.put(COLUMN_TITLE, task.title)
            values.put(COLUMN_IS_DONE, if (task.isCompleted) 1 else 0)
            values.put(COLUMN_POSITION, task.position)

            task.details?.let {
                values.put(COLUMN_NOTES, it.notes)
                values.put(COLUMN_FILE_PATH, it.filePath)
            }

            values.put(COLUMN_TIMESTAMP, System.currentTimeMillis())

            val id = db.insert(TABLE_TASKS, null, values)
            return id
        } finally {
            db?.close()
        }
    }

    // Update Task
    fun updateTask(task: Task): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, task.title)
            put(COLUMN_IS_DONE, if (task.isCompleted) 1 else 0)
            put(COLUMN_POSITION, task.position)
            put(COLUMN_NOTES, task.details?.notes)
            put(COLUMN_FILE_PATH, task.details?.filePath)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }

        return db.update(
            TABLE_TASKS,
            values,
            "$COLUMN_POSITION = ?",
            arrayOf(task.position.toString())
        )
    }

    // Delete Task
    fun deleteTask(id: Int) {
        val db = this.writableDatabase
        db.delete(TABLE_TASKS, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
    }

    val allTasks: List<Task>
        get() {
            val taskList: MutableList<Task> = ArrayList()
            val selectQuery = "SELECT * FROM $TABLE_TASKS ORDER BY $COLUMN_POSITION ASC"
            val db = readableDatabase
            var cursor: Cursor? = null

            try {
                cursor = db.rawQuery(selectQuery, null)

                if (cursor?.moveToFirst() == true) {
                    // Required columns
                    val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
                    val positionIndex = cursor.getColumnIndexOrThrow(COLUMN_POSITION)

                    // Optional columns
                    val titleIndex = cursor.getColumnIndex(COLUMN_TITLE)
                    val isDoneIndex = cursor.getColumnIndex(COLUMN_IS_DONE)
                    val notesIndex = cursor.getColumnIndex(COLUMN_NOTES)
                    val filePathIndex = cursor.getColumnIndex(COLUMN_FILE_PATH)
                    val timestampIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP)

                    do {
                        val task = Task(
                            id = cursor.getInt(idIndex),
                            title = if (titleIndex >= 0) cursor.getString(titleIndex) else null,
                            isCompleted = if (isDoneIndex >= 0) cursor.getInt(isDoneIndex) == 1 else false,
                            details = TaskDetails(
                                notes = if (notesIndex >= 0) cursor.getString(notesIndex) else null,
                                filePath = if (filePathIndex >= 0) cursor.getString(filePathIndex) else null
                            ),
                            timestamp = if (timestampIndex >= 0) cursor.getLong(timestampIndex) else System.currentTimeMillis()
                        ).apply {
                            this.position = cursor.getInt(positionIndex)
                        }
                        taskList.add(task)
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
                db.close()
            }

            return taskList
        }


    // Clear All Tasks
    fun clearAllTasks() {
        val db = this.writableDatabase
        db.delete(TABLE_TASKS, null, null)
        db.close()
    }

    fun getTaskByPosition(position: Int): Task? {
        val db = readableDatabase
        var cursor: Cursor? = null
        var task: Task? = null

        try {
            cursor = db.query(
                TABLE_TASKS,
                null,
                "$COLUMN_POSITION = ?",
                arrayOf(position.toString()),
                null,
                null,
                null
            )

            if (cursor?.moveToFirst() == true) {
                // Required columns - use getColumnIndexOrThrow
                val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
                val positionIndex = cursor.getColumnIndexOrThrow(COLUMN_POSITION)

                // Optional columns - use safe handling
                val titleIndex = cursor.getColumnIndex(COLUMN_TITLE)
                val isDoneIndex = cursor.getColumnIndex(COLUMN_IS_DONE)
                val notesIndex = cursor.getColumnIndex(COLUMN_NOTES)
                val filePathIndex = cursor.getColumnIndex(COLUMN_FILE_PATH)
                val timestampIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP)

                task = Task(
                    id = cursor.getInt(idIndex),
                    title = if (titleIndex >= 0) cursor.getString(titleIndex) else null,
                    isCompleted = if (isDoneIndex >= 0) cursor.getInt(isDoneIndex) == 1 else false,
                    details = TaskDetails(
                        notes = if (notesIndex >= 0) cursor.getString(notesIndex) else null,
                        filePath = if (filePathIndex >= 0) cursor.getString(filePathIndex) else null
                    ),
                    timestamp = if (timestampIndex >= 0) cursor.getLong(timestampIndex) else System.currentTimeMillis()
                ).apply {
                    this.position = cursor.getInt(positionIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            cursor?.close()
            db.close()
        }

        return task
    }

    companion object {
        // Database Name and Version
        private const val DATABASE_NAME = "ToDoDB"
        private const val DATABASE_VERSION = 2

        // Table Name and Columns
        private const val TABLE_TASKS = "tasks"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_IS_DONE = "isDone"
        private const val COLUMN_COMPLETED = "completed"
        private const val COLUMN_FILE_PATH = "filePath"
        private const val COLUMN_NOTES = "notes"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_POSITION = "position"

        // Create Table Query
        private val TABLE_CREATE = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, 
                $COLUMN_TITLE TEXT, 
                $COLUMN_CONTENT TEXT, 
                $COLUMN_IS_DONE INTEGER, 
                $COLUMN_FILE_PATH TEXT,
                $COLUMN_NOTES TEXT, 
                $COLUMN_TIMESTAMP INTEGER DEFAULT ${System.currentTimeMillis()}, 
                $COLUMN_POSITION INTEGER
            );
        """
    }
}
