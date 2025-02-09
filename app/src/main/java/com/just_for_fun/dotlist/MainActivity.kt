package com.just_for_fun.dotlist

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.just_for_fun.dotlist.data.local.entities.Task
import com.just_for_fun.dotlist.ui.main.TaskAdapter
import com.just_for_fun.dotlist.ui.viewmodel.TaskViewModel
import com.just_for_fun.dotlist.ui.viewmodel.TaskViewModelFactory
import com.just_for_fun.dotlist.utils.ToastUtil
import com.just_for_fun.taskview.TaskDatabase

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: View
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskDatabase: TaskDatabase
    private lateinit var filePickerLauncher: ActivityResultLauncher<Array<String>>
    private var currentPickerTask: Task? = null
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskDatabase = TaskDatabase(this)
        taskViewModel = ViewModelProvider(this, TaskViewModelFactory(taskDatabase))[TaskViewModel::class.java]
        recyclerView = findViewById(R.id.recycler_view)
        emptyTextView = findViewById(R.id.emptyTextView)

        setupRecyclerView()
        setupFilePicker()
        setupFAB()
        observeTasks()

        taskViewModel.allTasks.observe(this) { tasks ->
            if (tasks.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyTextView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyTextView.visibility = View.GONE
                taskAdapter.submitList(tasks)
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(
            context = this,
            taskDatabase = taskDatabase,
            lifecycleScope = lifecycleScope,
            onFilePickerLauncher = { task ->
                currentPickerTask = task
                filePickerLauncher.launch(arrayOf("*/*"))
            }
        )
        recyclerView.adapter = taskAdapter
    }

    private fun setupFAB() {
        findViewById<FloatingActionButton>(R.id.floatingActionButton).setOnClickListener {
            lifecycleScope.launch {
                val newTask = Task(id = 0, title = "", notes = "", isChecked = false, attachment = null)
                taskDatabase.insertTask(newTask)
            }
        }
    }

    private fun observeTasks() {
        lifecycleScope.launch {
            taskDatabase.getAllTask().collect { tasks ->
                taskAdapter.submitList(tasks)
            }
        }
    }

    private fun setupFilePicker() {
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { uri ->
                lifecycleScope.launch {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )

                        Log.d("File Picker", "URI: $uri")
                        Log.d("File Picker", "Persisted Permission: ${contentResolver.persistedUriPermissions}")
                        Log.d("Task File Picker", "File selected: $uri")

                        val updatedTask = currentPickerTask?.copy(attachment = uri.toString())
                        if (updatedTask != null) {
                            taskDatabase.updateTask(updatedTask)
                            openDialogBox()
                        }
                    } catch (e: SecurityException) {
                        withContext(Dispatchers.Main) {
                            Log.e("Task File Picker", "Permission error: ${e.message}")

                            ToastUtil.showCustomToast(this@MainActivity, "Permission error: ${e.message}", 2)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("Task File Picker", "Error uploading file: ${e.message}")
                            ToastUtil.showCustomToast(this@MainActivity, "Error uploading file: ${e.message}", 2)
                        }
                    }
                }
            } ?: ToastUtil.showCustomToast(this@MainActivity, "No file selected.", 2)
        }
    }

    private fun openDialogBox() {
        AlertDialog.Builder(this)
            .setTitle("Uploaded File")
            .setMessage("The file path is stored as a reference. If you delete the file from storage after uploading, the preview button will throw an error.")
            .setPositiveButton("ok") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (Request all media permissions)
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            // Android 12 and below
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        // Request permissions only if there are missing ones
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
            requestPermissions(permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                val deniedPermissions = mutableListOf<String>()

                // Check which permissions were granted/denied
                for (i in permissions.indices) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissions.add(permissions[i])
                    }
                }

                if (!deniedPermissions.isEmpty()) {
                    // Some permissions were denied
                    var message = "Required permissions not granted: "

                    // Create user-friendly permission names
                    val friendlyNames = deniedPermissions.map { permission ->
                        when (permission) {
                            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage"
                            Manifest.permission.READ_MEDIA_IMAGES -> "Photos"
                            Manifest.permission.READ_MEDIA_VIDEO -> "Videos"
                            Manifest.permission.READ_MEDIA_AUDIO -> "Audio"
                            else -> permission
                        }
                    }

                    message += friendlyNames.joinToString(", ")

                    // Check if user clicked "Don't ask again" for any permission
                    val shouldShowRationale = deniedPermissions.any { permission ->
                        shouldShowRequestPermissionRationale(permission)
                    }

                    if (shouldShowRationale) {
                        AlertDialog.Builder(this)
                            .setTitle("Permissions Required")
                            .setMessage("These permissions are needed to properly save and display your task attachments. Would you like to grant them now?")
                            .setPositiveButton("Yes") { _, _ ->
                                // Request permissions again
                                checkAndRequestPermissions()
                            }
                            .setNegativeButton("No") { dialog, _ ->
                                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                                dialog.dismiss()
                            }
                            .create()
                            .show()
                    } else {
                        // User denied permission AND checked "Don't ask again"
                        AlertDialog.Builder(this)
                            .setTitle("Permissions Required")
                            .setMessage("Required permissions have been permanently denied. Please enable them in Settings to use all app features.")
                            .setPositiveButton("Open Settings") { _, _ ->
                                // Open app settings
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri
                                startActivity(intent)
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                                dialog.dismiss()
                            }
                            .create()
                            .show()
                    }
                }
            }
        }
    }
}