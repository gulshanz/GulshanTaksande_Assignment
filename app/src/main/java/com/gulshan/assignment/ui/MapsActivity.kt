@file:Suppress("DEPRECATION")

package com.gulshan.assignment.ui

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat.isLocationEnabled
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.gulshan.assignment.R
import com.gulshan.assignment.databinding.ActivityMapsBinding
import com.gulshan.assignment.util.Helper.Companion.askLocation
import com.gulshan.assignment.util.ViewModelFactory


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var currentLocation: Location? = null
    private var isInitialLocated: Boolean = false
    private val LOCATION_SETTINGS_REQUEST = 99
    private val STORAGE_PERMISSION = 101
    private val REQUEST_CODE = 88
    val REQUEST_IMAGE_CAPTURE = 1
    val CHOOSER_REQUEST_CODE = 102
    var locationManager: LocationManager? = null
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        initViewModel()
        initUiComponents()
    }

    override fun onResume() {
        super.onResume()
        makeToast("Mapview will be blank as no API_KEY is Provided")
    }

    private fun initViewModel() {
        val viewModelFactory = ViewModelFactory()
        viewModel =
            ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
        viewModel.progressLive.observe(this) {
            showProgressBar(it)
        }
        viewModel.toastMessageLive.observe(this) {
            makeSnackBar(it)
        }
    }

    private fun initUiComponents() {
        binding.buttonPhoto.setOnClickListener {
            selectAndSavePhoto()
        }

        binding.buttonSignature.setOnClickListener {
            selectAndSaveSignature()
        }
    }


    private fun selectAndSaveSignature() {
        openSignatureDialog()
    }

    private fun selectAndSavePhoto() {
        val allowed = checkStoragePermissions()
        if (!allowed) {
            requestStoragePermissions()
            return
        }
        val galleryIntent = Intent()
        galleryIntent.type = "image/*"
        galleryIntent.action = Intent.ACTION_PICK
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        // should put limit on selection, as everything will be processed in UI thread!
        // can't put limit while selection from gallery directly need to use custom tool for
        // fetching and selecting images

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val chooser = Intent.createChooser(galleryIntent, "Gallery")

        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent))
        startActivityForResult(chooser, CHOOSER_REQUEST_CODE)
    }

    private fun requestStoragePermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                READ_EXTERNAL_STORAGE,
                WRITE_EXTERNAL_STORAGE
            ), STORAGE_PERMISSION
        )
    }

    private fun checkStoragePermissions(): Boolean {
        val read = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
        val write = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        val result =
            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        return result
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // map will be blank as we don't have API KEY, nothing related to map will work

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

    }

    override fun onLocationChanged(p0: Location) {
        currentLocation = p0
        val latLng = LatLng(p0.latitude, p0.longitude)
        if (!isInitialLocated) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
            isInitialLocated = true
        }
    }

    private fun initMapComponents() {
        val arePresent = checkAndRequestLocationPermission()
        if (!arePresent)
            return
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        makeToast("Fetching current location...")

    }


    private fun checkAndRequestLocationPermission(): Boolean {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isLocationEnabled: Boolean = isLocationEnabled(locationManager!!)
        if (!isLocationEnabled) {
            askLocation(this, this, LOCATION_SETTINGS_REQUEST)
            return false
        }
        val requiredPermission = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (ContextCompat.checkSelfPermission(this, requiredPermission[0]) !=
            PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, requiredPermission[1]) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, requiredPermission, REQUEST_CODE)
            return false
        }
        return true
    }

//    private fun isLocationEnabled(): Boolean {
//        return checkIsLocationEnabled(contentResolver)
//    }

    override fun onProviderDisabled(provider: String) {
        makeToast("Location disabled, please turn on location!")
    }

    private fun makeToast(string: String) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
    }

    fun makeSnackBar(string: String){
        Snackbar.make(findViewById(android.R.id.content),string,Snackbar.LENGTH_LONG).show()
    }

    override fun onProviderEnabled(provider: String) {
//        super.onProviderEnabled(provider)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            for (i in permissions.indices) {
                val permissionResult = grantResults[i]
                val permission = permissions[i]
                if (permissionResult != PackageManager.PERMISSION_GRANTED) {
                    if (!shouldShowRequestPermissionRationale(permission!!)) {
                        // permanently denied
                        makeToast("Can't proceed further please enable permissions ")
                        return
                    }
                    makeToast("Can't proceed further please enable permissions")
                    finish()
                    return
                }
            }
            initMapComponents()
        } else if (requestCode == STORAGE_PERMISSION) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    makeToast("Please enable permissions to proceed further")
                }
            }
            selectAndSavePhoto()
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_SETTINGS_REQUEST) {
            if (resultCode == RESULT_OK) {
                initMapComponents()
            } else {
                makeToast("Please enable to location and restart the app")
            }
        } else if (requestCode == CHOOSER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    makeToast("Something went wrong")
                    return
                }
                // image from camera
                try {
                    val bm = data.extras?.get("data") as Bitmap
                    viewModel.convertAndSaveToFile(bm = bm, contentResolver = contentResolver)
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // multiple image selection
                if (data.data == null) {
                    viewModel.saveMultipleImages(data.clipData, contentResolver)
                } else {
                    // single image
                    val uri = data.data
                    viewModel.convertAndSaveToFile(uri = uri, contentResolver = contentResolver)
                }
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            viewModel.convertAndSaveToFile(bm = imageBitmap, contentResolver = contentResolver)
        }
    }

    private fun showProgressBar(b: Boolean) {
        if (b) {
            binding.progress.visibility = View.VISIBLE
        } else {
            binding.progress.visibility = View.GONE
        }
    }

    private fun openSignatureDialog() {
        val dialog = SignatureDialog()
        dialog.isCancelable = false
        dialog.show(supportFragmentManager, "tag")
    }


}