package com.ml.quaterion.facenetdetection

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile

class DirectorySelectionActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val SHARED_PREF_DIR_URI_KEY = "shared_pref_dir_uri_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_directory_selection)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE)

        // Set up the Toolbar as the action bar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Set up the UI components
        val selectedDirectoryPathTextView: TextView = findViewById(R.id.selected_directory_path)
        val selectDirectoryButton: Button = findViewById(R.id.select_directory_button)

        // Display the currently selected directory path if available
        val savedDirUriString = sharedPreferences.getString(SHARED_PREF_DIR_URI_KEY, null)
        if (savedDirUriString != null) {
            val dirUri = Uri.parse(savedDirUriString)
            selectedDirectoryPathTextView.text = "Selected Directory: ${getDirectoryName(dirUri)}"
        } else {
            selectedDirectoryPathTextView.text = "No directory selected"
        }

        selectDirectoryButton.setOnClickListener {
            launchChooseDirectoryIntent()
        }
    }

    private fun getDirectoryName(uri: Uri): String {
        val documentFile = DocumentFile.fromTreeUri(this, uri)
        return documentFile?.name ?: "Unknown directory"
    }

    private fun launchChooseDirectoryIntent() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        directoryAccessLauncher.launch(intent)
    }

    private val directoryAccessLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val dirUri = it.data?.data ?: return@registerForActivityResult

        // Save the selected directory URI in SharedPreferences
        sharedPreferences.edit().putString(SHARED_PREF_DIR_URI_KEY, dirUri.toString()).apply()

        // Update the TextView with the new directory path
        findViewById<TextView>(R.id.selected_directory_path).text = "Selected Directory: ${getDirectoryName(dirUri)}"

        // Notify RegisterFaceDataActivity about the directory change
        val resultIntent = Intent().apply {
            putExtra("dirUri", dirUri.toString())
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle back button click
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
}
