
package com.ml.quaterion.facenetdetection

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.method.ScrollingMovementMethod
import android.util.Size
import android.view.View
import android.view.WindowInsets
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.ml.quaterion.facenetdetection.databinding.ActivityMainBinding
import com.ml.quaterion.facenetdetection.model.FaceNetModel
import com.ml.quaterion.facenetdetection.model.Models
import java.io.*
import java.util.concurrent.Executors
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices


class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val SHARED_PREF_DIR_URI_KEY = "shared_pref_dir_uri_key"
    private val DIRECTORY_SELECTION_REQUEST_CODE = 100
    private var isSerializedDataStored = false

    // Serialized data will be stored ( in app's private storage ) with this filename.
    private val SERIALIZED_DATA_FILENAME = "image_data"

    // Shared Pref key to check if the data was stored.
    private val SHARED_PREF_IS_DATA_STORED_KEY = "is_data_stored"

    private lateinit var activityMainBinding : ActivityMainBinding
    private lateinit var previewView : PreviewView
    private lateinit var frameAnalyser  : FrameAnalyser
    private lateinit var faceNetModel : FaceNetModel
    private lateinit var fileReader : FileReader
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentBranch: String? = null

            // <----------------------- User controls --------------------------->
    private lateinit var mainActivity: MainActivity;
    // Use the device's GPU to perform faster computations.
    // Refer https://www.tensorflow.org/lite/performance/gpu
    private val useGpu = true

    // Use XNNPack to accelerate inference.
    // Refer https://blog.tensorflow.org/2020/07/accelerating-tensorflow-lite-xnnpack-integration.html
    private val useXNNPack = true

    // You may the change the models here.
    // Use the model configs in Models.kt
    // Default is Models.FACENET ; Quantized models are faster
    private val modelInfo = Models.FACENET

    // Camera Facing
    private val cameraFacing = CameraSelector.LENS_FACING_FRONT

    // <---------------------------------------------------------------->


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        lateinit var logTextView : TextView

        fun setMessage( message : String ) {
            logTextView.text = message
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isRegistIn = intent.getBooleanExtra("isRegistIn", true)
        mainActivity=this
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d("Location", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                    determineBranch(location)
                }
            }
        }

        // Request location permission if not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            getCurrentLocation()
        }

        // Remove the status bar to have a full screen experience
        // See this answer on SO -> https://stackoverflow.com/a/68152688/10878733
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!
                .hide( WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
        else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        activityMainBinding = ActivityMainBinding.inflate( layoutInflater )
        setContentView( activityMainBinding.root )

        previewView = activityMainBinding.previewView
        logTextView = activityMainBinding.logTextview
        logTextView.movementMethod = ScrollingMovementMethod()
        // Necessary to keep the Overlay above the PreviewView so that the boxes are visible.
        val boundingBoxOverlay = activityMainBinding.bboxOverlay
        boundingBoxOverlay.cameraFacing = cameraFacing
        boundingBoxOverlay.setWillNotDraw( false )
        boundingBoxOverlay.setZOrderOnTop( true )

        faceNetModel = FaceNetModel( this , modelInfo , useGpu , useXNNPack )
        frameAnalyser = FrameAnalyser( this , boundingBoxOverlay , faceNetModel, isRegistIn, currentBranch)
        fileReader = FileReader( faceNetModel )


        // We'll only require the CAMERA permission from the user.
        // For scoped storage, particularly for accessing documents, we won't require WRITE_EXTERNAL_STORAGE or
        // READ_EXTERNAL_STORAGE permissions. See https://developer.android.com/training/data-storage
        if ( ActivityCompat.checkSelfPermission( this , Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED ) {
            requestCameraPermission()
        }
        else {
            startCameraPreview()
        }
        sharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE)

        // Check if the directory URI is already stored
        val savedDirUriString = sharedPreferences.getString(SHARED_PREF_DIR_URI_KEY, null)
        if (savedDirUriString == null) {
            // No directory selected before, start DirectorySelectionActivity
            val intent = Intent(this, DirectorySelectionActivity::class.java)
            startActivityForResult(intent, DIRECTORY_SELECTION_REQUEST_CODE)
        } else {
            // Directory is already selected, rescan it
            val dirUri = Uri.parse(savedDirUriString)
            rescanDirectory(dirUri)
        }

    }

    // ---------------------------------------------- //
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DIRECTORY_SELECTION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val dirUriString = data?.getStringExtra("dirUri") ?: return
            val dirUri = Uri.parse(dirUriString)
            sharedPreferences.edit().putString(SHARED_PREF_DIR_URI_KEY, dirUriString).apply()
            rescanDirectory(dirUri)
        }
    }

    // Attach the camera stream to the PreviewView.
    private fun startCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance( this )
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider) },
            ContextCompat.getMainExecutor(this) )
    }

    private fun bindPreview(cameraProvider : ProcessCameraProvider) {
        val preview : Preview = Preview.Builder().build()
        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing( cameraFacing )
            .build()
        preview.setSurfaceProvider( previewView.surfaceProvider )
        val imageFrameAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size( 480, 640 ) )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
        imageFrameAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), frameAnalyser )
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview , imageFrameAnalysis  )
    }

    // We let the system handle the requestCode. This doesn't require onRequestPermissionsResult and
    // hence makes the code cleaner.
    // See the official docs -> https://developer.android.com/training/permissions/requesting#request-permission
    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch( Manifest.permission.CAMERA )
    }

    private val cameraPermissionLauncher = registerForActivityResult( ActivityResultContracts.RequestPermission() ) {
        isGranted ->
        if ( isGranted ) {
            startCameraPreview()
        }
        else {
            val alertDialog = AlertDialog.Builder( this ).apply {
                setTitle( "Camera Permission")
                setMessage( "The app couldn't function without the camera permission." )
                setCancelable( false )
                setPositiveButton( "ALLOW" ) { dialog, which ->
                    dialog.dismiss()
                    requestCameraPermission()
                }
                setNegativeButton( "CLOSE" ) { dialog, which ->
                    dialog.dismiss()
                    finish()
                }
                create()
            }
            alertDialog.show()
        }

    }


    // ---------------------------------------------- //

    private fun launchChooseDirectoryIntent() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        directoryAccessLauncher.launch(intent)
    }


    // Read the contents of the select directory here.
    // The system handles the request code here as well.
    // See this SO question -> https://stackoverflow.com/questions/47941357/how-to-access-files-in-a-directory-given-a-content-uri
    private val directoryAccessLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val dirUri = it.data?.data ?: return@registerForActivityResult

        // Save the selected directory URI in SharedPreferences
        sharedPreferences.edit().putString(SHARED_PREF_DIR_URI_KEY, dirUri.toString()).apply()

        // Rescan the selected directory
        rescanDirectory(dirUri)
    }

    private fun rescanDirectory(dirUri: Uri) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            dirUri,
            DocumentsContract.getTreeDocumentId(dirUri)
        )
        val tree = DocumentFile.fromTreeUri(this, childrenUri)
        val images = ArrayList<Pair<String, Bitmap>>()
        var errorFound = false

        if (tree!!.listFiles().isNotEmpty()) {
            for (doc in tree.listFiles()) {
                if (doc.isDirectory && !errorFound) {
                    val name = doc.name!!
                    for (imageDocFile in doc.listFiles()) {
                        try {
                            images.add(Pair(name, getFixedBitmap(imageDocFile.uri)))
                        } catch (e: Exception) {
                            errorFound = true
                            Logger.log("Could not parse an image in $name directory. Make sure that the file structure is " +
                                    "as described in the README of the project and then restart the app.")
                            break
                        }
                    }
                    Logger.log("Found ${doc.listFiles().size} images in $name directory")
                } else {
                    errorFound = true
                    Logger.log("The selected folder should contain only directories. Make sure that the file structure is " +
                            "as described in the README of the project and then restart the app.")
                }
            }
        } else {
            errorFound = true
            Logger.log("The selected folder doesn't contain any directories. Make sure that the file structure is " +
                    "as described in the README of the project and then restart the app.")
        }

        if (!errorFound) {
            fileReader.run(images, fileReaderCallback)
            Logger.log("Detecting faces in ${images.size} images ...")
        } else {
            showErrorDialog()
        }
    }

    private fun showErrorDialog() {
        val alertDialog = AlertDialog.Builder(this).apply {
            setTitle("Error while parsing directory")
            setMessage("There were some errors while parsing the directory. Please see the log below. Make sure that the file structure is as described in the README of the project and then tap RESELECT")
            setCancelable(false)
            setPositiveButton("RESELECT") { dialog, which ->
                dialog.dismiss()
                launchChooseDirectoryIntent()
            }
            setNegativeButton("CANCEL") { dialog, which ->
                dialog.dismiss()
                finish()
            }
            create()
        }
        alertDialog.show()
    }

    // Get the image as a Bitmap from given Uri and fix the rotation using the Exif interface
    // Source -> https://stackoverflow.com/questions/14066038/why-does-an-image-captured-using-camera-intent-gets-rotated-on-some-devices-on-a
    private fun getFixedBitmap( imageFileUri : Uri ) : Bitmap {
        var imageBitmap = BitmapUtils.getBitmapFromUri( contentResolver , imageFileUri )
        val exifInterface = ExifInterface( contentResolver.openInputStream( imageFileUri )!! )
        imageBitmap =
            when (exifInterface.getAttributeInt( ExifInterface.TAG_ORIENTATION ,
                ExifInterface.ORIENTATION_UNDEFINED )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> BitmapUtils.rotateBitmap( imageBitmap , 90f )
                ExifInterface.ORIENTATION_ROTATE_180 -> BitmapUtils.rotateBitmap( imageBitmap , 180f )
                ExifInterface.ORIENTATION_ROTATE_270 -> BitmapUtils.rotateBitmap( imageBitmap , 270f )
                else -> imageBitmap
            }
        return imageBitmap
    }


    // ---------------------------------------------- //


    private val fileReaderCallback = object : FileReader.ProcessCallback {
        override fun onProcessCompleted(data: ArrayList<Pair<String, FloatArray>>, numImagesWithNoFaces: Int) {
            frameAnalyser.faceList = data
            saveSerializedImageData( data )
            Logger.log( "Images parsed. Found $numImagesWithNoFaces images with no faces." )
        }
    }


    private fun saveSerializedImageData(data : ArrayList<Pair<String,FloatArray>> ) {
        val serializedDataFile = File( filesDir , SERIALIZED_DATA_FILENAME )
        ObjectOutputStream( FileOutputStream( serializedDataFile )  ).apply {
            writeObject( data )
            flush()
            close()
        }
        sharedPreferences.edit().putBoolean( SHARED_PREF_IS_DATA_STORED_KEY , true ).apply()
    }


    private fun loadSerializedImageData() : ArrayList<Pair<String,FloatArray>> {
        val serializedDataFile = File( filesDir , SERIALIZED_DATA_FILENAME )
        val objectInputStream = ObjectInputStream( FileInputStream( serializedDataFile ) )
        val data = objectInputStream.readObject() as ArrayList<Pair<String,FloatArray>>
        objectInputStream.close()
        return data
    }
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    private fun getCurrentLocation() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun determineBranch(location: Location) {
        Log.d("Location", "Determining branch for Lat: ${location.latitude}, Lon: ${location.longitude}")

        val branches = mapOf(
            "Japfa Tower" to Pair(Pair(-7.273, -7.271), Pair(112.742, 112.743)),
            "Branch B" to Pair(Pair(19.075, 19.078), Pair(72.877, 72.879))
        )

        for ((branch, coords) in branches) {
            val (latRange, lonRange) = coords
            if (location.latitude in latRange.first..latRange.second &&
                location.longitude in lonRange.first..lonRange.second) {
                currentBranch = branch
                frameAnalyser.updateCurrentBranch(currentBranch)  // Update the FrameAnalyser instance
                Logger.log("User is at $currentBranch")
//                Toast.makeText(
//                    this,
//                    "Current Location: Lat: ${location.latitude}, Lon: ${location.longitude}, Branch: $currentBranch",
//                    Toast.LENGTH_LONG
//                ).show()

                break
            }
        }
        if (currentBranch == null) {
            Logger.log("User is not at a known branch")
        }

    }

}
