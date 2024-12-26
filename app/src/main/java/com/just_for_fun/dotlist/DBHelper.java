package com.just_for_fun.dotlist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

// Database Helper Class
public class DBHelper extends SQLiteOpenHelper {

    // Database Name and Version
    private static final String DATABASE_NAME = "ToDoDB";
    private static final int DATABASE_VERSION = 1;

    // Table Name and Columns
    private static final String TABLE_TASKS = "tasks";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_IS_DONE = "isDone";

    // Create Table Query
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_TASKS + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_TITLE + " TEXT, "
                    + COLUMN_IS_DONE + " INTEGER,"
                    + "filePath TEXT);";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) { // Add 'filePath' column in version 2
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN filePath TEXT");
        }
        if (oldVersion < 3) { // Future updates should follow similar checks
            // Example: Adding a new column for timestamps
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN timestamp DATETIME DEFAULT CURRENT_TIMESTAMP");
        }
    }

    // Insert Task
    public void insertTask(String title, boolean isDone, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_IS_DONE, isDone ? 1 : 0);
        values.put("filePath", filePath);
        db.insert(TABLE_TASKS, null, values);
        db.close();
    }

    // Update Task
    public void updateTask(int id, boolean isDone, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DONE, isDone ? 1 : 0);
        if (filePath != null) {
            values.put("filePath", filePath);
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
                taskList.add(new Task(id, title, isDone, filePath));
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