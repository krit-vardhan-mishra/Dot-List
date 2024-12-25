package com.just_for_fun.dotlist;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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

    private void handleFileAttachment(Uri uri) {
        int taskIndex = getTaskIndexFromView(lastClickedView);
        Task task = getTaskAtIndex(taskIndex);

        if (task == null) return;

        task.getDetails().setFilePath(uri.toString());

        View row = taskContainer.getChildAt(taskIndex);
        ImageView uploadButton = row.findViewById(R.id.uploadButton);
        TextView taskDetailsTextView = row.findViewById(R.id.taskDetailsTextView);
        uploadButton.setImageResource(R.drawable.ic_check);
        taskDetailsTextView.setVisibility(View.VISIBLE);
        taskDetailsTextView.setText("Attached: " + uri.getLastPathSegment());

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

    private void addTaskToUI(Task task) {
        View row = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);
        EditText editText = row.findViewById(R.id.taskEditText);
        CheckBox checkBox = row.findViewById(R.id.taskCheckbox);
        ImageView uploadButton = row.findViewById(R.id.uploadButton);

        editText.setText(task.getTitle());
        checkBox.setChecked(task.isCompleted());

        row.setOnClickListener(v -> lastClickedView = v);

        checkBox.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            dbHelper.updateTask(task.getId(), isChecked, task.getDetails().getFilePath());
        }));

        taskContainer.addView(row);
    }

    private void addTextWatchersToTasks() {
        for (int i = 0; i < taskContainer.getChildCount(); i++) {
            View row = taskContainer.getChildAt(i);
            EditText editText = row.findViewById(R.id.taskEditText);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    checkAndAddRows();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

        }
    }

    private void checkAndAddRows() {
        int filledBoxes = 0;
        for (int i = 0; i < taskContainer.getChildCount(); i++) {
            View row = taskContainer.getChildAt(i);
            EditText editText = row.findViewById(R.id.taskEditText);
            if (!editText.getText().toString().isEmpty()) {
                filledBoxes++;
            }
        }

        double fillPercentage = (double) filledBoxes / currentCapacity * 100;
        if (fillPercentage >= 80) {
            // Double the capacity
            int newCapacity = currentCapacity * 2;
            for (int i = 0; i < currentCapacity - taskContainer.getChildCount(); i++) {
                View row = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);
                addTextWatchersToTasks();
                taskContainer.addView(row);
            }
            addTextWatchersToTasks();
        }
    }

    private void addNewTaskRow() {
        for (Task task : tasks) { // Prevent duplicates
            if (task.getTitle().isEmpty()) return;
        }

        View row = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);
        EditText editText = row.findViewById(R.id.taskEditText);
        CheckBox checkBox = row.findViewById(R.id.taskCheckbox);

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Task newTask = new Task(editText.getText().toString(), isChecked);
            dbHelper.insertTask(newTask.getTitle(), isChecked, null);
            tasks.add(newTask);
        });

        taskContainer.addView(row);
    }

    // Handle file attachment
    public void onFileAttached(View view) {
        lastClickedView = view;
        pickPdfLauncher.launch("application/pdf"); // Launch PDF picker
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
