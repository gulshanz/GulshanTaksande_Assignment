package com.gulshan.assignment.ui

import android.content.ClipData
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel : ViewModel() {
    val toastMessageLive: MutableLiveData<String> = MutableLiveData()
    val progressLive: MutableLiveData<Boolean> = MutableLiveData()
    val dismissDialog: MutableLiveData<Boolean> = MutableLiveData()
    val folderName = "AssignmentGulshan"

    @Suppress("DEPRECATION")
    fun convertAndSaveToFile(
        uri: Uri? = null,
        bm: Bitmap? = null,
        contentResolver: ContentResolver
    ) {
        lateinit var bitmap: Bitmap
        progressLive.postValue(true)
        if (uri != null) {
            bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } else {
            if (bm != null) {
                bitmap = bm
            }
        }

        val file = saveBitmapToFile(bitmap)
        if (file == null) {
            toastMessageLive.postValue("Something went wrong while saving file")
        }
        toastMessageLive.postValue("Image saved successfully to $folderName folder")
        dismissDialog.postValue(true)
        progressLive.postValue(false)

    }

    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        //create a file to write bitmap data
        val file = createFile()
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }

    fun createFile(): File? {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss.SSS", Locale.ENGLISH).format(Date())
        val fileName = "$timeStamp.jpg"
        val rootDirectory = Environment.getExternalStorageDirectory().absolutePath
        val filesDirectory = File(rootDirectory, folderName)

        if (!filesDirectory.exists()) {
            val status = filesDirectory.mkdirs()
            if (!status) {
                return null
            }
        }

        val file = File(filesDirectory.absolutePath, fileName)
        if (file.exists()) {
            file.delete()
        }
        return file
    }

    fun saveMultipleImages(clipData: ClipData?, contentResolver: ContentResolver) {
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                convertAndSaveToFile(
                    uri = clipData.getItemAt(i).uri,
                    contentResolver = contentResolver
                )
            }
        }
        progressLive.postValue(false)
    }


}