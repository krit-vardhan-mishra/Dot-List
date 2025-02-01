package com.just_for_fun.dotlist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.just_for_fun.dotlist.Database.DBHelper;
import com.just_for_fun.dotlist.Database.DatabaseExecutor;
import com.just_for_fun.dotlist.Task.Task;
import com.just_for_fun.dotlist.Task.TaskAdapter;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnFileUploadListener, TaskAdapter.OnTaskDeleteListener {

    // Constants
    private static final int PICK_FILE_REQUEST = 101;

    // UI Components
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private FloatingActionButton fabAddTask;

    // Data
    private DBHelper dbHelper;
    private SparseArray<Integer> requestCodeMap = new SparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Database Helper
        dbHelper = new DBHelper(this);

        // Initialize UI Components
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup Button Listeners
        setupButtonListeners();
    }

    // Initialize UI Components
    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view);
        fabAddTask = findViewById(R.id.addTask);
    }

    // Setup RecyclerView with Adapter and LayoutManager
    private void setupRecyclerView() {
        DatabaseExecutor.getInstance().execute(() -> {
            List<Task> initialTasks = dbHelper.getAllTasks(); // Effectively final
            if (initialTasks.isEmpty()) {
                // Add default tasks in the background
                for (int i = 0; i < 3; i++) {
                    Task task = new Task();
                    dbHelper.addTask(task);
                }
                // Reload tasks into a new variable
                List<Task> reloadedTasks = dbHelper.getAllTasks();
                runOnUiThread(() -> setupAdapter(reloadedTasks));
            } else {
                runOnUiThread(() -> setupAdapter(initialTasks));
            }
        });
    }

    // Helper method to initialize the adapter
    private void setupAdapter(List<Task> tasks) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(tasks, this, this, dbHelper, this);
        recyclerView.setAdapter(taskAdapter);
    }

    // Setup Button Listeners
    private void setupButtonListeners() {
        // Add Task Button
        fabAddTask.setOnClickListener(v -> taskAdapter.addNewTask());
    }

    // Handle File Upload Request
    @Override
    public void onFileUpload(int position) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST + position);
    }

    // Handle Activity Result for File Upload
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            int position = requestCode - PICK_FILE_REQUEST;
            if (position >= 0 && position < taskAdapter.getTasks().size()) {
                Uri uri = data.getData();
                Task task = taskAdapter.getTasks().get(position);
                task.setFileUri(uri);
                DatabaseExecutor.getInstance().execute(() -> {
                    dbHelper.updateTask(task);
                    runOnUiThread(() -> taskAdapter.notifyItemChanged(position));
                });
            }
        }
    }

    // Show File Preview
    public void showFilePreview(Uri fileUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, getContentResolver().getType(fileUri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            showError("Cannot open file: " + e.getMessage());
        }
    }

    // Show Error Message
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Handle Task Deletion
    @Override
    public void onTaskDelete(int position) {
        Task task = taskAdapter.getTasks().get(position);
        DatabaseExecutor.getInstance().execute(() -> {
            dbHelper.deleteTaskPermanently(task.getId());
            runOnUiThread(() -> taskAdapter.removeTask(position));
        });
    }
}