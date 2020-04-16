package com.bohil.coin.main

import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException
import com.amazonaws.mobileconnectors.kinesisvideo.client.KinesisVideoAndroidClientFactory
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSource
import com.amazonaws.services.rekognition.model.BoundingBox
import com.amazonaws.services.rekognition.model.Image
import com.bohil.coin.DBUtility
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentCoinBinding
import com.bohil.coin.settings.UserManager
import com.google.android.gms.vision.Frame


private lateinit var viewModel: CoinViewModel
private lateinit var binding: FragmentCoinBinding
private lateinit var mCameraMediaSource:AndroidCameraMediaSource
private lateinit var textureView:TextureView
private lateinit var surfaceView: SurfaceView
private lateinit var mHolder:SurfaceHolder
private lateinit var picture:Drawable
private lateinit var canvas:Canvas
private lateinit var userIG:String
private lateinit var image : Image
private lateinit var layout : FrameLayout
private lateinit var igTextView : TextView
private lateinit var nameView : TextView
private var topCoord : Float = 0.0f
private var bottomCoord: Float = 0.0f
private var leftCoord : Float = 0.0f
private var rightCoord : Float = 0.0f


@Suppress("DEPRECATION")
class CoinFragment : Fragment(), TextureView.SurfaceTextureListener {
    companion object {
        const val TAG = "CoinFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_coin, container, false)
        binding.lifecycleOwner = this

        layout = binding.mainFrame
        igTextView = TextView(context)
        igTextView.textSize = 20f
        igTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ig_icon, 0, 0, 0)
        nameView = TextView(context)
        nameView.textSize = 20f

        // Binding TextureView
        textureView = binding.coinTextureView
        textureView.surfaceTextureListener = this

        //TODO Implement bounding box with this xml
        //Bouding Box to draw on canvas
        picture = resources.getDrawable(R.drawable.boundingbox)

        // SurfaceView
        surfaceView = binding.surfaceView
        surfaceView.setZOrderOnTop(true)
        mHolder = surfaceView.holder
        mHolder.setFormat(PixelFormat.TRANSPARENT)

        // TextView for Users Instagram
        //binding.tempUserText.setOnClickListener { viewModel.navigateToInstagram(context!!) }

        //TODO Change network requests to background threads in viewmodel
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
        image = DBUtility.retrieveImageFromS3(UserManager.UserID)
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
    private fun drawFocusRect(bg:Bitmap, box:BoundingBox, color:Int){
        var left = 0f
        var top = 0f
        val height = bg.height
        val width = bg.width

        canvas = mHolder.lockCanvas()
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        /*val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = 5f*/


        left = width * box.left
        top = height * box.top

        val d = resources.getDrawable(R.drawable.face_square, null)
        d.setBounds(left.toInt(), top.toInt(), (left + (width * box.width)).toInt(),
            (top + (height * box.height)).toInt()
        )

        d.draw(canvas)

        leftCoord = left
        rightCoord = left + (width * box.width)
        topCoord = top
        bottomCoord = top + (height * box.height)

        //canvas.drawRect(left, top, left + (width * box.width), top + (height * box.height), paint)
        mHolder.unlockCanvasAndPost(canvas)

    }

    private fun clearFocusRect(){
        canvas = mHolder.lockCanvas()
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        mHolder.unlockCanvasAndPost(canvas)
    }

    /**
     * Initiates the Kinesis Video Client
     */
    private fun createClientAndStartStreaming(previewTexture: SurfaceTexture) {
        try {
            DBUtility.createKinesisVideoClient(context!!)

            // Media Source to send data to Video Stream
            mCameraMediaSource = DBUtility.createMediaSource(context!!)
            mCameraMediaSource.setPreviewSurfaces(Surface(previewTexture))

            // Starts streaming
            resumeStreaming()
        } catch (e: KinesisVideoException) {
            Log.e(TAG, "unable to start streaming")
            throw RuntimeException("unable to start streaming", e)
        }
    }

