package com.just_for_fun.dotlist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

// Database Helper Class
public class DBHelper extends SQLiteOpenHelper {

    // Database Name and Version
    private static final String DATABASE_NAME = "ToDoDB";
    private static final int DATABASE_VERSION = 2;

    private static final String COLUMN_NOTES = "notes";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_POSITION = "position";

    // Table Name and Columns
    private static final String TABLE_TASKS = "tasks";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_IS_DONE = "isDone";
    private static final String COLUMN_FILE_PATH = "filePath";

    // Create Table Query
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_TASKS + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_TITLE + " TEXT, "
                    + COLUMN_IS_DONE + " INTEGER, "
                    + COLUMN_FILE_PATH + " TEXT, "
                    + COLUMN_NOTES + " TEXT, "
                    + COLUMN_TIMESTAMP + " INTEGER, "
                    + COLUMN_POSITION + " INTEGER);";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_NOTES + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_TIMESTAMP + " INTEGER DEFAULT " + System.currentTimeMillis());
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_POSITION + " INTEGER DEFAULT 0");
        }
    }

    // Insert Task
    public long insertTask(Task task) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_TITLE, task.getTitle());
            values.put(COLUMN_IS_DONE, task.isCompleted() ? 1 : 0);
            values.put(COLUMN_FILE_PATH, task.getFilePath());
            values.put(COLUMN_NOTES, task.getDetails().getNotes());
            values.put(COLUMN_TIMESTAMP, task.getTimestamp());
            values.put(COLUMN_POSITION, task.getPosition());
            return db.insert(TABLE_TASKS, null, values);
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    // Update Task
    public void updateTask(int id, boolean isDone, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DONE, isDone ? 1 : 0);
        if (filePath != null) {
            values.put(COLUMN_FILE_PATH, filePath);
        }
        db.update(TABLE_TASKS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Delete Task
    public void deleteTask(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Get All Tasks
    public List<Task> getAllTasks() {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TASKS, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String title = cursor.getString(1);
                boolean isDone = cursor.getInt(2) == 1;
                String filePath = cursor.getString(3);
                TaskDetails details = new TaskDetails(null, filePath);
                taskList.add(new Task(id, title, isDone, details));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return taskList;
    }

    // Clear All Tasks
    public void clearAllTasks() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, null, null);
        db.close();
    }
}
