package com.gulshan.assignment.util

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.graphics.Bitmap
import android.os.Environment
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import java.io.File
import java.io.FileOutputStream

class Helper {
    companion object {
        fun askLocation(context: Context?, activity: Activity?, activityCode: Int) {
            val locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

            val settingsBuilder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
            settingsBuilder.setAlwaysShow(true)
            val result: Task<LocationSettingsResponse> =
                LocationServices.getSettingsClient(context!!)
                    .checkLocationSettings(settingsBuilder.build())
            result.addOnCompleteListener { task: Task<LocationSettingsResponse?> ->
                try {
                    val response: LocationSettingsResponse? =
                        task.getResult(ApiException::class.java)
                    // location is already enabled
                } catch (ex: ApiException) {
                    when (ex.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            val resolvableApiException =
                                ex as ResolvableApiException
                            resolvableApiException
                                .startResolutionForResult(
                                    activity!!,
                                    activityCode
                                )
                        } catch (e: SendIntentException) {
                            e.printStackTrace()
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {}
                    }
                }
            }
        }

        fun checkIsLocationEnabled(contentResolver: ContentResolver?): Boolean {
            return try {
                val locationMode =
                    Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
                locationMode != Settings.Secure.LOCATION_MODE_OFF
            } catch (e: SettingNotFoundException) {
                e.printStackTrace()
                false
            }
        }



    }
}