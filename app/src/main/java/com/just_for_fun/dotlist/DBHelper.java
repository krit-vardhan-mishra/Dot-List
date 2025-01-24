//package com.just_for_fun.dotlist;
//
//import android.content.ContentValues;
//import android.content.Context;
//import android.database.Cursor;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteOpenHelper;
//import android.util.Log;
//
//import java.util.ArrayList;
//import java.util.List;
//
//// Database Helper Class
//public class DBHelper extends SQLiteOpenHelper {
//
//    // Database Name and Version
//    private static final String DATABASE_NAME = "ToDoDB";
//    private static final int DATABASE_VERSION = 2;
//
//    private static final String COLUMN_NOTES = "notes";
//    private static final String COLUMN_TIMESTAMP = "timestamp";
//    private static final String COLUMN_POSITION = "position";
//
//
//    // Table Name and Columns
//    private static final String TABLE_TASKS = "tasks";
//    private static final String COLUMN_ID = "id";
//    private static final String COLUMN_TITLE = "title";
//    private static final String COLUMN_IS_DONE = "isDone";
//    private static final String COLUMN_FILE_PATH = "filePath";
//
//    private static final String COLUMN_CONTENT = "content";
//    private static final String COLUMN_COMPLETED = "completed";
//
//    // Create Table Query
//    private static final String TABLE_CREATE =
//            "CREATE TABLE " + TABLE_TASKS + " ("
//                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
//                    + COLUMN_TITLE + " TEXT, "
//                    + COLUMN_CONTENT + " TEXT, "  // Added missing column
//                    + COLUMN_IS_DONE + " INTEGER, "
//                    + COLUMN_COMPLETED + " INTEGER, "  // Added missing column
//                    + COLUMN_FILE_PATH + " TEXT, "
//                    + COLUMN_NOTES + " TEXT, "
//                    + COLUMN_TIMESTAMP + " INTEGER DEFAULT " + System.currentTimeMillis() + ", "
//                    + COLUMN_POSITION + " INTEGER);";
//
//    public DBHelper(Context context) {
//        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//        db.execSQL(TABLE_CREATE);
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        if (oldVersion < 2) {
//            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_NOTES + " TEXT");
//            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_TIMESTAMP + " INTEGER DEFAULT " + System.currentTimeMillis());
//            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_POSITION + " INTEGER DEFAULT 0");
//        }
//    }
//
//    // Insert Task
//    public long insertTask(Task task) {
//        SQLiteDatabase db = null;
//        try {
//            db = getWritableDatabase();
//            ContentValues values = new ContentValues();
//
//            // Handle both content-based and title-based tasks
//            if (task.getContent() != null) {
//                values.put(COLUMN_CONTENT, task.getContent());
//            } else {
//                values.put(COLUMN_TITLE, task.getTitle());
//            }
//
//            values.put(COLUMN_IS_DONE, task.isCompleted() ? 1 : 0);
//            values.put(COLUMN_FILE_PATH, task.getFilePath());
//            values.put(COLUMN_POSITION, task.getPosition());
//
//            // Only add notes if details exists
//            if (task.getDetails() != null) {
//                values.put(COLUMN_NOTES, task.getDetails().getNotes());
//            }
//
//            values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
//
//            return db.insert(TABLE_TASKS, null, values);
//        } finally {
//            if (db != null && db.isOpen()) {
//                db.close();
//            }
//        }
//    }
//
//    // Delete Task
//    public void deleteTask(int id) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        db.delete(TABLE_TASKS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
//        db.close();
//    }
//
//    public int updateTaskContent(int position, String title) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues values = new ContentValues();
//
//        values.put(COLUMN_TITLE, title);
//
//        int rowsAffected = db.update(TABLE_TASKS,
//                values,
//                COLUMN_POSITION + " = ?",
//                new String[]{String.valueOf(position)});
//        db.close();
//        return rowsAffected;
//    }
//
//    // Get all tasks
//    public List<Task> getAllTasks() {
//        List<Task> taskList = new ArrayList<>();
//        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " ORDER BY " + COLUMN_POSITION;
//
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = null;
//
//        try {
//            cursor = db.rawQuery(selectQuery, null);
//
//            if (cursor != null && cursor.moveToFirst()) {
//                int idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID);
//                int titleIndex = cursor.getColumnIndex(COLUMN_TITLE);  // Use getColumnIndex instead
//                int contentIndex = cursor.getColumnIndex(COLUMN_CONTENT);  // Add content index
//                int isDoneIndex = cursor.getColumnIndex(COLUMN_IS_DONE);
//                int filePathIndex = cursor.getColumnIndex(COLUMN_FILE_PATH);
//                int notesIndex = cursor.getColumnIndex(COLUMN_NOTES);
//                int positionIndex = cursor.getColumnIndex(COLUMN_POSITION);
//                int timestampIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP);
//
//                do {
//                    Task task = new Task();
//                    task.setId(cursor.getInt(idIndex));
//
//                    // Handle both content and title based tasks
//                    if (contentIndex != -1 && !cursor.isNull(contentIndex)) {
//                        task.setContent(cursor.getString(contentIndex));
//                    } else if (titleIndex != -1 && !cursor.isNull(titleIndex)) {
//                        task.setTitle(cursor.getString(titleIndex));
//                    }
//
//                    if (isDoneIndex != -1) {
//                        task.setCompleted(cursor.getInt(isDoneIndex) == 1);
//                    }
//
//                    if (filePathIndex != -1) {
//                        task.setFilePath(cursor.getString(filePathIndex));
//                    }
//
//                    if (positionIndex != -1) {
//                        task.setPosition(cursor.getInt(positionIndex));
//                    }
//
//                    TaskDetails details = new TaskDetails();
//                    if (notesIndex != -1) {
//                        details.setNotes(cursor.getString(notesIndex));
//                    }
//                    task.setDetails(details);
//
//                    if (timestampIndex != -1) {
//                        task.setTimestamp(cursor.getLong(timestampIndex));
//                    }
//
//                    taskList.add(task);
//                } while (cursor.moveToNext());
//            }
//        } catch (Exception e) {
//            Log.e("DBHelper", "Error reading tasks: " + e.getMessage());
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//            db.close();
//        }
//
//        return taskList;
//    }
//
//}
