package com.just_for_fun.dotlist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CheckBox checkBox;
    private EditText notesArea;
    private ImageButton upArrow;
    private ImageButton downArrow;
    private EditText taskEditText;
    private ImageButton uploadButton;
    private ImageButton previewButton;
    private ConstraintLayout notesLayout;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    private LinearLayout taskContainer;
    private int currentCapacity = 5;
    private final List<Task> tasks = new ArrayList<>();
    private final List<Boolean> containerStates = new ArrayList<>();
    private DBHelper dbHelper;
    private Uri selectedFileUri;

    // Dynamic container management
    private final int MIN_CONTAINERS = 5;
    private final int ADD_THRESHOLD = 2;
    private final int REMOVE_THRESHOLD = 2;
    private View lastClickedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        upArrow = findViewById(R.id.up_arrow);
        notesArea = findViewById(R.id.notesArea);
        downArrow = findViewById(R.id.down_arrow);
        checkBox = findViewById(R.id.taskCheckbox);
        notesLayout = findViewById(R.id.notesLayout);
        taskEditText = findViewById(R.id.taskEditText);
        uploadButton = findViewById(R.id.uploadButton);
        previewButton = findViewById(R.id.previewButton);
        taskContainer = findViewById(R.id.taskContainer);

        // Initially hide the notes layout
        notesLayout.setVisibility(View.GONE);
        upArrow.setVisibility(View.GONE);

        dbHelper = new DBHelper(this);
        taskContainer = findViewById(R.id.taskContainer);

        // Add initial input boxes
        for (int i = 0; i < currentCapacity; i++) {
            addNewTaskContainer();
        }

        // Starting the monitoring thread
        startContainerMonitor();

        downArrow.setOnClickListener(v -> {
                notesLayout.setVisibility(View.VISIBLE);
                downArrow.setVisibility(View.GONE);
                upArrow.setVisibility(View.VISIBLE);

            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) taskEditText.getLayoutParams();
            layoutParams.endToStart = upArrow.getId();
            layoutParams.startToEnd = checkBox.getId();
            taskEditText.setLayoutParams(layoutParams);
        });

        upArrow.setOnClickListener(v -> {
                notesLayout.setVisibility(View.GONE);
                downArrow.setVisibility(View.VISIBLE);
                upArrow.setVisibility(View.GONE);

            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) taskEditText.getLayoutParams();
            layoutParams.endToStart = downArrow.getId();
            layoutParams.startToEnd = checkBox.getId();
            taskEditText.setLayoutParams(layoutParams);
        });

        uploadButton.setOnClickListener(v -> openFilePicker());

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            selectedFileUri = data.getData();
                            if (selectedFileUri != null) {
                                previewButton.setVisibility(View.VISIBLE);
                                previewButton.setImageURI(selectedFileUri);
                            }
                        }
                    }
                }
        );

    }

    private void startContainerMonitor() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);

                    runOnUiThread(() -> {
                    int emptyCount = 0;
                    int totalCount = containerStates.size();

                    for (boolean state : containerStates) {
                        if (!state) emptyCount++;
                    }

                    if (emptyCount <= ADD_THRESHOLD) {
                        addNewTaskContainer();
                        addNewTaskContainer();
                    }

                    if (totalCount > MIN_CONTAINERS && emptyCount > REMOVE_THRESHOLD) {
                        for (int i = containerStates.size()-1; i >= 0; i--) {
                            if (!containerStates.get(i)) {
                                removeTaskContainer(i);
                                emptyCount--;
                                if (emptyCount <= REMOVE_THRESHOLD) break;
                            }
                        }
                    }
                });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void addNewTaskContainer() {
        View container = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);
        EditText taskEditText = container.findViewById(R.id.taskEditText);

        taskEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int index = taskContainer.indexOfChild(container);
                updateTaskState(index, !s.toString().trim().isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        taskContainer.addView(container);
        containerStates.add(false);
    }

    private void removeTaskContainer(int index) {
        taskContainer.removeViewAt(index);
        containerStates.remove(index);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private void updateTaskState(int index, boolean isFilled) {
        containerStates.set(index, isFilled);
    }
}

/*
package com.just_for_fun.dotlist;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CheckBox checkBox;
    private EditText notesArea;
    private ImageButton upArrow;
    private ImageButton downArrow;
    private EditText taskEditText;
    private ImageButton uploadButton;
    private ImageButton previewButton;
    private ConstraintLayout notesLayout;

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

    private void addTaskToUI(Task task) {
        View row = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);
        EditText editText = row.findViewById(R.id.taskEditText);
        CheckBox checkBox = row.findViewById(R.id.taskCheckbox);
        ImageView downArrow = row.findViewById(R.id.downArrow);
        ImageView previewIcon = row.findViewById(R.id.previewIcon);

        editText.setText(task.getTitle());
        checkBox.setChecked(task.isCompleted());

        row.setOnClickListener(v -> lastClickedView = v);

        downArrow.setOnClickListener(v -> {
            TextView taskDetailsTextView = row.findViewById(R.id.taskDetailsTextView);
            if (taskDetailsTextView.getVisibility() == View.VISIBLE) {
                taskDetailsTextView.setVisibility(View.GONE);
            } else {
                taskDetailsTextView.setVisibility(View.VISIBLE);
            }
        });

        if (task.getFilePath() != null && !task.getFilePath().isEmpty()) {
            previewIcon.setVisibility(View.VISIBLE);
            previewIcon.setOnClickListener(v -> openPDFWithSystemViewer(task.getFilePath()));
        } else {
            previewIcon.setVisibility(View.GONE);
        }

        checkBox.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            dbHelper.updateTask(task.getId(), isChecked, task.getDetails().getFilePath());
        }));

        taskContainer.addView(row);
    }

    private void openPDFWithSystemViewer(String filePath) {
        File file = new File(filePath);

        if (file.exists()) {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    new File(filePath)
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
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String text = s.toString();

                    if (!text.isEmpty()) {
                        Task task = getTaskAtIndex(position);

                        if (task == null) {
                            task = new Task(-1, text, false, (String) null);
                            tasks.add(task);
                            dbHelper.insertTask(text, false, null);
                        }
                    }
                    checkAndAddRows();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

        }
    }

    private void checkAndAddRows() {
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

*/