    private fun resumeStreaming() {
        try {
            if (mCameraMediaSource == null) {
                return
            }

            mCameraMediaSource.start()
            Thread.sleep(1000)
            /*try{
                DBUtility.addFaceToCollection(context!!)
            }catch(e:Exception){
                Log.d(TAG, e.toString())
            }*/
            makeToast("resumed streaming", 0)
            binding.coinStreamButton.text = activity!!.getText(R.string.stop_streaming)
        } catch (e: KinesisVideoException) {
            Log.e(TAG, "unable to resume streaming", e)
            makeToast("failed to resume streaming", 1)
        }
    }

    private fun pauseStreaming() {
        try {
            if (mCameraMediaSource == null) {
                return
            }
            mCameraMediaSource.stop()
            makeToast("stopped streaming",0)
            binding.coinStreamButton.text = activity!!.getText(R.string.start_streaming)
        } catch (e: KinesisVideoException) {
            Log.e(TAG, "unable to pause streaming", e)
            makeToast("failed to pause streaming", 1)
        }
    }


    override fun onResume() {
        super.onResume()
        try{
            resumeStreaming()
        }catch(e: Exception){
            Log.d(TAG ,e.toString())
        }
    }

    override fun onPause() {
        super.onPause()
        pauseStreaming()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.coinStreamButton.setOnClickListener{pauseStreaming()}
    }



    ////
    // TextureView.SurfaceTextureListener methods
    ////

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        layout.removeView(igTextView)

        // Creating screenshot of the screen
        val screenFrame = Bitmap.createBitmap(textureView.width, textureView.height, Bitmap.Config.ARGB_8888)
        val screenImage = Bitmap.createScaledBitmap(screenFrame, 100, 100, false)
        textureView.getBitmap(screenImage)

        // Converts to AWS Image
        //val image = Image().withBytes(viewModel.convertToImage(context!!, screenImage))

        // Scanning the capture for faces
        try {
            // App slowed from this network request
            //Scanning the image to retrieve faces
            val results = viewModel.detectFaces(image)
            if (results?.faceDetails?.size!! > 0) {
                // Drawing The Bounding box on found faces
                val faceDetails = results.faceDetails

                // Clears the canvas if no faces are recognized
                if(faceDetails.size == 0)
                    clearFocusRect()

                for (face in faceDetails) {
                    // Drawing Box around users face
                    val box = face.boundingBox
                    drawFocusRect(screenFrame, box, Color.WHITE)
                }

                // TODO Put in own try block and handle network request?
                // Identifying users in the image
                val facesFound = viewModel.searchCollection(context!!, image)
                for (face in facesFound) {
                    //Get the users information by passing the externalImageId, which corresponds to the UserID, as key to UserManager.UserDocs
                    val userData = UserManager.UserDocs[face.face.externalImageId]

                    //Get the relevant info we want by calling userData?.get("")
                    val handle = userData?.igHandle
                    val name = "${userData?.first} ${userData?.last}"

                    if(!handle.isNullOrEmpty()){
                        userIG = handle
                        igTextView.text = "@${userIG}"
                        nameView.text = name
                        igTextView.setOnClickListener { viewModel.navigateToInstagram(context!!, handle) }

                        val params = FrameLayout.LayoutParams(layout.width,layout.height)
                        params.leftMargin = rightCoord.toInt()
                        params.topMargin = bottomCoord.toInt()
                        layout.addView(igTextView, params)

                        val params2 = FrameLayout.LayoutParams(layout.width,layout.height)
                        params2.rightMargin = leftCoord.toInt()
                        params2.topMargin = topCoord.toInt()
                        layout.addView(nameView, params2)
                    }
                }
            }

        } catch (e: Exception) {
            Log.d(TAG, e.toString())
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        surface!!.setDefaultBufferSize(1280, 720)
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
