package com.bohil.coin

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.util.Size
import com.amazonaws.kinesisvideo.client.KinesisVideoClient
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException
import com.amazonaws.kinesisvideo.producer.StreamInfo
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.SignOutOptions
import com.amazonaws.mobile.client.UserState.SIGNED_IN
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobileconnectors.kinesisvideo.client.KinesisVideoAndroidClientFactory
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSource
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSourceConfiguration
import com.amazonaws.mobileconnectors.kinesisvideo.util.CameraUtils.getCameras
import com.amazonaws.mobileconnectors.kinesisvideo.util.CameraUtils.getSupportedResolutions
import com.amazonaws.mobileconnectors.kinesisvideo.util.VideoEncoderUtils.getSupportedMimeTypes
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.rekognition.AmazonRekognition
import com.amazonaws.services.rekognition.AmazonRekognitionClient
import com.amazonaws.services.rekognition.model.*
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.util.IOUtils
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.ResultListener
import com.amplifyframework.storage.result.StorageUploadFileResult
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.bohil.coin.settings.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * Class containing utility methods for Firebase and AWS
 */

object DBUtility {
    private val TAG = DBUtility::class.java.simpleName
    // Network INSTANCES

    val AWSInstance: AWSMobileClient = AWSMobileClient.getInstance()
    val FirebaseInstance = FirebaseFirestore.getInstance()
    var rekognitionClient: AmazonRekognition = AmazonRekognitionClient(AWSInstance)
    lateinit var kinesisVideoClient: KinesisVideoClient

    //Network FIELDS
    private val region: Regions = Regions.US_EAST_2
    private const val FRAMERATE_20 = 20
    private const val BITRATE_384_KBPS = 384 * 1024
    private const val RETENTION_PERIOD_48_HOURS = 2 * 24
    private val RESOLUTION_320x240 = Size(320, 240)

    //// AWS Initialization and Destruction

    /*
     * Initializes AWS Services at the start of the application
     */
    fun initAWS(appContext: Context) {
        // Initializing the AWS Amplify instance
        AWSInstance.initialize(appContext, object : Callback<UserStateDetails> {
            override fun onResult(userStateDetails: UserStateDetails) {
                try {
                    Amplify.addPlugin(AWSS3StoragePlugin())
                    //TODO Change Amplify storage to use Transfer Service
                    appContext.startService(
                        Intent(
                            appContext,
                            TransferService::class.java
                        )
                    )
                    Amplify.configure(appContext)
                    when (userStateDetails.userState) {
                        SIGNED_IN -> AWSMobileClient.getInstance().signOut()
                    }
                } catch (e: java.lang.Exception) {
                    Log.e("ApiQuickstart", e.message)
                }
            }

            override fun onError(e: Exception?) {
                Log.e("INIT", "Initialization error.", e)
            }
        }
        )
    }

    /*
     * Signs out of the AWS Service
     */
    fun signOutAWS() {
        AWSInstance.signOut(
            SignOutOptions.builder().signOutGlobally(true).build(),
            object : Callback<Void?> {
                override fun onResult(result: Void?) {
                    Log.d("LogInActivity", "signed-out")
                }

                override fun onError(e: java.lang.Exception) {
                    Log.e("LogInActivity", "sign-out error", e)
                }
            })
    }

    //// KINESIS REQUESTS

    /**
     * Sets up the configuration for the Data sent to the Video Stream
     * @return
     */
    fun getCurrentConfiguration(appContext: Context, cameraDirection: Int): AndroidCameraMediaSourceConfiguration? {

        // 0 gives back camera, 1 gives front
        val cameras = getCameras(kinesisVideoClient)
        val resolutions = getSupportedResolutions(appContext, cameras[cameraDirection].cameraId)
        val mimeTypes = getSupportedMimeTypes()
        var select1080p = 0
        var selectHEVC = 0

        // Setting the stream resolution to 1080p
        for ((index, size) in resolutions.withIndex()) {
            if (size.width == 1920 && size.height == 1080) {
                select1080p = index
            }
        }

        // Setting the streaming mime type to hevc
        for ((index, mime) in mimeTypes.withIndex()) {
            if (mime.mimeType.toString() == "video/hevc") {
                selectHEVC = index
            }
        }

        return AndroidCameraMediaSourceConfiguration(
            AndroidCameraMediaSourceConfiguration.builder()
                .withCameraId(cameras[cameraDirection].cameraId)
                .withCameraFacing(cameras[cameraDirection].cameraFacing)
                .withIsEncoderHardwareAccelerated(
                    cameras[cameraDirection].isEndcoderHardwareAccelerated
                )
                .withCameraOrientation(-cameras[cameraDirection].cameraOrientation)
                .withEncodingMimeType(mimeTypes[selectHEVC].mimeType)
                .withHorizontalResolution(resolutions[select1080p].width)
                .withVerticalResolution(resolutions[select1080p].height)
                .withFrameRate(FRAMERATE_20)
                .withRetentionPeriodInHours(RETENTION_PERIOD_48_HOURS)
                .withEncodingBitRate(BITRATE_384_KBPS)
                .withNalAdaptationFlags(StreamInfo.NalAdaptationFlags.NAL_ADAPTATION_ANNEXB_CPD_AND_FRAME_NALS)
                .withIsAbsoluteTimecode(false)
        )
    }

