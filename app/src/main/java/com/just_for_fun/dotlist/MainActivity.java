package com.just_for_fun.dotlist;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout taskContainer;
    private int currentCapacity = 5;
    private final List<Task> tasks = new ArrayList<>();
    private DBHelper dbHelper;
    private ActivityResultLauncher<String> pickPdfLauncher;
    private View lastClickedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        taskContainer = findViewById(R.id.taskContainer);

        // Register the PDF picker launcher
        pickPdfLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleFileAttachment(uri);
                    }
                }
        );

        // Load Tasks for Database
        loadTasksFromDatabase();

        // Add initial input boxes
        for (int i = 0; i < currentCapacity; i++) {
            taskContainer.addView(getLayoutInflater().inflate(R.layout.task_row, taskContainer, false));
        }

        // Add text changed listener to each EditText
        addTextWatchersToTasks();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
        taskContainer.removeAllViews();
    }

    @SuppressLint("SetTextI18n")
    private void handleFileAttachment(Uri uri) {
        int taskIndex = getTaskIndexFromView(lastClickedView);
        Task task = getTaskAtIndex(taskIndex);

        if (task == null) return;
        task.getDetails().setFilePath(uri.toString());

        View row = taskContainer.getChildAt(taskIndex);
        ImageView uploadButton = row.findViewById(R.id.uploadButton);
        TextView taskDetailsTextView = row.findViewById(R.id.taskDetailsTextView);
        ImageView previewButton = row.findViewById(R.id.previewButton);

        uploadButton.setImageResource(R.drawable.ic_check);
        taskDetailsTextView.setVisibility(View.VISIBLE);
        taskDetailsTextView.setText("Attached: " + uri.getLastPathSegment());

        previewButton.setOnClickListener(v -> openPdfPreview(uri));

        dbHelper.updateTask(task.getId(), task.isCompleted(), task.getDetails().getFilePath());
    }

    private void loadTasksFromDatabase() {
        tasks.clear();
        List<Task> savedTasks = dbHelper.getAllTasks();
        for (Task task : savedTasks) {
            addTaskToUI(task);
            tasks.add(task);
        }
    }

    @SuppressLint("SetTextI18n")
    private void addTaskToUI(Task task) {
        View row = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);
        EditText editText = row.findViewById(R.id.taskEditText);
        CheckBox checkBox = row.findViewById(R.id.taskCheckbox);
        ImageView downArrow = row.findViewById(R.id.downArrow);
        ImageView previewIcon = row.findViewById(R.id.previewIcon);
        ImageButton uploadButton = row.findViewById(R.id.uploadButton);
        LinearLayout notesLayout = row.findViewById(R.id.notesLayout);

        editText.setText("Enter Task");
        checkBox.setChecked(task.isCompleted());

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    downArrow.setVisibility(View.VISIBLE);
                } else {
                    downArrow.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        row.setOnClickListener(v -> lastClickedView = v);

        downArrow.setOnClickListener(v -> {
            notesLayout.setVisibility(notesLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        uploadButton.setOnClickListener(v -> {
            lastClickedView = row;
            pickPdfLauncher.launch("application/pdf");
        });

        previewIcon.setOnClickListener(v -> openPDFWithSystemViewer(task.getFilePath()));

        checkBox.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            dbHelper.updateTask(task.getId(), isChecked, task.getDetails().getFilePath());
        }));

        editText.setOnClickListener(v -> {
            if (!editText.hasFocus() && editText.getText().toString().trim().isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Task")
                        .setMessage("Do you want to delete this task?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            taskContainer.removeView(row);
                            dbHelper.deleteTask(task.getId());
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        taskContainer.addView(row);
    }

    private void openPDFWithSystemViewer(String filePath) {
        File file = new File(filePath);

        if (file.exists()) {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No PDF Viewer Found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPdfPreview(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No PDF viewer installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void addTextWatchersToTasks() {
        for (int i = 0; i < taskContainer.getChildCount(); i++) {
            View row = taskContainer.getChildAt(i);
            EditText editText = row.findViewById(R.id.taskEditText);
            final int position = i;

            editText.addTextChangedListener(new TextWatcher() {
                private boolean isEditing = false;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String text = s.toString();

                    if (!text.isEmpty()) {
                        Task task = getTaskAtIndex(position);

                        if (task == null) {
                            task = new Task(-1, text, false, new TaskDetails());
                            tasks.add(task);
                            dbHelper.insertTask(text, false, null);
                        }
                    }
                    checkAddRows();
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isEditing) return;

                    String input = s.toString().trim(); // Removes extra spaces or symbols
                    if (input.isEmpty()) {
                        isEditing = true;
                        editText.removeTextChangedListener(this);
                        editText.setText("");
                        editText.setSelection(0);
                        editText.addTextChangedListener(this);
                        isEditing = false;

                        Task task = getTaskAtIndex(position);
                        if (task != null) {
                            tasks.remove(task);
                            dbHelper.deleteTask(task.getId());
                        }
                    }
                }
            });

        }
    }

    private void checkAddRows() {
        try {
            int filledBoxes = 0;
            int totalChildren = taskContainer.getChildCount();

            for (int i = 0; i < taskContainer.getChildCount(); i++) {
                View row = taskContainer.getChildAt(i);
                EditText editText = row.findViewById(R.id.taskEditText);

                if (!editText.getText().toString().isEmpty()) {
                    filledBoxes++;
                }
            }

            double fillPercentage = totalChildren > 0 ?
                    (double) filledBoxes / currentCapacity * 100 : 0;

            if (fillPercentage >= 80) {
                // Double the capacity
                int newCapacity = currentCapacity * 2;
                int rowsToAdd = newCapacity - totalChildren;

                for (int i = 0; i < rowsToAdd; i++) {
                    View row = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);
                    taskContainer.addView(row);
                }
                currentCapacity = newCapacity;
                addTextWatchersToTasks();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error in checkAndAddRows: " + e.getMessage());
        }

    }

    private int getTaskIndexFromView(View view) {
        LinearLayout parentRow = (LinearLayout) view.getParent();
        return taskContainer.indexOfChild(parentRow);
    }

    private Task getTaskAtIndex(int index) {
        if (index >= 0 && index < tasks.size()) {
            return tasks.get(index);
        }
        return null;
    }
}
