package com.just_for_fun.dotlist.ui.main

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color.*
import android.graphics.Typeface
import android.net.Uri.*
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.ActionMode
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.just_for_fun.dotlist.R
import com.just_for_fun.dotlist.data.local.entities.NoteFormattingEntity
import com.just_for_fun.dotlist.data.local.entities.Task
import com.just_for_fun.dotlist.domain.models.TextStyle
import com.just_for_fun.dotlist.utils.ToastUtil
import com.just_for_fun.taskview.TaskDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskAdapter(
    private val context: Context,
    private val taskDatabase: TaskDatabase,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onFilePickerLauncher: (taskId: Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DiffCallBack()) {

    private val expandedItems = mutableListOf<Long>()

    class DiffCallBack : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.title == newItem.title &&
                    oldItem.notes == newItem.notes &&
                    oldItem.isChecked == newItem.isChecked &&
                    oldItem.attachment == newItem.attachment
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {

        private val checkBox: CheckBox = itemView.findViewById(R.id.taskCheckbox)
        private val notesArea: EditText = itemView.findViewById(R.id.notesArea)
        private val upArrow: ImageButton = itemView.findViewById(R.id.up_arrow)
        private val downArrow: ImageButton = itemView.findViewById(R.id.down_arrow)
        private val taskEditText: EditText = itemView.findViewById(R.id.taskEditText)
        private val uploadButton: ImageButton = itemView.findViewById(R.id.uploadButton)
        private val previewButton: ImageButton = itemView.findViewById(R.id.previewButton)
        private val notesLayout: View = itemView.findViewById(R.id.notesLayout)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        private var currentTask: Task? = null
        private var taskUpdateJob: Job? = null
        private var visibilityUpdateJob: Job? = null

        init {
            notesArea.customSelectionActionModeCallback = object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    val inflater = mode?.menuInflater
                    inflater?.inflate(R.menu.task_actions, menu)
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    return false
                }

                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                    return when (item?.itemId) {
                        R.id.action_bold -> {
                            applyFormatting(TextStyle.BOLD)
                            mode?.finish()
                            true
                        }
                        R.id.action_italic -> {
                            applyFormatting(TextStyle.ITALIC)
                            mode?.finish()
                            true
                        }
                        R.id.action_underline -> {
                            applyFormatting(TextStyle.UNDERLINE)
                            mode?.finish()
                            true
                        }
                        R.id.action_strikethrough -> {
                            applyFormatting(TextStyle.STRIKETHROUGH)
                            mode?.finish()
                            true
                        }
                        R.id.action_color -> {
                            applyFormatting(TextStyle.COLOR)
                            mode?.finish()
                            true
                        }
                        R.id.action_normal -> {
                            removeFormattingFromSelection()
                            mode?.finish()
                            true
                        }
                        else -> false
                    }
                }

                override fun onDestroyActionMode(mode: ActionMode?) {
                    // Cleanup if needed
                }
            }
        }

        fun bind(task: Task) {
            currentTask = task
            loadTaskDataIntoViews(task)
            setupListeners()
        }

        private fun loadTaskDataIntoViews(task: Task) {
            lifecycleScope.launch(Dispatchers.IO) {
                val formattingList = taskDatabase.getFormattingForTask(task.id)
                val spannable = applyFormattingToText(task.notes, formattingList)

                withContext(Dispatchers.Main) {
                    taskEditText.setText(task.title)
                    notesArea.setText(spannable)
                    checkBox.isChecked = task.isChecked
                    updateAttachmentButtonsVisibility(task.attachment)
                    updateArrowVisibility(task.title.isNotEmpty())

                    val isExpanded = expandedItems.contains(task.id)
                    updateVisibilityState(isExpanded)
                }
            }
        }

        private fun updateVisibilityState(show: Boolean) {
            visibilityUpdateJob?.cancel()
            visibilityUpdateJob = lifecycleScope.launch {
                delay(100)
                withContext(Dispatchers.Main) {
                    notesLayout.visibility = if (show) View.VISIBLE else View.GONE
                    upArrow.visibility = if (show) View.VISIBLE else View.GONE
                    downArrow.visibility = if (show) View.GONE else View.VISIBLE
                }
            }
        }

        private fun setupListeners() {
            setupTextWatchers()
            setupCheckboxListener()
            setupArrowListeners()
            setupAttachmentListeners()
        }

        private fun setupTextWatchers() {
            taskEditText.addTextChangedListener(createTitleTextWatcher())
            notesArea.addTextChangedListener(createNotesTextWatcher())
        }

        private fun createTitleTextWatcher() = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateArrowVisibility(s?.isNotEmpty() == true)
            }
            override fun afterTextChanged(s: Editable) {
                taskUpdateJob?.cancel()
                taskUpdateJob = lifecycleScope.launch {
                    delay(2000)
                    currentTask?.let {
                        val updatedTask = it.copy(title = s.toString())
                        taskDatabase.updateTask(updatedTask)
                    }
                }
            }
        }

        private fun createNotesTextWatcher() = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                taskUpdateJob?.cancel()
                taskUpdateJob = lifecycleScope.launch {
                    delay(2000)
                    currentTask?.let {
                        val updatedTask = it.copy(notes = s.toString())
                        taskDatabase.updateTask(updatedTask)
                    }
                }
            }
        }

        private fun setupCheckboxListener() {
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                currentTask?.let {
                    val updatedTask = it.copy(isChecked = isChecked)
                    lifecycleScope.launch {
                        taskDatabase.updateTask(updatedTask)
                    }
                }
            }

            checkBox.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showDeleteConfirmationDialog(position)
                }
                true
            }
        }

        private fun showDeleteConfirmationDialog(position: Int) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Delete Task")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete") { _, _ -> deleteTask(position) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun deleteTask(position: Int) {
            if (position == RecyclerView.NO_POSITION) return

            val taskToDelete = getItem(position)
            lifecycleScope.launch {
                taskDatabase.deleteTask(taskToDelete)
                val updatedList = currentList.toMutableList().apply {
                    removeAt(position)
                }
                submitList(updatedList)
            }

            ToastUtil.showCustomToast(context, "Task Deleted.", 2, RED);
        }

        private fun setupArrowListeners() {
            downArrow.setOnClickListener {
                if (downArrow.isEnabled) {
                    currentTask?.let { task ->
                        expandedItems.add(task.id)
                        updateVisibilityState(true)
                    }
                } else {
                    ToastUtil.showCustomToast(context, "Please enter a task title first.", 2)
                }
            }

            upArrow.setOnClickListener {
                currentTask?.let { task ->
                    expandedItems.remove(task.id)
                    updateVisibilityState(false)
                }
            }
        }

        private fun setupAttachmentListeners() {
            uploadButton.setOnClickListener {
                currentTask?.let { task -> onFilePickerLauncher(task) }
            }


            previewButton.setOnClickListener {
                currentTask?.attachment?.let { uri ->
                    openFilePreview(uri)
                }
            }

            previewButton.setOnLongClickListener {
                deleteButton.visibility = View.VISIBLE
                true
            }

            deleteButton.setOnClickListener {
                currentTask?.let { task ->
                    val updatedTask = task.copy(attachment = null)
                    lifecycleScope.launch {
                        taskDatabase.updateTask(updatedTask)
                        withContext(Dispatchers.Main) {
                            updateAttachmentButtonsVisibility(null)
                        }
                    }
                }
                ToastUtil.showCustomToast(context, "Task Attachment Deleted.", 2)
            }
        }

        private fun updateAttachmentButtonsVisibility(attachment: String?) {
            uploadButton.visibility = if (attachment == null) View.VISIBLE else View.GONE
            previewButton.visibility = if (attachment != null) View.VISIBLE else View.GONE
            deleteButton.visibility = View.GONE
        }

        private fun updateArrowVisibility(isTaskTitleNotEmpty: Boolean) {
            downArrow.isEnabled = isTaskTitleNotEmpty
        }

        private fun openFilePreview(uriString: String) {
            val uri = parse(uriString)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, context.contentResolver.getType(uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }

        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            if (notesArea.hasSelection()) {
                return
            }

            val inflater = MenuInflater(v?.context)
            inflater.inflate(R.menu.task_actions, menu)

            menu?.findItem(R.id.action_bold)?.setOnMenuItemClickListener {
                applyFormatting(TextStyle.BOLD)
                true
            }

            menu?.findItem(R.id.action_italic)?.setOnMenuItemClickListener {
                applyFormatting(TextStyle.ITALIC)
                true
            }

            menu?.findItem(R.id.action_underline)?.setOnMenuItemClickListener {
                applyFormatting(TextStyle.UNDERLINE)
                true
            }

            menu?.findItem(R.id.action_strikethrough)?.setOnMenuItemClickListener {
                applyFormatting(TextStyle.STRIKETHROUGH)
                true
            }

            menu?.findItem(R.id.action_color)?.setOnMenuItemClickListener {
                applyFormatting(TextStyle.COLOR)
                true
            }
        }

        private fun applyFormatting(style: TextStyle) {
            val start = notesArea.selectionStart
            val end = notesArea.selectionEnd

            if (start == end) return

            val spannable = notesArea.text as Spannable
            when (style) {
                TextStyle.BOLD -> spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                TextStyle.ITALIC -> spannable.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                TextStyle.UNDERLINE -> spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                TextStyle.STRIKETHROUGH -> spannable.setSpan(StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                TextStyle.COLOR -> showColorPicker { selectedColor ->
                    spannable.setSpan(ForegroundColorSpan(selectedColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    saveFormatting(style, start, end, selectedColor)
                }
                TextStyle.NORMAL -> {
                    val spans = spannable.getSpans(start, end, Any::class.java)
                    for (span in spans) {
                        spannable.removeSpan(span)
                    }
                }
            }
            saveFormatting(style, start, end)
        }

        private fun saveFormatting(style: TextStyle, start: Int, end: Int, color: Int = 0) {
            currentTask?.let { task ->
                val formatting = NoteFormattingEntity(
                    taskId = task.id,
                    start = start,
                    end = end,
                    style = style,
                    color = color
                )
                lifecycleScope.launch {
                    taskDatabase.insertNoteFormatting(formatting)
                }
            }
        }

        private fun applyFormattingToText(text: String, formattingList: List<NoteFormattingEntity>): SpannableString {
            val spannable = SpannableString(text)

            formattingList.forEach { formatting ->
                when (formatting.style) {
                    TextStyle.BOLD -> spannable.setSpan(StyleSpan(Typeface.BOLD), formatting.start, formatting.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    TextStyle.ITALIC -> spannable.setSpan(StyleSpan(Typeface.ITALIC), formatting.start, formatting.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    TextStyle.UNDERLINE -> spannable.setSpan(UnderlineSpan(), formatting.start, formatting.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    TextStyle.STRIKETHROUGH -> spannable.setSpan(StrikethroughSpan(), formatting.start, formatting.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    TextStyle.COLOR -> spannable.setSpan(ForegroundColorSpan(formatting.color), formatting.start, formatting.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    TextStyle.NORMAL -> {
                        val spans = spannable.getSpans(formatting.start, formatting.end, Any::class.java)
                        for (span in spans) {
                            spannable.removeSpan(span)
                        }
                    }
                }
            }
            return spannable
        }

        private fun showColorPicker(onColorSelected: (Int) -> Unit) {
            val colors = listOf(
                RED to "Red",
                GREEN to "Green",
                BLUE to "Blue",
                YELLOW to "Yellow",
                BLACK to "Black",
                WHITE to "White",
                CYAN to "Cyan"
            )

            val colorNames = colors.map { it.second }.toTypedArray()

            AlertDialog.Builder(itemView.context)
                .setTitle("Choose a Color")
                .setItems(colorNames) { _, which ->
                    val selectedColor = colors[which].first
                    onColorSelected(selectedColor)
                }
                .show()
        }

        private fun removeFormattingFromSelection() {
            val spannable = notesArea.text as Spannable
            val selStart = notesArea.selectionStart
            val selEnd = notesArea.selectionEnd

            val spans = spannable.getSpans(selStart, selEnd, Any::class.java)
            for (span in spans) {
                if (span is StyleSpan || span is UnderlineSpan || span is StrikethroughSpan || span is ForegroundColorSpan) {
                    val spanStart = spannable.getSpanStart(span)
                    val spanEnd = spannable.getSpanEnd(span)
                    val flags = spannable.getSpanFlags(span)

                    if (spanStart < selEnd && spanEnd > selStart) {
                        spannable.removeSpan(span)
                    }

                    if (spanStart < selStart) {
                        when (span) {
                            is StyleSpan -> spannable.setSpan(StyleSpan(span.style), spanStart, selStart, flags)
                            is UnderlineSpan -> spannable.setSpan(UnderlineSpan(), spanStart, selStart, flags)
                            is StrikethroughSpan -> spannable.setSpan(StrikethroughSpan(), spanStart, selStart, flags)
                            is ForegroundColorSpan -> spannable.setSpan(ForegroundColorSpan(span.foregroundColor), spanStart, selStart, flags)
                        }
                    }

                    if (spanEnd > selEnd) {
                        when (span) {
                            is StyleSpan -> spannable.setSpan(StyleSpan(span.style), selEnd, spanEnd, flags)
                            is UnderlineSpan -> spannable.setSpan(UnderlineSpan(), selEnd, spanEnd, flags)
                            is StrikethroughSpan -> spannable.setSpan(StrikethroughSpan(), selEnd, spanEnd, flags)
                            is ForegroundColorSpan -> spannable.setSpan(ForegroundColorSpan(span.foregroundColor), selEnd, spanEnd, flags)
                        }
                    }
                }
            }

            currentTask?.let { task ->
                lifecycleScope.launch {
                    taskDatabase.deleteFormattingInRange(task.id, selStart, selEnd)
                }
            }
        }

        fun clearPendingJobs() {
            visibilityUpdateJob?.cancel()
            taskUpdateJob?.cancel()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_view, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.clearPendingJobs()
    }
}