    /**
     * Creates the Video Source for the Video Stream
     */
    fun createMediaSource(appContext: Context, cameraDirection: Int): AndroidCameraMediaSource {
        return kinesisVideoClient
            .createMediaSource("CoinVideoStream", getCurrentConfiguration(appContext, cameraDirection))
                as AndroidCameraMediaSource
    }

    /**
     * Creates the Kinesis Video Client
     * Rekogntion Client is initialized here
     */
    fun createKinesisVideoClient(appContext: Context) {
        // Creates the Kinesis Video Instance
        try {
            kinesisVideoClient = KinesisVideoAndroidClientFactory.createKinesisVideoClient(
                appContext,
                region,
                AWSInstance
            )
            //val credentialsProvider = CognitoCachingCredentialsProvider(
            // appContext, "us-east-2:9ebbd566-0241-4444-bae5-89f6fce31385", region)
            //rekognitionClient = AmazonRekognitionClient(credentialsProvider)

            initializeRekogntionClient()

        } catch (e: KinesisVideoException) {
            Log.e(
                TAG,
                "Failed to create Kinesis Video client",
                e
            )
        }
    }

    //// REKOGNITION REQUESTSd
    private fun initializeRekogntionClient() {
        rekognitionClient = AmazonRekognitionClient(AWSInstance.credentials)
        rekognitionClient.setEndpoint("rekognition.us-east-2.amazonaws.com")
        rekognitionClient.setRegion(Region.getRegion(Regions.US_EAST_2))
    }

    /**
     * Detects the faces in a given image using Rekognition
     */
   fun detectFaces(image: Image): DetectFacesResult? {
        val facesRequest = DetectFacesRequest().withImage(image)
        var result: DetectFacesResult? = null
        try {
            result = rekognitionClient.detectFaces(facesRequest)
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
        }

        return result
    }

    /**
     * Creates a Face Collection in the Rekognition region
     */
    fun createCollection(appContext: Context) {
        val request = CreateCollectionRequest().withCollectionId(getCollectionID(appContext))
        val collectionResult = rekognitionClient.createCollection(request)
    }

    /**
     * Deletes a specified face id from the collection
     */
    fun deleteFaceFromCollection(appContext: Context) {
        val deleteFacesRequest = DeleteFacesRequest()
            .withCollectionId(getCollectionID(appContext))
            .withFaceIds("")
        val deleteFacesResult = rekognitionClient.deleteFaces(deleteFacesRequest)
    }

    /**
     * Deletes the collection
     */
    fun deleteCollection(appContext: Context) {
        val request = DeleteCollectionRequest()
            .withCollectionId(getCollectionID(appContext))
        val deleteCollectionResult = rekognitionClient.deleteCollection(request)
    }

    /**
     * Adds the user to the face collection from their image in the S3 Collection
     * Saves the face id (externalImageId) as their email before the @
    !! All faces in a picture will be saved with the same externalImageId
     */
    fun addFaceToCollection(appContext: Context, id : String) {
        val image = retrieveImageFromS3(id)

        try {
            val indexFacesRequest = IndexFacesRequest()
                .withImage(image)
                .withQualityFilter(QualityFilter.AUTO)
                .withMaxFaces(1)
                .withCollectionId(getCollectionID(appContext))
                .withExternalImageId(id)
                .withDetectionAttributes("DEFAULT")

            val indexFacesResult = rekognitionClient.indexFaces(indexFacesRequest)
            val faceRecords = indexFacesResult.faceRecords
            for (faceRecord in faceRecords) {
                continue
            }

            val unindexedFaces = indexFacesResult.unindexedFaces
            for (unindexedFace in unindexedFaces) {
                continue
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: $e.toString()")
        }
    }

    /**
     * Lists all the faces in the face collection
     */
    fun listCollection(appContext: Context) {
        var facesResult: ListFacesResult? = null
        var paginationToken: String? = null
        do {
            if (facesResult != null) {
                paginationToken = facesResult.nextToken
            }
            val facesRequest = ListFacesRequest()
                .withCollectionId(getCollectionID(appContext))
                .withMaxResults(100)
                .withNextToken(paginationToken)

            facesResult = rekognitionClient.listFaces(facesRequest)
            val faces = facesResult.faces
            for (face in faces) {
                continue
            }
        } while (facesResult != null && facesResult.nextToken != null)

    }

    /**
     * Searches for a face in a collection
     */
    fun searchCollection(appContext: Context, image: Image): SearchFacesByImageResult {

        val searchFace = SearchFacesByImageRequest()
            .withCollectionId(getCollectionID(appContext))
            .withImage(image)
            .withFaceMatchThreshold(93f)
            .withMaxFaces(1)

        val searchFacesResult =
            rekognitionClient.searchFacesByImage(searchFace)

        return searchFacesResult
    }

    /**
     * Retrieves the users image from the S3 collection
     */
    fun retrieveImageFromS3(id : String): Image {
        val pictureName = "public/${id}.jpg"

        return Image()
            .withS3Object(
                S3Object()
                    .withBucket("coinbucket00940-coinback")
                    .withName(pictureName)
            )
    }

    fun retrieveImageFromS3Bitmap(appContext: Context, id: String): Bitmap{
        val pictureName = "public/${id}.jpg"
        var currentPhotoPath = ""
        val image = Image().withS3Object(
            S3Object()
                .withBucket("coinbucket00940-coinback")
                .withName(pictureName))

        val imageContent = AmazonS3Client(AWSInstance.credentials).getObject(image.s3Object.bucket, "public/${id}.jpg").objectContent

        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val userFile = File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }

        IOUtils.copy(imageContent, FileOutputStream(currentPhotoPath))
        val imageBitmap = BitmapFactory.decodeFile(File(currentPhotoPath).absolutePath)
        return imageBitmap
    }


