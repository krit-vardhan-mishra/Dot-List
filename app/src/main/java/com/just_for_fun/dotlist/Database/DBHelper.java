package com.just_for_fun.dotlist.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.just_for_fun.dotlist.Task.Task;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_TASKS = "tasks";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CHECKED = "checked";
    private static final String COLUMN_NOTES = "notes";
    private static final String COLUMN_FILE_URI = "file_uri";

    private static final String COLUMN_CREATED_TIME = "created_time";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_TASKS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_CHECKED + " INTEGER DEFAULT 0, " +
                COLUMN_NOTES + " TEXT, " +
                COLUMN_FILE_URI + " TEXT, " +
                COLUMN_CREATED_TIME + " INTEGER DEFAULT (strftime('%s','now')))";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        onCreate(db);
    }

    public long addTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, task.getTitle());
        values.put(COLUMN_CHECKED, task.isChecked() ? 1 : 0);
        values.put(COLUMN_NOTES, task.getNotes());
        values.put(COLUMN_FILE_URI, task.getFileUri() != null ? task.getFileUri().toString() : null);

        long id = db.insert(TABLE_TASKS, null, values);
        task.setId(id);
        db.close();
        return id;
    }

    public List<Task> getAllTasks() {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query without COLUMN_DELETION_TIME
        Cursor cursor = db.query(
                TABLE_TASKS, // Table name
                null,        // Columns (null means all columns)
                null,        // Selection (no condition)
                null,        // Selection arguments
                null,        // Group by
                null,        // Having
                COLUMN_CREATED_TIME + " DESC" // Order by
        );

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task();

                // Safely get column indices
                int idIndex = cursor.getColumnIndex(COLUMN_ID);
                int titleIndex = cursor.getColumnIndex(COLUMN_TITLE);
                int checkedIndex = cursor.getColumnIndex(COLUMN_CHECKED);
                int notesIndex = cursor.getColumnIndex(COLUMN_NOTES);
                int fileUriIndex = cursor.getColumnIndex(COLUMN_FILE_URI);

                // Set task properties only if the column exists
                if (idIndex != -1) {
                    task.setId(cursor.getLong(idIndex));
                }
                if (titleIndex != -1) {
                    task.setTitle(cursor.getString(titleIndex));
                }
                if (checkedIndex != -1) {
                    task.setChecked(cursor.getInt(checkedIndex) == 1);
                }
                if (notesIndex != -1) {
                    task.setNotes(cursor.getString(notesIndex));
                }
                if (fileUriIndex != -1) {
                    String uriString = cursor.getString(fileUriIndex);
                    task.setFileUri(uriString != null ? Uri.parse(uriString) : null);
                }

                taskList.add(task);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return taskList;
    }

    public int updateTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, task.getTitle());
        values.put(COLUMN_CHECKED, task.isChecked() ? 1 : 0);
        values.put(COLUMN_NOTES, task.getNotes());
        values.put(COLUMN_FILE_URI, task.getFileUri() != null ? task.getFileUri().toString() : null);

        return db.update(TABLE_TASKS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(task.getId())});
    }

    public void deleteTaskPermanently(long taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COLUMN_ID + " = ?", new String[]{String.valueOf(taskId)});
        db.close();
    }

}