package com.bohil.coin.main

import android.annotation.SuppressLint
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.ScaleDrawable
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException
import com.amazonaws.mobileconnectors.kinesisvideo.client.KinesisVideoAndroidClientFactory
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSource
import com.amazonaws.services.rekognition.model.BoundingBox
import com.amazonaws.services.rekognition.model.CompareFacesRequest
import com.amazonaws.services.rekognition.model.Image
import com.bohil.coin.DBUtility
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentCoinBinding
import com.bohil.coin.settings.UserManager
import com.bohil.coin.settings.UserSettingsFragment


private lateinit var viewModel: CoinViewModel
private lateinit var binding: FragmentCoinBinding
private lateinit var mCameraMediaSource:AndroidCameraMediaSource
private lateinit var textureView:TextureView
private lateinit var surfaceView: SurfaceView
private lateinit var mHolder:SurfaceHolder
private lateinit var picture:Drawable
private lateinit var canvas:Canvas
private lateinit var image : Image
private lateinit var layout : FrameLayout
private var textViews : MutableList<TextView> = mutableListOf()
private var previewwTexture:SurfaceTexture? = null

// Variable to determine whether the user has started the stream
private var startStream = false
// Determines when the stream timer has finished
private var timeUp = true

@Suppress("DEPRECATION")
class CoinFragment : Fragment(), TextureView.SurfaceTextureListener {
    companion object {
        const val TAG = "CoinFragment"
        // List containing all the users found so far
        private val facesFoundList: MutableList<Pair<String, String>> = mutableListOf<Pair<String, String>>()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_coin, container, false)
        binding.lifecycleOwner = this

        layout = binding.mainFrame

        // Binding TextureView
        textureView = binding.coinTextureView
        textureView.surfaceTextureListener = this

        // SurfaceView
        surfaceView = binding.surfaceView
        surfaceView.setZOrderOnTop(true)
        mHolder = surfaceView.holder
        mHolder.setFormat(PixelFormat.TRANSPARENT)

        // Stream button
        binding.coinStreamButton.text = activity!!.getText(R.string.start_streaming)

        // Change Camera Icon
        //binding.changeCamera.setOnClickListener { changeCameraDirection() }

        // TextView for Users Instagram
        //binding.tempUserText.setOnClickListener { viewModel.navigateToInstagram(context!!) }

        // Setting the RecylcerView for found users
        //viewManager = LinearLayoutManager(context)
        //recyclerView.layoutManager = viewManager
        //viewAdapter = MyAdapter(myDataset)