    //// DATABASE REQUESTS

    /**
     * Adds the user to Firebase
     */
    suspend fun addFirebaseUser(
        user: HashMap<String, String>, collectionName: String,
        cognitoFirestore: String,
        userPicture: Pair<File, Uri>,
        appContext: Context
    ) {
        FirebaseInstance.collection(collectionName)
            .add(user)
            .addOnSuccessListener {
                //Update user's FireStore ID in Cognito
                try {
                    Log.d(TAG, "Updating the user Firestore Key")
                    GlobalScope.launch {
                        updateCognito(cognitoFirestore, it.id, appContext)
                        uploadFile(userPicture, it.id, appContext)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, e.toString())
                }
            }

            .addOnFailureListener {
                Log.w(TAG, "Error adding document", it)
            }
    }


    /**
     * Updates the user's Firestore ID in Cognito and adds to Face Collection
     */
    private suspend fun updateCognito(attribute: String, id: String, appContext: Context) = withContext(Dispatchers.IO) {
        try {
            UserManager.setUserId(appContext, id)
            AWSInstance.updateUserAttributes(hashMapOf(attribute to id))
        } catch (e: Exception) {
            Log.e(TAG, "Unable to add key", e)
        }
        Log.d(TAG, "User key finished adding")

    }

    /**
     * Uploads the file to the Amazon s3 collection
     */
    fun uploadFile(userFile: Pair<File, Uri>, firebaseID: String, appContext: Context) {
        Amplify.Storage.uploadFile(
            "$firebaseID.jpg",
            userFile.first.absolutePath,
            object : ResultListener<StorageUploadFileResult> {
                override fun onResult(result: StorageUploadFileResult?) {
                    Log.d(TAG, "File added successfully")
                    addFaceToCollection(appContext, firebaseID)
                }

                override fun onError(error: Throwable?) {
                    Log.d(TAG, "File not added successfully")
                }
            }
        )
    }

    //TODO Retrieve users social media handles and store them locally

    // TODO Change to Firebase name
    fun getName(): String {
        //return AWSInstance.username.substring(0, AWSInstance.username.indexOf("@"))
        return UserManager.UserID
    }

    /**
     * Converts the bitmap to a ByteBuffer Rekognition can use
     */
    fun convertToByteBuffer(context: Context, screenImage: Bitmap): ByteBuffer{
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
     * Creates a Rekognition Image using Byte Array
     */
    fun createImageFromByte(sourceImageBytes: ByteBuffer): Image{
        return Image().withBytes(sourceImageBytes)
    }

    /**
     * Retrieves the users Instagram handle from Firestore
     */
    fun retrieveInstagram(appContext: Context): String {
        return  AWSInstance.userAttributes.getValue("custom:instagramHandle")

        /*
        val document = AWSInstance.userAttributes
            .getValue(appContext.getString(R.string.cognito_firestore))

        val docRef =
            FirebaseInstance.collection(appContext.getString(R.string.firestore_table))
                .document(document)

        docRef.get()
            .addOnSuccessListener { doc ->
                Toast.makeText(appContext, "Retrieved document", Toast.LENGTH_LONG).show()
                val user = doc.data
                if (user != null) {
                    igHandle = user["igHandle"] as String
                }

            }
            .addOnFailureListener{ exception ->
                Toast.makeText(appContext, "Failed to get IG: $exception", Toast.LENGTH_LONG).show()
            }

        return igHandle*/
    }

    fun retrieveTwitter(appContext: Context) {
    }

    fun retrieveSnapchat(appContext: Context) {
    }

    private fun getUserPictureKey(appContext: Context): String{
        return "${AWSInstance.userAttributes.getValue(appContext.getString(R.string.cognito_firestore))}.jpg"
    }

    /**
     * Returns the ID of the Face Collection
     */
    private fun getCollectionID(appContext: Context): String{
        return appContext.getString(R.string.face_collection_name)
    }
}






