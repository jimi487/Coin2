package com.bohil.coin.main

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amazonaws.services.rekognition.model.DetectFacesResult
import com.amazonaws.services.rekognition.model.FaceMatch
import com.amazonaws.services.rekognition.model.Image
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
    var handle = ""
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
     * Creates the Bounding Box over the recognized face
     */
    fun createBox(){

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

    fun addFaceToCollection(appContext: Context){
        GlobalScope.launch{
            DBUtility.addFaceToCollection(appContext)
        }

    }

    fun searchCollection(appContext:Context, image:Image): List<FaceMatch>{
        return DBUtility.searchCollection(appContext, image)
    }

    fun retrieveName(): String{
        return DBUtility.getName()
    }

    fun retrieveInstagram(appContext: Context): String{
        runBlocking {
            val getHandleJob = GlobalScope.launch {
                handle = DBUtility.retrieveInstagram(appContext)
            }
            getHandleJob.join()
        }
        return handle
    }

    fun retrieveTwitter(appContext: Context){
        return DBUtility.retrieveTwitter(appContext)
    }
    fun retrieveSnapchat(appContext: Context){
        return DBUtility.retrieveSnapchat(appContext)

    }

    fun navigateToInstagram(appContext: Context){
        val igHandle = DBUtility.retrieveInstagram(appContext)
        val uri = Uri.parse("http://instagram.com/_u/$handle")
        val insta = Intent(Intent.ACTION_VIEW, uri)
        insta.setPackage("com.instagram.android")

        if(isIntentAvailable(insta, appContext)) appContext.startActivity(insta)
        else appContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/hvnchoj")))

    }

    private fun isIntentAvailable(intent: Intent, appContext: Context): Boolean{
        return appContext.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size > 0
    }

}
