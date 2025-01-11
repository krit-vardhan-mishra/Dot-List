package com.just_for_fun.dotlist

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var checkBox: CheckBox
    private lateinit var notesArea: EditText
    private lateinit var upArrow: ImageButton
    private lateinit var downArrow: ImageButton
    private lateinit var taskEditText: EditText
    private lateinit var deleteButton: ImageButton
    private lateinit var uploadButton: ImageButton
    private lateinit var previewButton: ImageButton
    private lateinit var notesLayout: ConstraintLayout
    private lateinit var taskContainer: LinearLayout
    private lateinit var scrollView: ScrollView

    private lateinit var fileWriteAccessLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var dbHelper: DBHelper
    private var selectedFileUri: Uri? = null
    private val containerStates: MutableList<Boolean> = ArrayList()
    private val tasks: MutableList<Task> = ArrayList()
    private val containerMonitorHandler = Handler(Looper.getMainLooper())
    private val isMonitoringActive = true

    private val MIN_CONTAINERS = 5
    private val ADD_THRESHOLD = 2
    private val REMOVE_THRESHOLD = 2

    private companion object {
        private const val KEY_TASK_STATES = "task_states"
        private const val KEY_NOTES = "notes"
        private const val KEY_FILE_URI = "file_uri"
        private const val STATE_SCROLL_POSITION = "scroll_position"
        private const val REQUEST_PERMISSIONS_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        loadAppData()
        initializeDatabase()
        setupInitialState(savedInstanceState)
        setupClickListeners()
        setupFilePicker()
        startContainerMonitor()
        checkAndRequestPermissions()
    }

    private fun initializeViews() {
        upArrow = findViewById(R.id.up_arrow)
        notesArea = findViewById(R.id.notesArea)
        downArrow = findViewById(R.id.down_arrow)
        checkBox = findViewById(R.id.taskCheckbox)
        notesLayout = findViewById(R.id.notesLayout)
        taskEditText = findViewById(R.id.taskEditText)
        uploadButton = findViewById(R.id.uploadButton)
        previewButton = findViewById(R.id.previewButton)
        taskContainer = findViewById(R.id.taskContainer)
        scrollView = findViewById(R.id.scroll_view)
        deleteButton = findViewById(R.id.deleteButton)
    }

    private fun initializeDatabase() {
        dbHelper = DBHelper(this)

        try {
            loadTasksAsync()
        } catch (e: Exception) {
            showError("Failed to load task: " + e.message)
        }
    }

    private fun loadTasksAsync() {
        val executor = Executors.newSingleThreadExecutor()
        val mainHandler = Handler(Looper.getMainLooper())

        executor.execute {
            try {
                val dbTasks = dbHelper.allTasks
                val activityRef = WeakReference(this)
                mainHandler.post {
                    val activity = activityRef.get()
                    activity?.let {
                        tasks.clear()
                        tasks.addAll(dbTasks)
                        populateTaskContainers()
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    showError("Failed to load tasks: ${e.message}")
                }
            }
        }
    }

    private fun setupInitialState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            notesLayout.visibility = View.GONE
            upArrow.visibility = View.GONE
            previewButton.visibility = View.GONE

            if (tasks.isEmpty()) {
                repeat(MIN_CONTAINERS) {
                    addNewTaskContainer()
                }
            }

            setupCheckBoxListener()
            setupPreviewButtonListener()
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        val booleanArray = savedInstanceState.getBooleanArray(KEY_TASK_STATES)

        booleanArray?.let {
            containerStates.clear()
            containerStates.addAll(it.toList())
        }

        notesArea.setText(savedInstanceState.getString(KEY_NOTES, ""))

        savedInstanceState.getString(KEY_FILE_URI)?.let {
            selectedFileUri = Uri.parse(it)
            updateFilePreview()
        }
    }


    private fun setupCheckBoxListener() {
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                notesArea.paintFlags = notesArea.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                notesArea.setTextColor(getColor(android.R.color.holo_red_dark))
            } else {
                notesArea.paintFlags = notesArea.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                notesArea.setTextColor(getColor(R.color.notesColor))
            }
        }
    }

    private fun setupPreviewButtonListener() {
        previewButton.setOnLongClickListener {
            deleteButton.visibility = View.VISIBLE
            true
        }

        deleteButton.setOnClickListener {
            deleteTaskFromDatabase(getEditTextIndex(taskEditText))
            removeUploadedFile()
        }
    }

    private fun setupClickListeners() {
        downArrow.setOnClickListener { toggleNotesVisibility(true) }
        upArrow.setOnClickListener { toggleNotesVisibility(false) }
        uploadButton.setOnClickListener { openFilePicker() }
        previewButton.setOnClickListener { showFilePreview() }
    }

    private fun toggleNotesVisibility(show: Boolean) {
        TransitionManager.beginDelayedTransition(scrollView)
        notesLayout.visibility = if (show) View.VISIBLE else View.GONE

        val layoutParams = taskEditText.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.endToStart = if (show) upArrow.id else downArrow.id
        layoutParams.startToEnd = checkBox.id
        taskEditText.layoutParams = layoutParams

        downArrow.visibility = if (show) View.GONE else View.VISIBLE
        upArrow.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setupFilePicker() {
        fileWriteAccessLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Handle the result here
                // The user has granted permission to modify the file
                updateFilePreview()
            } else {
                showError("File modification permission denied.")
            }
        }

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                handleFileSelection(uri)
            } else {
                showError("File selection canceled or failed.")
            }
        }
    }

    private fun handleFileSelection(uri: Uri?) {
        uri?.let {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val writeRequestIntent = MediaStore.createWriteRequest(contentResolver, listOf(it))
                    val intentSender = writeRequestIntent.intentSender
                    val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()

                    // Use fileWriteAccessLauncher.launch() with IntentSenderRequest
                    fileWriteAccessLauncher.launch(intentSenderRequest)
                } else {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    selectedFileUri = it
                    updateFilePreview()
                }
            } catch (e: IntentSender.SendIntentException) {
                showError("Failed to select file: ${e.message}")
            } catch (e: Exception) {
                showError("An unexpected error occurred: ${e.message}")
            }
        }
    }


    private fun updateFilePreview() {
        uploadButton.visibility = View.GONE
        previewButton.visibility = View.VISIBLE

        selectedFileUri?.let { uri ->
            try {
                val mimeType = contentResolver.getType(uri)
                if (mimeType?.startsWith("image/") == true) {
                    Glide.with(this).load(uri).placeholder(R.drawable.ic_loading_spinner).error(R.drawable.ic_file_error).into(previewButton)
                } else {
                    previewButton.setImageResource(R.drawable.preview_icon)
                }
            } catch (e: Exception) {
                previewButton.setImageResource(R.drawable.preview_icon)
                showError("Failed to load file preview")
            }
        }
    }

    private fun showFilePreview() {
        selectedFileUri?.let { uri ->
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = uri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            } catch (e: Exception) {
                showError("Cannot open file: ${e.message}")
            }
        }
    }

    private fun removeUploadedFile() {
        selectedFileUri = null // Clear the reference
        previewButton.visibility = View.GONE
        deleteButton.visibility = View.GONE
        uploadButton.visibility = View.VISIBLE
        Toast.makeText(this, "File removed successfully", Toast.LENGTH_SHORT).show()
    }

    private fun updateTaskEditTextConstraints(notesVisibility: Boolean) {
        val layoutParams = taskEditText.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.endToStart = if (notesVisibility) upArrow.id else downArrow.id
        taskEditText.layoutParams = layoutParams
    }

    private fun addNewTaskContainer() {
        val container = layoutInflater.inflate(R.layout.task_row, taskContainer, false)

        val taskEditText: EditText = container.findViewById(R.id.taskEditText)
        val checkBox: CheckBox = container.findViewById(R.id.taskCheckbox)
        val downArrow: ImageButton = container.findViewById(R.id.down_arrow)
        val upArrow: ImageButton = container.findViewById(R.id.up_arrow)
        val notesLayout: ConstraintLayout = container.findViewById(R.id.notesLayout)

        notesLayout.visibility = View.GONE
        upArrow.visibility = View.GONE

        downArrow.setOnClickListener {
            val transition: Transition = AutoTransition()
            transition.duration = 500

            TransitionManager.beginDelayedTransition(taskContainer)
            notesLayout.visibility = View.VISIBLE
            downArrow.visibility = View.GONE
            upArrow.visibility = View.VISIBLE

            val layoutParams = taskEditText.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.endToStart = upArrow.id
            layoutParams.startToEnd = checkBox.id
            taskEditText.layoutParams = layoutParams
        }

        upArrow.setOnClickListener {
            val transition: Transition = AutoTransition()
            transition.duration = 500

            TransitionManager.beginDelayedTransition(taskContainer)
            notesLayout.visibility = View.GONE
            downArrow.visibility = View.VISIBLE
            upArrow.visibility = View.GONE

            val layoutParams = taskEditText.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.endToStart = downArrow.id
            layoutParams.startToEnd = checkBox.id
            taskEditText.layoutParams = layoutParams
        }

        taskContainer.addView(container)
        containerStates.add(false)
    }

    private fun removeEmptyContainers(count: Int) {
        var remaining = count
        for (i in containerStates.size - 1 downTo 0) {
            if (!containerStates[i] && remaining > 0) {
                taskContainer.removeViewAt(i)
                containerStates.removeAt(i)
                remaining--
            }
        }
    }

    private fun startContainerMonitor() {
        containerMonitorHandler.postDelayed(object : Runnable {
            override fun run() {
                if (isMonitoringActive) {
                    updateContainers()
                    handleContainerMonitor()
                    containerMonitorHandler.postDelayed(this, 500)
                }
            }
        }, 5000)
    }

    private fun handleContainerMonitor() {
        if (taskContainer.childCount > ADD_THRESHOLD) {
            addNewTaskContainer()
        }
        if (taskContainer.childCount > REMOVE_THRESHOLD) {
            removeTaskContainer()
        }
    }

    private fun removeTaskContainer() {
        val lastTask = taskContainer.getChildAt(taskContainer.childCount - 1)
        taskContainer.removeView(lastTask)
    }

    private fun updateContainers() {
        val emptyCount = countEmptyContainers()

        while (containerStates.size < taskContainer.childCount) {
            containerStates.add(false)
        }
        while (containerStates.size > taskContainer.childCount) {
            containerStates.removeAt(containerStates.size - 1)
        }

        if (emptyCount <= ADD_THRESHOLD) {
            addNewTaskContainer()
        } else if (containerStates.size > MIN_CONTAINERS && emptyCount > REMOVE_THRESHOLD) {
            removeEmptyContainers(emptyCount - REMOVE_THRESHOLD)
        }
    }

    private fun countEmptyContainers(): Int {
        return containerStates.count { !it  }
    }

    private fun populateTaskContainers() {
        for (task in tasks) {
            if (task.content == null) {
                continue
            }

            val container = layoutInflater.inflate(R.layout.task_row, taskContainer, false)
            val taskEdit = container.findViewById<EditText>(R.id.taskEditText)

            taskEdit.setText(task.title)
            setupTaskEditText(taskEdit)
            taskContainer.addView(container)
            containerStates.add(task.content != null && task.content!!.isNotEmpty())
        }
    }

    private fun setupTaskEditText(editText: EditText) {
        val container = editText.parent as View
        val checkBox = container.findViewById<CheckBox>(R.id.taskCheckbox)
        val notesArea = container.findViewById<EditText>(R.id.notesArea)

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val index = getEditTextIndex(editText)
                val taskTitle = s.toString()
                val taskNotes = notesArea.text.toString()
                val isTaskCompleted = checkBox.isChecked

                val task = Task()
                task.position = index
                task.title = taskTitle
                task.isCompleted = isTaskCompleted

                val details = TaskDetails()
                details.notes = taskNotes
                if (selectedFileUri != null) {
                    details.filePath = selectedFileUri.toString()
                }
                task.details = details

                saveTaskToDatabase(task)
                updateTaskState(
                    index,
                    s.toString().trim { it <= ' ' }.isNotEmpty()
                )
            }

            override fun afterTextChanged(s: Editable) {
                val updatedNotes = s.toString()
                val currentTask = tasks[getEditTextIndex(editText)]
                currentTask.details?.notes = updatedNotes

                dbHelper.updateTask(currentTask)
            }
        })

        notesArea.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val index = getEditTextIndex(editText)
                saveTaskToDatabase(createTaskFromViews(editText, notesArea, checkBox, index))
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        checkBox.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean ->
            val index = getEditTextIndex(editText)
            saveTaskToDatabase(createTaskFromViews(editText, notesArea, checkBox, index))
        }
    }

    private fun createTaskFromViews(
        taskEdit: EditText,
        notesArea: EditText,
        checkBox: CheckBox,
        index: Int
    ): Task {
        return Task().apply {
            position = index
            title = taskEdit.text.toString()
            isCompleted = checkBox.isChecked
            details = TaskDetails().apply {
                notes = notesArea.text.toString()
                filePath = selectedFileUri?.toString()
            }
        }
    }

    private fun updateTaskState(index: Int, isFilled: Boolean) {
        containerStates[index] = isFilled
    }

    private fun getEditTextIndex(editText: EditText): Int {
        val container = editText.parent as View
        return taskContainer.indexOfChild(container)
    }

    private fun saveTaskToDatabase(task: Task) {
        try {
            val existingTask = dbHelper.getTaskByPosition(task.position)
            if (existingTask != null) {
                task.id = existingTask.id
                val updateCount = dbHelper.updateTask(task)

                if (updateCount > 0) {
                    val index = tasks.indexOfFirst { it.position == task.position }

                    if (index != -1) {
                        tasks[index] = task
                    } else {
                        tasks.add(task)
                    }
                }
            } else {
                val newId = dbHelper.insertTask(task)
                task.id = newId.toInt()
                tasks.add(task)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving task: ${e.message}")
            showError("Failed to save task")
        }
    }

    private fun deleteTaskFromDatabase(index: Int) {
        dbHelper.deleteTask(index)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_TASK_STATES, ArrayList(containerStates))
        outState.putString(KEY_NOTES, notesArea.text.toString())
        outState.putInt(STATE_SCROLL_POSITION, scrollView.scrollY)
        selectedFileUri?.let { outState.putString(KEY_FILE_URI, it.toString()) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            handleLandscapeMode()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            handlePortraitMode()
        }

        if (isKeyboardVisible(newConfig)) {
            scrollToActiveTask()
        }

        taskContainer.post {
            updateContainers()
            if (notesLayout.visibility == View.VISIBLE) {
                updateTaskEditTextConstraints(true)
            }
        }
    }

    private fun handleLandscapeMode() {
        val params = taskContainer.layoutParams as MarginLayoutParams
        params.setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
        taskContainer.layoutParams = params

        for (i in 0 until taskContainer.childCount) {
            taskContainer.getChildAt(i).layoutParams.width = resources.getDimensionPixelSize(R.dimen.task_width_landscape)
        }
    }

    private fun handlePortraitMode() {
        val params = taskContainer.layoutParams as MarginLayoutParams
        params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        taskContainer.layoutParams = params

        for (i in 0 until taskContainer.childCount) {
            taskContainer.getChildAt(i).layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    private fun isKeyboardVisible(config: Configuration): Boolean {
        return (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
    }

    private fun scrollToActiveTask() {
        val focusedChild = taskContainer.focusedChild
        focusedChild?.let {
            scrollView.post {
                scrollView.smoothScrollTo(0, it.top)
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        val mimeTypes = arrayOf("application/pdf", "text/plain")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        filePickerLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        saveAppData()
    }

    private fun saveAppData() {
        getSharedPreferences("AppData", MODE_PRIVATE).edit().putString("notes", notesArea.text.toString()).apply()
    }

    private fun loadAppData() {
        val savedNotes = getSharedPreferences("AppData", MODE_PRIVATE).getString("notes", "") ?: ""
        notesArea.setText(savedNotes)
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = mutableListOf<String>()

            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (permissions.isNotEmpty()) {
                requestPermissions(permissions.toTypedArray(), REQUEST_PERMISSIONS_CODE)
            }
        }
    }

}