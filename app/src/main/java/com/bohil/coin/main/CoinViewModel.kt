package com.bohil.coin.main

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSource
import com.amazonaws.services.rekognition.model.DetectFacesResult
import com.amazonaws.services.rekognition.model.FaceMatch
import com.amazonaws.services.rekognition.model.Image
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult
import com.amazonaws.util.IOUtils
import com.bohil.coin.DBUtility
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.system.exitProcess

private const val TAG = "CoinViewModel"

/**
 * ViewModel for the Coin Fragment
 * Holds and initiates the configurations for the Kinesis Stream
 */
class CoinViewModel : ViewModel() {
    //The Direction of the Camera. Back Camera: 0 Front Camera: 1
    private var cameraDirection = 1

    //TODO Properly implement
    /**
     * Changes the direction of the camera
     */
    fun changeCameraDirection(appContext: Context): AndroidCameraMediaSource{
        cameraDirection = if(cameraDirection == 0) 1 else 0
        return DBUtility.createMediaSource(appContext, cameraDirection)
    }

    init{

    }

    /**
     * Converts the bitmap to a ByteBuffer Rekognition can use
     */
    fun convertToImage(context: Context, screenImage: Bitmap): ByteBuffer{
        var sourceImageBytes: ByteBuffer? = null

        // Converting to Byte Array
        val bostream = ByteArrayOutputStream()
        screenImage.compress(Bitmap.CompressFormat.JPEG, 100, bostream)
        val bitmapdata = bostream.toByteArray()

        // Converting to File
        val f = File(context.cacheDir, "input")
        try {
            f.createNewFile()
        } catch (e: Exception) {
            e.fillInStackTrace()
        }
        try {
            val fostream = FileOutputStream(f)
            fostream.write(bitmapdata)
            fostream.flush()
            fostream.close()
        } catch (e: Exception) {
            e.fillInStackTrace()
        }

        //Creates source Image
        try {
            FileInputStream(f).use { inputStream ->
                sourceImageBytes =
                    ByteBuffer.wrap(IOUtils.toByteArray(inputStream))
            }
        } catch (e: Exception) {
            println("Failed to load source screenImage $f")
            exitProcess(1)
        }

        return sourceImageBytes!!
    }

    /**
     * Detects the faces present in a Rekognition Image
     */
    fun detectFaces(image: Image): DetectFacesResult?{
        var results:DetectFacesResult? = null
        runBlocking {
            val getFacesDetectedJob = GlobalScope.launch {
                results =  DBUtility.detectFaces(image)
            }
            // Wait for job to complete
            getFacesDetectedJob.join()
        }
        return results
    }

    fun clearCollection(appContext: Context){
        GlobalScope.launch {
            DBUtility.deleteFaceFromCollection(appContext)
        }
    }

    fun listCollection(appContext:Context){
        GlobalScope.launch {
            DBUtility.listCollection(appContext)
        }
    }

    fun deleteCollection(appContext: Context){
        GlobalScope.launch {
            DBUtility.deleteCollection(appContext)
        }
    }

   /*fun addFaceToCollection(appContext: Context){
        GlobalScope.launch{
            DBUtility.addFaceToCollection(appContext)
        }

    }*/

    fun searchCollection(appContext:Context, image:Image): SearchFacesByImageResult{

        var results:SearchFacesByImageResult? = null
        runBlocking {
            val getFacesDetectedJob = GlobalScope.launch {
                results =  DBUtility.searchCollection(appContext,image)
            }
            // Wait for job to complete
            getFacesDetectedJob.join()
        }
        return results!!
    }


    fun navigateToInstagram(appContext: Context, handle : String){
        val uri = Uri.parse("http://instagram.com/_u/$handle")
        val insta = Intent(Intent.ACTION_VIEW, uri)
        insta.setPackage("com.instagram.android")

        if(isIntentAvailable(insta, appContext)) appContext.startActivity(insta)
        else appContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/${handle}")))

    }

    private fun isIntentAvailable(intent: Intent, appContext: Context): Boolean{
        return appContext.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size > 0
    }

}