        // Temporarily changing the thread mode to allow network requests on main
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.simple_nav, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
        image = DBUtility.retrieveImageFromS3("test")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_settings -> {
                pauseStreaming()
                findNavController().navigate(CoinFragmentDirections.actionCoinFragmentToUserSettingsFragment())
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CoinViewModel::class.java)
    }

    /**
     * Draws the rectangle over the image
     */
    private fun drawFocusRect(bg:Bitmap, box:BoundingBox, name : String, instagram : String?){
        var left = 0f
        var top = 0f
        val height = bg.height
        val width = bg.width

        canvas = mHolder.lockCanvas()
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)


        left = width * box.left
        top = height * box.top

        var d = resources.getDrawable(R.drawable.face_square, null)


        d.setBounds(left.toInt(), top.toInt(), (left + (width * box.width)).toInt(), (top + (height * box.height)).toInt())
        d.draw(canvas)

        if(box.left < 0.5) {
            createNameView(name, left + (box.width * bg.width), top + (box.height * bg.height))
            createInstagramView(instagram,  left + (box.width * bg.width), top + (box.height * bg.height) + 60)
        } else {
            createNameView(name, left, top + (box.height * bg.height))
            createInstagramView(instagram,  left, top + (box.height * bg.height) + 60)
        }


        //canvas.drawRect(left, top, left + (width * box.width), top + (height * box.height), paint)
        mHolder.unlockCanvasAndPost(canvas)

    }

    private fun clearCanvas(){
        canvas = mHolder.lockCanvas()
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        mHolder.unlockCanvasAndPost(canvas)
    }

    /**
     * Initiates the Kinesis Video Client and starts the stream
     */
    private fun createClientAndStartStreaming(previewTexture: SurfaceTexture) {
        try {
            DBUtility.createKinesisVideoClient(context!!)

            // Media Source to send data to Video Stream
            mCameraMediaSource = DBUtility.createMediaSource(context!!, 1)
            mCameraMediaSource.setPreviewSurfaces(Surface(previewTexture))

            if (mCameraMediaSource == null) {
                return
            }
            mCameraMediaSource.start()
        } catch (e: KinesisVideoException) {
            Log.e(TAG, "unable to start streaming")
            throw RuntimeException("unable to start streaming", e)
        }
    }

    /**
     * Starts the stream
     */
    private fun resumeStreaming() {
        try {
            startStream = true
            binding.coinStreamButton.text = activity!!.getText(R.string.stop_streaming)
            // Starts the Timer for the stream
            timeUp = false
            /*
            GlobalScope.launch {
                delay(5000)
                timeUp = true
                startStream = false
                clearCanvas()
                updateListView()
            }*/
        } catch (e: KinesisVideoException) {
            Log.e(TAG, "unable to resume streaming", e)
            makeToast("failed to resume streaming", 1)
        }
    }

    /**
     * Pauses the stream
     */
    private fun pauseStreaming() {
        try {
            if (mCameraMediaSource == null) {
                return
            }
            startStream = false
            clearCanvas()
            updateListView()
            binding.TxtFacesDetected.text = "0 face(s) detected"
            makeToast("stopped streaming",0)
            binding.coinStreamButton.text = activity!!.getText(R.string.start_streaming)
        } catch (e: KinesisVideoException) {
            Log.e(TAG, "unable to pause streaming", e)
            makeToast("failed to pause streaming", 1)
        }
        binding.coinStreamButton.text = activity!!.getText(R.string.start_streaming)


    }

    override fun onResume() {
        super.onResume()
        try{
            mCameraMediaSource.start()
        }catch(e: Exception){
            Log.d(TAG ,e.toString())
        }
    }

    override fun onPause() {
        super.onPause()
        mCameraMediaSource.stop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.coinStreamButton.setOnClickListener{streamButtonClick()}
    }

    /**
     * On click for the stream button
      */
    private fun streamButtonClick(){
        if(!startStream) resumeStreaming()
        else pauseStreaming()
    }

    /**
     * Navigates to the users Instagram
     * Handles stopping the streaming instance
     */
    private fun navigateToInstagram(handle: String){
        try {
            if (mCameraMediaSource == null) {
                return
            }
            mCameraMediaSource.stop()
            viewModel.navigateToInstagram(context!!, handle)
        } catch (e: KinesisVideoException) {
            Log.e(TAG, "unable to pause streaming", e)
            makeToast("failed to pause streaming", 1)
        }

    }

    private fun clearAllTextsFromScreen() {
        for(textView in textViews) {
            layout.removeView(textView)
        }
        layout.invalidate()
        textViews.clear()
    }

    private fun createNameView(name : String?, leftMargin: Float, topMargin : Float) {
        var textView = TextView(context)
        textView.textSize = 30f
        textView.text = name

        val params = FrameLayout.LayoutParams(layout.width, layout.height)
        params.leftMargin = leftMargin.toInt()
        params.topMargin = topMargin.toInt()
        layout.addView(textView, params)
        layout.invalidate()
        textViews.add(textView)
    }

    private fun createInstagramView(instagram : String?,leftMargin: Float, topMargin: Float) {
        var textView = TextView(context)
        textView.textSize = 30f
        textView.text = instagram

        val params2 = FrameLayout.LayoutParams(layout.width, layout.height)
        params2.leftMargin = leftMargin.toInt()
        params2.topMargin = topMargin.toInt()
        layout.addView(textView, params2)
        layout.invalidate()
        textViews.add(textView)
    }

    private fun updateListView(){
        val layout = binding.facesFoundLayout
        for(faces in facesFoundList){
            val userSettings = UserSettingsFragment()
            var userBitmap = DBUtility.retrieveImageFromS3Bitmap(context!!, faces.first)
            userBitmap = Bitmap.createScaledBitmap(userBitmap, 400, 400, false)
            val userImage = ImageButton(context)
            userImage.layoutParams = LinearLayout.LayoutParams(400,400)
            userImage.rotation = 90F
            userImage.x = 20F
            userImage.y = 20F
            userImage.setImageBitmap(userBitmap)
            userImage.setOnClickListener { navigateToInstagram(faces.second) }
            layout.addView(userImage)
        }
    }

    //TODO Properly implement
    /**
     * Changes the direction of the camera
     */
    private fun changeCameraDirection(){
        try {
            if (mCameraMediaSource != null) mCameraMediaSource.stop()
            if (DBUtility.kinesisVideoClient != null) DBUtility.kinesisVideoClient.stopAllMediaSources()
            KinesisVideoAndroidClientFactory.freeKinesisVideoClient()

            DBUtility.createKinesisVideoClient(context!!)
            mCameraMediaSource = viewModel.changeCameraDirection(context!!)
            mCameraMediaSource.setPreviewSurfaces(Surface(previewwTexture))

            if (mCameraMediaSource == null) {
                return
            }
            mCameraMediaSource.start()
        } catch (e: KinesisVideoException) {
            Log.e(TAG, "failed to release kinesis video client", e)
        }
    }

    ////
    // TextureView.SurfaceTextureListener methods
    ////

    @SuppressLint("SetTextI18n")
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        //Scan the users face after the start stream button has been tapped
        if (startStream && !timeUp) {
            // Updates when a face has been found
            var faceFound = false

            // Creating screenshot of the screen
            val screenFrame = Bitmap.createBitmap(textureView.width, textureView.height, Bitmap.Config.ARGB_8888)
            val screenImage = Bitmap.createScaledBitmap(screenFrame, 100, 100, false)
            textureView.getBitmap(screenImage)

            // Converts to AWS Image
            val image = Image().withBytes(viewModel.convertToImage(context!!, screenImage))
            try {
                clearAllTextsFromScreen()
                val results = viewModel.detectFaces(image)
                Log.i(TAG, "Detected ${results!!.faceDetails.size} faces")
                binding.TxtFacesDetected.text = "${results!!.faceDetails.size} face(s) detected"
                if(results!!.faceDetails.isEmpty()) {
                    clearCanvas()
                    return
                }
                for (i in results!!.faceDetails) {
                    val faceShot = Bitmap.createBitmap(
                        screenImage,
                        (i.boundingBox.left * screenImage.width).toInt(),
                        (i.boundingBox.top * screenImage.height).toInt(),
                        (i.boundingBox.width * screenImage.width).toInt(),
                        (i.boundingBox.height * screenImage.height).toInt()
                    )
                    val facesFound = viewModel.searchCollection(context!!, Image().withBytes((viewModel.convertToImage(context!!, faceShot))))
                    Log.i(TAG, "Searching collection for face @ ${i.boundingBox}")
                    if (facesFound.faceMatches.isEmpty()) {
                        Log.i(TAG, "Face not found in collection")
                        drawFocusRect(screenFrame, i.boundingBox, "Face not recognized", "")
                        continue
                    } else {
                        for (face in facesFound.faceMatches) {
                            val userData = UserManager.UserDocs[face.face.externalImageId]

                            //Get the relevant info we want by calling userData?.get(""
                            val name = "${userData?.first} ${userData?.last}"
                            val handle = userData?.igHandle

                            Log.i(TAG, "Face found in collection with name $name (id: ${face.face.externalImageId}")

                            drawFocusRect(screenFrame, i.boundingBox, name, "@${handle}")

                            if (Pair(
                                    face.face.externalImageId,
                                    handle
                                ) !in facesFoundList
                            ) facesFoundList.add(Pair(face.face.externalImageId, handle!!))

                        }
                    }
                }
            } catch (e: Exception) {
                //makeToast(e.toString(), 1)
                Log.e(TAG, e.toString())
            }
/*
            // Scanning the capture for faces
            try {
                clearAllTextsFromScreen()
                // TODO Put in own try block and handle network request?
                // Identifying users in the image

                /*val results = viewModel.detectFaces(image)
                if (results?.faceDetails!!.isEmpty()) {
                    clearCanvas()
                    return
                }*/

                val facesFound = viewModel.searchCollection(context!!, image)

                if(facesFound.faceMatches.isEmpty()) {
                    clearCanvas()
                    return
                }

                for (face in facesFound.faceMatches) {
                    //Get the users information by passing the externalImageId, which corresponds to the UserID, as key to UserManager.UserDocs
                    val userData = UserManager.UserDocs[face.face.externalImageId]

                    //Get the relevant info we want by calling userData?.get(""
                    val name = "${userData?.first} ${userData?.last}"
                    val handle = userData?.igHandle

                   //drawFocusRect(screenFrame, facesFound.searchedFaceBoundingBox, name, handle)

                    if (Pair(
                            face.face.externalImageId,
                            handle
                        ) !in facesFoundList
                    ) facesFoundList.add(Pair(face.face.externalImageId, handle!!))
                }
            } catch (e: Exception) {
                //makeToast(e.toString(), 1)
                Log.e(TAG, e.toString())
            }*/
        }
    }


    /**
     * Created after the onCreateView
     */
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        surface!!.setDefaultBufferSize(1280, 720)
        previewwTexture = surface
        createClientAndStartStreaming(surface)
    }
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        TODO("Not yet implemented")
    }
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        try {
            if (mCameraMediaSource != null) mCameraMediaSource.stop()
            if (DBUtility.kinesisVideoClient != null) DBUtility.kinesisVideoClient.stopAllMediaSources()
            KinesisVideoAndroidClientFactory.freeKinesisVideoClient()
        } catch (e: KinesisVideoException) {
            Log.e(TAG, "failed to release kinesis video client", e)
        }
        return true
    }

    private fun makeToast(msg: String, length: Int) {
        if (length == 0)
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() else
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    }

}
