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
import java.util.concurrent.TimeUnit;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_TASKS = "tasks";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CHECKED = "checked";
    private static final String COLUMN_NOTES = "notes";
    private static final String COLUMN_FILE_URI = "file_uri";
    private static final String COLUMN_DELETION_TIME = "deletion_time";

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
                COLUMN_DELETION_TIME + " INTEGER DEFAULT -1)";
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
        values.put(COLUMN_DELETION_TIME, task.getDeletionTime());

        long id = db.insert(TABLE_TASKS, null, values);
        task.setId(id);
        db.close();
        return id;
    }

    public List<Task> getAllTasks() {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASKS, null, COLUMN_DELETION_TIME + " = ?", new String[]{"-1"}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                task.setChecked(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHECKED)) == 1);
                task.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)));
                String uriString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_URI));
                task.setFileUri(uriString != null ? Uri.parse(uriString) : null);
                task.setDeletionTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DELETION_TIME)));
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
        values.put(COLUMN_DELETION_TIME, task.getDeletionTime());

        return db.update(TABLE_TASKS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(task.getId())});
    }

    public void deleteTaskPermanently(long taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COLUMN_ID + " = ?", new String[]{String.valueOf(taskId)});
        db.close();
    }

    public List<Task> getDeletedTasks() {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        long thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        String selection = COLUMN_DELETION_TIME + " > ? AND " + COLUMN_DELETION_TIME + " != ?";
        String[] selectionArgs = {String.valueOf(thirtyDaysAgo), "-1"};

        Cursor cursor = db.query(TABLE_TASKS, null, selection, selectionArgs, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                task.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)));
                task.setDeletionTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DELETION_TIME)));
                taskList.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return taskList;
    }
}