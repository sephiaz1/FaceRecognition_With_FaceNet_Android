package com.ml.quaterion.facenetdetection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.io.OutputStream

class RegisterFaceDataActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var folderNameEditText: EditText
    private var selectedImages: MutableList<Uri> = mutableListOf()
    private var rootUri: Uri? = null

    private val REQUEST_CODE_SELECT_PHOTOS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_face_data)

        listView = findViewById(R.id.folder_list_view)
        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed() // Call onBackPressed() when the custom button is clicked
        }

        // Launch directory chooser
        launchChooseDirectoryIntent()

        val registerFaceButton: Button = findViewById(R.id.registerFaceButton)
        registerFaceButton.setOnClickListener { showCreateFolderDialog() }
    }

    private fun launchChooseDirectoryIntent() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        directoryAccessLauncher.launch(intent)
    }

    private val directoryAccessLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val dirUri: Uri? = result.data?.data
            if (dirUri != null) {
                rootUri = dirUri
                displayDirectories(dirUri)
            }
        }

    private fun displayDirectories(dirUri: Uri) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            dirUri,
            DocumentsContract.getTreeDocumentId(dirUri)
        )

        val tree = DocumentFile.fromTreeUri(this, childrenUri)
        val folderNames = mutableListOf<String>()

        tree?.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                folderNames.add(file.name ?: "Unknown")
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, folderNames)
        listView.adapter = adapter
    }

    private fun showCreateFolderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_folder, null)
        folderNameEditText = dialogView.findViewById(R.id.folderNameEditText)
        val addPhotosButton: Button = dialogView.findViewById(R.id.addPhotosButton)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Create Folder and Add Photos")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val folderName = folderNameEditText.text.toString().trim()
                if (folderName.isNotEmpty() && rootUri != null) {
                    createFolderAndSavePhotos(folderName)
                } else {
                    Toast.makeText(this, "Please select a directory and enter a folder name.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Handle the "Add Photos" button click
        addPhotosButton.setOnClickListener { launchImagePicker() }
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_SELECT_PHOTOS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_PHOTOS && resultCode == RESULT_OK) {
            data?.let { intent ->
                val clipData = intent.clipData
                selectedImages.clear()
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        selectedImages.add(clipData.getItemAt(i).uri)
                    }
                } else {
                    intent.data?.let { selectedImages.add(it) }
                }
            }
        }
    }

    private fun createFolderAndSavePhotos(folderName: String) {
        val parentUri = rootUri ?: return
        val newFolderUri = createFolder(parentUri, folderName)

        if (newFolderUri != null) {
            savePhotosToFolder(newFolderUri)
            Toast.makeText(this, "Folder created and photos added.", Toast.LENGTH_SHORT).show()
            displayDirectories(parentUri) // Refresh the ListView
        } else {
            Toast.makeText(this, "Failed to create folder.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createFolder(parentUri: Uri, folderName: String): Uri? {
        val documentFile = DocumentFile.fromTreeUri(this, parentUri)
        return documentFile?.createDirectory(folderName)?.uri
    }

    private fun savePhotosToFolder(folderUri: Uri) {
        for (uri in selectedImages) {
            try {
                val imageName = getFileName(uri) ?: "image_${System.currentTimeMillis()}.jpg"
                val newFileUri = DocumentsContract.createDocument(
                    contentResolver, folderUri, "image/jpeg", imageName
                )
                contentResolver.openOutputStream(newFileUri!!)?.use { outputStream ->
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: Exception) {
                Log.e("RegisterFaceDataActivity", "Error saving image: $e")
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return it.getString(nameIndex)
                }
            }
        }
        return null
    }
}
