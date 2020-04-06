package com.bohil.coin.fragment_streaming;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.amazonaws.kinesisvideo.client.KinesisVideoClient;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.kinesisvideo.client.KinesisVideoAndroidClientFactory;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSource;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSourceConfiguration;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.AgeRange;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.util.IOUtils;
import com.bohil.coin.R;
import com.bohil.coin.VideoStreamApp;
import com.bohil.coin.main.SimpleNavActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class StreamingFragment extends Fragment implements TextureView.SurfaceTextureListener {
    public static final String KEY_MEDIA_SOURCE_CONFIGURATION = "mediaSourceConfiguration";
    public static final String KEY_STREAM_NAME = "streamName";

    private static final String TAG = StreamingFragment.class.getSimpleName();

    // Starting Rekognition
    private AmazonRekognition rekognitionClient = new AmazonRekognitionClient(AWSMobileClient.getInstance());
    private TextureView textureView;
    private Button mStartStreamingButton;
    private KinesisVideoClient mKinesisVideoClient;
    private String mStreamName;
    private AndroidCameraMediaSourceConfiguration mConfiguration;
    private AndroidCameraMediaSource mCameraMediaSource;
    private ByteBuffer sourceImageBytes;

    private SimpleNavActivity navActivity;

    public static StreamingFragment newInstance(SimpleNavActivity navActivity) {
        StreamingFragment s = new StreamingFragment();
        s.navActivity = navActivity;
        return s;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        getArguments().setClassLoader(AndroidCameraMediaSourceConfiguration.class.getClassLoader());
        mStreamName = getArguments().getString(KEY_STREAM_NAME);
        mConfiguration = getArguments().getParcelable(KEY_MEDIA_SOURCE_CONFIGURATION);

        // Temporarily changing the thread mode to allow network requests on main
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        final View view = inflater.inflate(R.layout.fragment_streaming, container, false);
        textureView = view.findViewById(R.id.texture);
        textureView.setSurfaceTextureListener(this);


        return view;
    }

    private void createClientAndStartStreaming(final SurfaceTexture previewTexture) {

        try {
            mKinesisVideoClient = KinesisVideoAndroidClientFactory.createKinesisVideoClient(
                    getActivity(),
                    VideoStreamApp.KINESIS_VIDEO_REGION,
                    VideoStreamApp.getCredentialsProvider());

            // Media Source to send data to Video Stream
            mCameraMediaSource = (AndroidCameraMediaSource) mKinesisVideoClient
                    .createMediaSource(mStreamName, mConfiguration);

            mCameraMediaSource.setPreviewSurfaces(new Surface(previewTexture));

            // Starts streaming
            resumeStreaming();
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to start streaming");
            throw new RuntimeException("unable to start streaming", e);
        }
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mStartStreamingButton = view.findViewById(R.id.start_streaming);
        mStartStreamingButton.setOnClickListener(stopStreamingWhenClicked());
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeStreaming();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseStreaming();
    }

    private View.OnClickListener stopStreamingWhenClicked() {
        return new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                pauseStreaming();
                navActivity.startConfigFragment();
            }
        };
    }

    /**
     * Starts streaming data to the Video Stream
     */
    private void resumeStreaming() {
        try {
            if (mCameraMediaSource == null) {
                return;
            }

            mCameraMediaSource.start();
            Toast.makeText(getActivity(), "resumed streaming", Toast.LENGTH_SHORT).show();
            mStartStreamingButton.setText(getActivity().getText(R.string.stop_streaming));
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to resume streaming", e);
            Toast.makeText(getActivity(), "failed to resume streaming", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseStreaming() {
        try {
            if (mCameraMediaSource == null) {
                return;
            }

            mCameraMediaSource.stop();
            Toast.makeText(getActivity(), "stopped streaming", Toast.LENGTH_SHORT).show();
            mStartStreamingButton.setText(getActivity().getText(R.string.start_streaming));
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to pause streaming", e);
            Toast.makeText(getActivity(), "failed to pause streaming", Toast.LENGTH_SHORT).show();
        }
    }

    ////
    // TextureView.SurfaceTextureListener methods
    ////

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        surfaceTexture.setDefaultBufferSize(1280, 720);
        createClientAndStartStreaming(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        try {
            if (mCameraMediaSource != null)
                mCameraMediaSource.stop();
            if (mKinesisVideoClient != null)
                mKinesisVideoClient.stopAllMediaSources();
            KinesisVideoAndroidClientFactory.freeKinesisVideoClient();
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "failed to release kinesis video client", e);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        //Toast.makeText(getContext(), "Updated?", Toast.LENGTH_SHORT).show();
        // Creating a screenshot of the screen
        Bitmap frame = Bitmap.createBitmap(textureView.getWidth(),textureView.getHeight(),
                Bitmap.Config.ARGB_8888);

        Bitmap image = Bitmap.createScaledBitmap(frame, 100, 100, false);
        textureView.getBitmap(image);

        // Converting to Byte Array
        ByteArrayOutputStream bostream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, bostream);
        byte[] bitmapdata = bostream.toByteArray();

        // Converting to File
        File f = new File(getContext().getCacheDir(), "input");
        try{
            f.createNewFile();
        }catch(Exception e){
            e.fillInStackTrace();
        }
        try{
            FileOutputStream fostream = new FileOutputStream(f);
            fostream.write(bitmapdata);
            fostream.flush();
            fostream.close();
        }catch(Exception e){
            e.fillInStackTrace();
        }

        //Creates source image
        try (InputStream inputStream = new FileInputStream(f)) {
            sourceImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        }
        catch(Exception e)
        {
            System.out.println("Failed to load source image " + f);
            System.exit(1);
        }

        Image source=new Image()
                .withBytes(sourceImageBytes);


        // Creating the face detect request
        DetectFacesRequest request = new DetectFacesRequest().withImage(source).withAttributes("ALL");
        // Detecting the face
        try {
            DetectFacesResult result = rekognitionClient.detectFaces(request);
            List<FaceDetail> faceDetails = result.getFaceDetails();

            Toast.makeText(getContext(), "Faces: " + faceDetails.size(),Toast.LENGTH_SHORT).show();

            for (FaceDetail face: faceDetails) {
                //TODO Discover error
                if (request.getAttributes().contains("ALL")) {
                    AgeRange ageRange = face.getAgeRange();
                    System.out.println("The detected face is estimated to be between "
                            + ageRange.getLow().toString() + " and " + ageRange.getHigh().toString()
                            + " years old.");
                    System.out.println("Here's the complete set of attributes:");
                } else { // non-default attributes have null values.
                    System.out.println("Here's the default set of attributes:");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*
        // Draws the bounding box around the detected faces.
        public void paintComponent(Graphics g) {
            float left = 0;
            float top = 0;
            int height = image.getHeight(this);
            int width = image.getWidth(this);

            Graphics2D g2d = (Graphics2D) g; // Create a Java2D version of g.

            // Draw the image.
            g2d.drawImage(image, 0, 0, width / scale, height / scale, this);
            g2d.setColor(new Color(0, 212, 0));

            // Iterate through faces and display bounding boxes.
            List<FaceDetail> faceDetails = result.getFaceDetails();
            for (FaceDetail face : faceDetails) {

                BoundingBox box = face.getBoundingBox();
                left = width * box.getLeft();
                top = height * box.getTop();
                g2d.drawRect(Math.round(left / scale), Math.round(top / scale),
                        Math.round((width * box.getWidth()) / scale), Math.round((height * box.getHeight())) / scale);

            }
        }*/

    }
}

