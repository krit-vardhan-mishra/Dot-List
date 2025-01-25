package com.just_for_fun.dotlist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.just_for_fun.dotlist.Bin.BinActivity;
import com.just_for_fun.dotlist.Database.DBHelper;
import com.just_for_fun.dotlist.Task.Task;
import com.just_for_fun.dotlist.Task.TaskAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnFileUploadListener, TaskAdapter.OnTaskDeleteListener {
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private static final int PICK_FILE_REQUEST = 101;
    private SparseArray<Integer> requestCodeMap = new SparseArray<>();
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);

        // Initialize RecyclerView
        List<Task> initialTasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            initialTasks.add(new Task());
        }

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(initialTasks, this, this, dbHelper);
        recyclerView.setAdapter(taskAdapter);

        // Setup FAB
        FloatingActionButton fab = findViewById(R.id.addTask);
        fab.setOnClickListener(v -> taskAdapter.addNewTask());

        // Add bin button click
        ImageButton binButton = findViewById(R.id.binImage);
        binButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BinActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onFileUpload(int position) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST + position);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            int position = requestCode - PICK_FILE_REQUEST;
            if (position >= 0 && position < taskAdapter.getTasks().size()) {
                Uri uri = data.getData();
                Task task = taskAdapter.getTasks().get(position);
                task.setFileUri(uri);
                dbHelper.updateTask(task);
                taskAdapter.notifyItemChanged(position);
            }
        }
    }

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

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskDelete(int position) {
        Task task = taskAdapter.getTasks().get(position);
        task.setDeletionTime(System.currentTimeMillis());
        dbHelper.updateTask(task);
        taskAdapter.removeTask(position);
    }
}

/*
@Override
public void onFileUpload(int position) {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType("*//*");
    intent.addCategory(Intent.CATEGORY_OPENABLE);

    int requestCode = PICK_FILE_REQUEST + position;
    requestCodeMap.put(requestCode, position);
    startActivityForResult(intent, requestCode);
}

@Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Integer position = requestCodeMap.get(requestCode);
            if (position != null) {
                Uri uri = data.getData();
                Task task = taskAdapter.getTasks().get(position);
                task.setFileUri(uri);  // Update task state
                taskAdapter.notifyItemChanged(position);
            }
        }
    }
*/