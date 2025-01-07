package com.just_for_fun.dotlist;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String KEY_TASK_STATES = "task_states";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_FILE_URI = "file_uri";
    private static final String STATE_SCROLL_POSITION = "scroll_position";
    private static final int REQUEST_PERMISSIONS = 1001;

    private CheckBox checkBox;
    private EditText notesArea;
    private ImageButton upArrow;
    private ImageButton downArrow;
    private EditText taskEditText;
    private ImageButton uploadButton;
    private ImageButton previewButton;
    private ConstraintLayout notesLayout;
    private LinearLayout taskContainer;
    private ScrollView scrollView;

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private DBHelper dbHelper;
    private Uri selectedFileUri;
    private final List<Boolean> containerStates = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();
    private final Handler containerMonitorHandler = new Handler(Looper.getMainLooper());
    private boolean isMonitoringActive = true;

    private final int MIN_CONTAINERS = 5;
    private final int ADD_THRESHOLD = 2;
    private final int REMOVE_THRESHOLD = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeDatabase();       // it's all starts from here.
        setupInitialState(savedInstanceState);
        setupClickListeners();
        setupFilePicker();
        startContainerMonitor();
    }

    private void initializeViews() {
        upArrow = findViewById(R.id.up_arrow);
        notesArea = findViewById(R.id.notesArea);
        downArrow = findViewById(R.id.down_arrow);
        checkBox = findViewById(R.id.taskCheckbox);
        notesLayout = findViewById(R.id.notesLayout);
        taskEditText = findViewById(R.id.taskEditText);
        uploadButton = findViewById(R.id.uploadButton);
        previewButton = findViewById(R.id.previewButton);
        taskContainer = findViewById(R.id.taskContainer);
        scrollView = findViewById(R.id.scroll_view);
    }

    private void initializeDatabase() {
        dbHelper = new DBHelper(this);
        loadTasksFromDatabase();            // second error
    }

    private void setupInitialState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            notesLayout.setVisibility(View.GONE);
            upArrow.setVisibility(View.GONE);
            previewButton.setVisibility(View.GONE);

            for (int i = 0; i < MIN_CONTAINERS; i++) {
                addNewTaskContainer();
            }
        }
    }

    private void setupClickListeners() {
        downArrow.setOnClickListener(v -> toggleNotesVisibility(true));
        upArrow.setOnClickListener(v -> toggleNotesVisibility(false));
        uploadButton.setOnClickListener(v -> openFilePicker());
        previewButton.setOnClickListener(v -> showFilePreview());
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleFileSelection(result.getData().getData());
                    }
                }
        );
    }

    private void handleFileSelection(Uri uri) {
        if (uri != null) {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            selectedFileUri = uri;
            updateFilePreview();
        }
    }

    private void updateFilePreview() {
        uploadButton.setVisibility(View.GONE);
        previewButton.setVisibility(View.VISIBLE);

        try {
            Glide.with(this)
                    .load(selectedFileUri)
                    .error(R.drawable.ic_file_placeholder)
                    .into(previewButton);
        } catch (Exception e) {
            previewButton.setImageResource(R.drawable.ic_file_placeholder);
        }
    }

    private void showFilePreview() {
        if (selectedFileUri != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(selectedFileUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } catch (Exception e) {
                showError("Cannot open file: " + e.getMessage());
            }
        }
    }

    private void toggleNotesVisibility(boolean show) {
        TransitionManager.beginDelayedTransition(scrollView);

        notesLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        downArrow.setVisibility(show ? View.GONE : View.VISIBLE);
        upArrow.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateTaskEditTextConstraints(boolean notesVisibility) {
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) taskEditText.getLayoutParams();
        layoutParams.endToStart = notesVisibility ? upArrow.getId() : downArrow.getId();
        taskEditText.setLayoutParams(layoutParams);
    }

    private void startContainerMonitor() {
        containerMonitorHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isMonitoringActive) {
                    updateContainers();
                    containerMonitorHandler.postDelayed(this, 5000); // Retry after 5 seconds
                }
            }
        });
    }

    private void updateContainers() {
        int emptyCount = countEmptyContainers();

        if (emptyCount <= ADD_THRESHOLD) {
            addNewTaskContainer();
        } else if (containerStates.size() > MIN_CONTAINERS && emptyCount > REMOVE_THRESHOLD) {
            removeEmptyContainers(emptyCount - REMOVE_THRESHOLD);
        }
    }

    private int countEmptyContainers() {
        int count = 0;

        for (boolean state : containerStates) {
            if (!state) count++;
        }

        return count;
    }

    private void addNewTaskContainer() {
        View container = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);

        ImageButton downArrow = container.findViewById(R.id.down_arrow);
        ImageButton upArrow = container.findViewById(R.id.up_arrow);
        ConstraintLayout notesLayout = container.findViewById(R.id.notesLayout);

        notesLayout.setVisibility(View.GONE);
        upArrow.setVisibility(View.GONE);

        downArrow.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition(taskContainer);
            notesLayout.setVisibility(View.VISIBLE);
            downArrow.setVisibility(View.GONE);
            upArrow.setVisibility(View.VISIBLE);
        });

        upArrow.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition(taskContainer);
            notesLayout.setVisibility(View.GONE);
            downArrow.setVisibility(View.VISIBLE);
            upArrow.setVisibility(View.GONE);
        });

        taskContainer.addView(container);
        containerStates.add(false);
    }

    private void removeEmptyContainers(int count) {
        for (int i = containerStates.size() - 1; i >= 0 && count > 0; i--) {
            if (!containerStates.get(i)) {
                taskContainer.removeViewAt(i);
                containerStates.remove(i);
                count--;
            }
        }
    }

    private void loadTasksFromDatabase() {
        tasks.clear();
        tasks.addAll(dbHelper.getAllTasks());

        for (Task task : tasks) {
            View container = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);
            EditText taskEdit = container.findViewById(R.id.taskEditText);
            taskEdit.setText(task.getContent());
            setupTaskEditText(taskEdit);
            taskContainer.addView(container);
            containerStates.add(!task.getContent().isEmpty());      // first error
        }
    }

    private void setupTaskEditText(EditText editText) {
        // Add TextWatcher to the EditText to monitor text changes
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Get the index of the EditText and update the task state
                int index = getEditTextIndex(editText);
                updateTaskState(index, !s.toString().trim().isEmpty()); // Update state based on whether the task is filled
                saveTaskToDatabase(index, s.toString()); // Save the updated task to the database
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateTaskState(int index, boolean isFilled) {
        containerStates.set(index, isFilled);
    }

    private int getEditTextIndex(EditText editText) {
        View container = (View) editText.getParent();
        return  taskContainer.indexOfChild(container);
    }

    private void saveTaskToDatabase(int index, String content) {
        Task task = new Task(index, content);
        task.setPosition(index);
        dbHelper.updateTask(task.getPosition(), task.isCompleted(), task.getFilePath());
    }

    private void deleteTaskFromDatabase(int index) {
        dbHelper.deleteTask(index);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_TASK_STATES, new ArrayList<>(containerStates));
        outState.putString(KEY_NOTES, notesArea.getText().toString());
        outState.putInt(STATE_SCROLL_POSITION, scrollView.getScrollY());
        if (selectedFileUri != null) {
            outState.putString(KEY_FILE_URI, selectedFileUri.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreState(Bundle savedInstanceState) {
        containerStates.clear();
        containerStates.addAll((List<Boolean>) savedInstanceState.getSerializable(KEY_TASK_STATES));

        notesArea.setText(savedInstanceState.getString(KEY_NOTES, ""));

        String uriString = savedInstanceState.getString(KEY_FILE_URI);
        if (uriString != null) {
            selectedFileUri = Uri.parse(uriString);
            updateFilePreview();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            handleLandscapeMode();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            handlePortraitMode();
        }

        if (isKeyboardVisible(newConfig)) {
            scrollToActiveTask();
        }

        taskContainer.post(() -> {
            updateContainers();
            if (notesLayout.getVisibility() == View.VISIBLE) {
                updateTaskEditTextConstraints(true);
            }
        });
    }

    private void handleLandscapeMode() {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) taskContainer.getLayoutParams();
        params.setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        taskContainer.setLayoutParams(params);

        for (int i = 0; i < taskContainer.getChildCount(); i++) {
            View child = taskContainer.getChildAt(i);
            child.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.task_width_landscape);
        }
    }

    private void handlePortraitMode() {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) taskContainer.getLayoutParams();
        params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        taskContainer.setLayoutParams(params);

        for (int i = 0; i < taskContainer.getChildCount(); i++) {
            View child = taskContainer.getChildAt(i);
            child.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    private boolean isKeyboardVisible(Configuration config) {
        return (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO);
    }

    private void scrollToActiveTask() {
        View focusedChild = taskContainer.getFocusedChild();
        if (focusedChild != null) {
            scrollView.post(() -> scrollView.smoothScrollTo(0, focusedChild.getTop()));
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }
}


/*
public class MainActivity extends AppCompatActivity {

    private static final String KEY_TASK_STATES = "task_states";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_FILE_URI = "file_uri";
    private static final String STATE_SCROLL_POSITION = "scroll_position";
    private static final int REQUEST_PERMISSIONS = 1001;
//    private static final String READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;

    private CheckBox checkBox;
    private EditText notesArea;
    private ImageButton upArrow;
    private ImageButton downArrow;
    private EditText taskEditText;
    private ImageButton uploadButton;
    private ImageButton previewButton;
    private ConstraintLayout notesLayout;
    private LinearLayout taskContainer;
    private ScrollView scrollView;

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private DBHelper dbHelper;
    private Uri selectedFileUri;
    private final List<Boolean> containerStates = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();
    private final Handler containerMonitorHandler = new Handler(Looper.getMainLooper());
    private boolean isMonitoringActive = true;

    // Dynamic container management
    private final int MIN_CONTAINERS = 5;
    private final int ADD_THRESHOLD = 2;
    private final int REMOVE_THRESHOLD = 2;

//    private boolean isTaskEditMode = false;
//    private int currentTaskPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        checkPermissions();
        initializeViews();
        initializeDatabase();
        setupInitialState(savedInstanceState);
        setupClickListeners();
        setupFilePicker();
        startContainerMonitor();
    }

//    private void checkPermissions() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (checkSelfPermission(READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
//            }
//        }
//    }

    private void initializeViews() {
        upArrow = findViewById(R.id.up_arrow);
        notesArea = findViewById(R.id.notesArea);
        downArrow = findViewById(R.id.down_arrow);
        checkBox = findViewById(R.id.taskCheckbox);
        notesLayout = findViewById(R.id.notesLayout);
        taskEditText = findViewById(R.id.taskEditText);
        uploadButton = findViewById(R.id.uploadButton);
        previewButton = findViewById(R.id.previewButton);
        taskContainer = findViewById(R.id.taskContainer);
        scrollView = findViewById(R.id.scroll_view);
    }

    private void initializeDatabase() {
        dbHelper = new DBHelper(this);
        loadTasksFromDatabase();
    }

    private void setupInitialState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            notesLayout.setVisibility(View.GONE);
            upArrow.setVisibility(View.GONE);
            previewButton.setVisibility(View.GONE);

            for (int i = 0; i < MIN_CONTAINERS; i++) {
                addNewTaskContainer();
            }
        }
    }

    private void setupClickListeners() {
        downArrow.setOnClickListener(v -> toggleNotesVisibility(true));
        upArrow.setOnClickListener(v -> toggleNotesVisibility(false));
        uploadButton.setOnClickListener(v -> openFilePicker());
        previewButton.setOnClickListener(v -> showFilePreview());

//        setupTaskEditText(taskEditText);
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleFileSelection(result.getData().getData());
                    }
                }
        );
    }

    private void handleFileSelection(Uri uri) {
        if (uri != null) {
//            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            selectedFileUri = uri;
            updateFilePreview();
//            saveTask();
        }
    }

    private void handleFilePickerResult(ActivityResult result) {
        try {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedFileUri = result.getData().getData();
                if (selectedFileUri != null) {
                    updateFilePreview();
                }
            }
        } catch (Exception e) {
            showError("Error selecting file: " + e.getMessage());
        }
    }

    private void updateFilePreview() {
        uploadButton.setVisibility(View.GONE);
        previewButton.setVisibility(View.VISIBLE);

        try {
            Glide.with(this)
                    .load(selectedFileUri)
                    .error(R.drawable.ic_file_placeholder)
                    .into(previewButton);
        } catch (Exception e) {
            previewButton.setImageResource(R.drawable.ic_file_placeholder);
        }
    }

    private void showFilePreview() {
        if (selectedFileUri != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(selectedFileUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } catch (Exception e) {
                showError("Cannot open file: " + e.getMessage());
            }
        }
    }

    private void toggleNotesVisibility(boolean show) {
        TransitionManager.beginDelayedTransition(scrollView);

        notesLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        downArrow.setVisibility(show ? View.GONE : View.VISIBLE);
        upArrow.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateTaskEditTextConstraints(boolean notesVisibility) {
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) taskEditText.getLayoutParams();
        layoutParams.endToStart = notesVisibility ? upArrow.getId() : downArrow.getId();
        taskEditText.setLayoutParams(layoutParams);
    }

    private void startContainerMonitor() {
        containerMonitorHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isMonitoringActive) {
                    updateContainers();
                    containerMonitorHandler.postDelayed(this, 5000); // Retry after 5 seconds
                }
            }
        });
    }

//    private void startContainerMonitor() {
//        containerMonitorHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (isMonitoringActive) {
//                    updateContainers();
//                    containerMonitorHandler.postDelayed(this, 5000);
//                }
//            }
//        });
//    }
//        Runnable monitorRunnable = new Runnable() {
//            @Override
//            public void run() {
//                if (isMonitoringActive) {
//                    updateContainers();
//                    containerMonitorHandler.postDelayed(this, 5000);
//                }
//            }
//        };
//        containerMonitorHandler.post(monitorRunnable);
//        moniterThread.start();
//        new Thread(() -> {
//            while (true) {
//                try {
//                    Thread.sleep(5000);
//
//                    runOnUiThread(() -> {
//                    int emptyCount = 0;
//                    int totalCount = containerStates.size();
//
//                    for (boolean state : containerStates) {
//                        if (!state) emptyCount++;
//                    }
//
//                    if (emptyCount <= ADD_THRESHOLD) {
//                        addNewTaskContainer();
//                        addNewTaskContainer();
//                    }
//
//                    if (totalCount > MIN_CONTAINERS && emptyCount > REMOVE_THRESHOLD) {
//                        for (int i = containerStates.size()-1; i >= 0; i--) {
//                            if (!containerStates.get(i)) {
//                                removeTaskContainer(i);
//                                emptyCount--;
//                                if (emptyCount <= REMOVE_THRESHOLD) break;
//                            }
//                        }
//                    }
//                });
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }

    private void updateContainers() {
        int emptyCount = countEmptyContainers();

        if (emptyCount <= ADD_THRESHOLD) {
            addNewTaskContainer();
        } else if (containerStates.size() > MIN_CONTAINERS && emptyCount > REMOVE_THRESHOLD) {
            removeEmptyContainers(emptyCount - REMOVE_THRESHOLD);
        }
    }

    private int countEmptyContainers() {
        int count = 0;

        for (boolean state : containerStates) {
            if (!state) count++;
        }

        return count;
    }

    private void addNewContainer(int count) {
        for (int i = 0; i < count; i++) {
            addNewTaskContainer();
        }
    }

//    private void addNewTaskContainer() {
//        View container = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);
//        setupTaskView(container);
//        taskContainer.addView(container);
//        containerStates.add(false);

        // Save task position
//        Task task = new Task("", false);
//        task.setPosition(taskContainer.getChildCount() - 1);
//        tasks.add(task);
//
//        TransitionManager.beginDelayedTransition(taskContainer);
//    }

    private void addNewTaskContainer() {
        View container = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);

        ImageButton downArrow = container.findViewById(R.id.down_arrow);
        ImageButton upArrow = container.findViewById(R.id.up_arrow);
        ConstraintLayout notesLayout = container.findViewById(R.id.notesLayout);

        notesLayout.setVisibility(View.GONE);
        upArrow.setVisibility(View.GONE);

        downArrow.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition(taskContainer);
            notesLayout.setVisibility(View.VISIBLE);
            downArrow.setVisibility(View.GONE);
            upArrow.setVisibility(View.VISIBLE);
        });

        upArrow.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition(taskContainer);
            notesLayout.setVisibility(View.GONE);
            downArrow.setVisibility(View.VISIBLE);
            upArrow.setVisibility(View.GONE);
        });

        taskContainer.addView(container);
        containerStates.add(false);
    }

    private void setupTaskView(View taskView) {
        EditText taskEdit = taskView.findViewById(R.id.taskEditText);
        CheckBox checkBox = taskView.findViewById(R.id.taskCheckbox);
    }

    private void setupTaskEditText(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int index = getEditTextIndex(editText);
                updateTaskState(index, !s.toString().trim().isEmpty());
                saveTaskToDatabase(index, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

//        editText.setOnFocusChangeListener((v, hasFocus) -> {
//            if (hasFocus) {
//                currentTaskPosition = getEditTextIndex(editText);
//                isTaskEditMode = true;
//            }
//        });
    }


//    private void saveTask() {
//        if (currentTaskPosition != -1 && currentTaskPosition < tasks.size()) {
//            Task currentTask = tasks.get(currentTaskPosition);
//            View taskView = taskContainer.getChildAt(currentTaskPosition);
//            EditText taskEdit = taskView.findViewById(R.id.taskEditText);
//
//            currentTask.setContent(taskEdit.getText().toString());
//            currentTask.setFileUri(selectedFileUri != null ? selectedFileUri.toString() : null);
//
//            // Save to database
//            saveTaskToDatabase(currentTaskPosition, currentTask.getContent());
//
//            // Reset edit mode
//            isTaskEditMode = false;
//            currentTaskPosition = -1;
//        }
//    }

    private int getEditTextIndex(EditText editText) {
        View container = (View) editText.getParent();
        return taskContainer.indexOfChild(container);
    }

//    private void removeEmptyContainers(int count) {
//        int removed = 0;
//        for (int i = containerStates.size() - 1; i >= 0 && removed < count; i--) {
//            if (!containerStates.get(i)) {
//                removeTaskContainer(i);
//                removed++;
//            }
//        }
//    }

    private void removeEmptyContainers(int count) {
        for (int i = containerStates.size() - 1; i >= 0 && count > 0; i--) {
            if (!containerStates.get(i)) {
                taskContainer.removeViewAt(i);
                containerStates.remove(i);
                count--;
            }
        }
    }

    private void removeTaskContainer(int index) {
        TransitionManager.beginDelayedTransition(taskContainer);
        taskContainer.removeViewAt(index);
        containerStates.remove(index);
        deleteTaskFromDatabase(index);
    }

    private void loadTasksFromDatabase() {
        tasks.clear();
        tasks.addAll(dbHelper.getAllTasks());

        for (Task task : tasks) {
            View container = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);
            EditText taskEdit = container.findViewById(R.id.taskEditText);
            taskEdit.setText(task.getContent());
            setupTaskEditText(taskEdit);
            taskContainer.addView(container);
            containerStates.add(!task.getContent().isEmpty());
        }
    }

    private void saveTaskToDatabase(int index, String content) {
        Task task = new Task(index, content);
        task.setPosition(index);
        dbHelper.updateTask(task.getPosition(), task.isCompleted(), task.getFilePath());
    }

    private void deleteTaskFromDatabase(int index) {
        dbHelper.deleteTask(index);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_TASK_STATES, new ArrayList<>(containerStates));
        outState.putString(KEY_NOTES, notesArea.getText().toString());
        outState.putInt(STATE_SCROLL_POSITION, scrollView.getScrollY());
        if (selectedFileUri != null) {
            outState.putString(KEY_FILE_URI, selectedFileUri.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreState(Bundle savedInstanceState) {
        containerStates.clear();
        containerStates.addAll((List<Boolean>) savedInstanceState.getSerializable(KEY_TASK_STATES));

        notesArea.setText(savedInstanceState.getString(KEY_NOTES, ""));

        String uriString = savedInstanceState.getString(KEY_FILE_URI);
        if (uriString != null) {
            selectedFileUri = Uri.parse(uriString);
            updateFilePreview();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Handle orientation changes
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            handleLandscapeMode();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            handlePortraitMode();
        }

        // Handle keyboard visibility
        if (isKeyboardVisible(newConfig)) {
            scrollToActiveTask();
        }

        // Refresh container layout
        taskContainer.post(() -> {
            updateContainers();
            if (notesLayout.getVisibility() == View.VISIBLE) {
                updateTaskEditTextConstraints(true);
            }
        });
    }

    private void handleLandscapeMode() {
        // Adjust margins and padding for landscape
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) taskContainer.getLayoutParams();
        params.setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        taskContainer.setLayoutParams(params);

        // Adjust task row width if needed
        for (int i = 0; i < taskContainer.getChildCount(); i++) {
            View child = taskContainer.getChildAt(i);
            child.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.task_width_landscape);
        }
    }

    private void handlePortraitMode() {
        // Reset margins and padding for portrait
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) taskContainer.getLayoutParams();
        params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        taskContainer.setLayoutParams(params);

        // Reset task row width
        for (int i = 0; i < taskContainer.getChildCount(); i++) {
            View child = taskContainer.getChildAt(i);
            child.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    private boolean isKeyboardVisible(Configuration config) {
        return (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO);
    }

    private void scrollToActiveTask() {
        View focusedChild = taskContainer.getFocusedChild();
        if (focusedChild != null) {
            scrollView.post(() -> scrollView.smoothScrollTo(0, focusedChild.getTop()));
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.setType("*"/"");    have to make it change it
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private void updateTaskState(int index, boolean isFilled) {
        containerStates.set(index, isFilled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isMonitoringActive = false;
        containerMonitorHandler.removeCallbacksAndMessages(null);
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
*/
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


