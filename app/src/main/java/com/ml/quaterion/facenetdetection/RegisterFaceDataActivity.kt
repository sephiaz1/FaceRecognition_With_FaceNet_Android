package com.ml.quaterion.facenetdetection

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
class RegisterFaceDataActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var folderNameEditText: EditText
    private var selectedImages: MutableList<Uri> = mutableListOf()
    private var rootUri: Uri? = null
    private lateinit var currentPhotoPath: String
    private lateinit var sharedPreferences: SharedPreferences
    private val SHARED_PREF_DIR_URI_KEY = "shared_pref_dir_uri_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_face_data)

        sharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE)

        listView = findViewById(R.id.folder_list_view)
        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Load the default directory
        loadDefaultDirectory()

        val registerFaceButton: FloatingActionButton = findViewById(R.id.registerFaceFab)
        registerFaceButton.setOnClickListener { showCreateFolderDialog() }
    }

    private fun loadDefaultDirectory() {
        val savedDirUriString = sharedPreferences.getString(SHARED_PREF_DIR_URI_KEY, null)
        if (savedDirUriString != null) {
            rootUri = Uri.parse(savedDirUriString)
            rootUri?.let { displayDirectories(it) }
        } else {
            // Prompt user to select a directory if not set
            launchChooseDirectoryIntent()
        }
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
                sharedPreferences.edit().putString(SHARED_PREF_DIR_URI_KEY, dirUri.toString()).apply()
                displayDirectories(dirUri)
            }
        }

    private fun displayDirectories(dirUri: Uri) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            dirUri,
            DocumentsContract.getTreeDocumentId(dirUri)
        )

        val tree = DocumentFile.fromTreeUri(this, childrenUri)
        val folderList = tree?.listFiles()?.filter { it.isDirectory } ?: emptyList()

        val adapter = FolderAdapter(this, folderList) { folder ->
            confirmAndDeleteFolder(folder)
        }
        listView.adapter = adapter
    }

    private lateinit var imageAdapter: ImageAdapter

    private fun showCreateFolderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_folder, null)
        folderNameEditText = dialogView.findViewById(R.id.folderNameEditText)
        val addPhotosButton: Button = dialogView.findViewById(R.id.addPhotosButton)
        val takePhotoButton: Button = dialogView.findViewById(R.id.takePhotoButton)
        val gridView: GridView = dialogView.findViewById(R.id.selectedPhotosGridView)

        imageAdapter = ImageAdapter(this, selectedImages)
        gridView.adapter = imageAdapter
        imageAdapter.notifyDataSetChanged()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Create Folder and Add Photos")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val folderName = folderNameEditText.text.toString().trim()
                if (folderName.isNotEmpty() && rootUri != null) {
                    createFolderAndSavePhotos(folderName)

                    selectedImages.clear()
                    imageAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Please select a directory and enter a folder name.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                selectedImages.clear()
                imageAdapter.notifyDataSetChanged()
            }
            .create()

        dialog.show()

        addPhotosButton.setOnClickListener {
            launchImagePicker()
        }

        takePhotoButton.setOnClickListener {
            launchCamera()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.biru_tua))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.biru_tua))
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "image/*"
        startActivityForResult(intent, 1)
    }

    private fun launchCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            null
        }
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.ml.quaterion.facenetdetection.fileprovider",
                it
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(intent, 2)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(null)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                1 -> { // Image Picker
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
                        imageAdapter.notifyDataSetChanged()
                    }
                }
                2 -> {
                    val photoUri = Uri.fromFile(File(currentPhotoPath))
                    selectedImages.add(photoUri)
                    imageAdapter.notifyDataSetChanged()
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
            displayDirectories(parentUri)
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

    private fun confirmAndDeleteFolder(folder: DocumentFile) {
        AlertDialog.Builder(this)
            .setTitle("Delete Folder")
            .setMessage("Are you sure you want to delete this folder?")
            .setPositiveButton("Yes") { _, _ ->
                if (folder.delete()) {
                    Toast.makeText(this, "Folder deleted", Toast.LENGTH_SHORT).show()
                    rootUri?.let { displayDirectories(it) }
                } else {
                    Toast.makeText(this, "Failed to delete folder", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private class FolderAdapter(
        context: Context,
        private val folders: List<DocumentFile>,
        private val onDeleteClick: (DocumentFile) -> Unit
    ) : ArrayAdapter<DocumentFile>(context, 0, folders) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.item_folder, parent, false
            )
            val folderNameTextView: TextView = view.findViewById(R.id.folder_name)
            val deleteButton: Button = view.findViewById(R.id.deleteButton)

            val folder = getItem(position)
            folderNameTextView.text = folder?.name

            deleteButton.setOnClickListener {
                folder?.let { onDeleteClick(it) }
            }

            return view
        }
    }

    private class ImageAdapter(
        private val context: Context,
        private val imageUris: MutableList<Uri>
    ) : BaseAdapter() {

        override fun getCount(): Int = imageUris.size

        override fun getItem(position: Int): Any = imageUris[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val imageView = convertView ?: ImageView(context)
            val uri = imageUris[position]

            (imageView as ImageView).apply {
                layoutParams = ViewGroup.LayoutParams(200, 200)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(4, 4, 4, 4)
                setImageURI(uri)
            }

            return imageView
        }
    }


}
