package com.magic_ar.slamar_android;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.HashMap;

/*
 *  [Android Development with OpenCV](http://docs.opencv.org/2.4/doc/tutorials/introduction/android_binary_package/dev_with_OCV_on_Android.html)
 */
@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    // A tag for log output.
    private static final String TAG = MainActivity.class.getSimpleName();

    // OpenCV static initialization.
    static {
        if(!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded.");
    }

    // Image detector.
    private ImageDetector mImageDetector;
    // The camera view.
    private  CameraBridgeViewBase mCameraView;
    // An adapter between the video camera and projection matrix.
    private CameraProjectionAdapter mCameraProjectionAdapter;
    // The renderer for 3D augmentations.
    private ARCubeRenderer mARRenderer;
    // Whether the ImageDetector is running.
    private boolean detectionRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // JavaCameraView
        mCameraView = new JavaCameraView(this, 0);
        mCameraView.setCvCameraViewListener(this);
        mCameraView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // GLSurfaceView
        final GLSurfaceView glSurfaceView = new GLSurfaceView(MainActivity.this);
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        glSurfaceView.setZOrderOnTop(true);
        glSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        requestCameraPermission(new PermissionCallback() {
            @Override
            public void onSuccess() {
                mARRenderer = new ARCubeRenderer();
                // CameraProjectionAdapter
                mCameraProjectionAdapter = new CameraProjectionAdapter();
                Camera camera = Camera.open(0);
                final Camera.Parameters  parameters = camera.getParameters();
                final Camera.Size size = camera.new Size(1920,1080); // 1280*720
                camera.release();
                mCameraProjectionAdapter.setCameraParameters(parameters, size);
                mARRenderer.cameraProjectionAdapter = mCameraProjectionAdapter;
                // ImageDetector
                try {
                        mImageDetector = new ImageDetector(MainActivity.this, R.drawable.starry_night, mCameraProjectionAdapter, 1.0);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to load drawable: " + "starry_night");
                        e.printStackTrace();
                }
                mARRenderer.filter = mImageDetector;

                // Set glSurfaceView
                glSurfaceView.setRenderer(mARRenderer);

                mCameraView.enableView();
                mCameraView.setMaxFrameSize(1920, 1080); //** 1280*720
                mCameraView.enableFpsMeter();
                ((ViewGroup)findViewById(R.id.preview)).addView(mCameraView);
                ((ViewGroup)findViewById(R.id.preview)).addView(glSurfaceView);
            }

            @Override
            public void onFailure() {
            }
        });
    }

    private interface PermissionCallback {
        void onSuccess();
        void onFailure();
    }
    private HashMap<Integer, PermissionCallback> permissionCallbacks = new HashMap<Integer, PermissionCallback>();
    private int permissionRequestCodeSerial = 0;
    @TargetApi(23)
    public void requestCameraPermission(PermissionCallback callback) {
        if(Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                int requestCode = permissionRequestCodeSerial;
                permissionRequestCodeSerial += 1;
                permissionCallbacks.put(requestCode, callback);
                requestPermissions(new String[]{Manifest.permission.CAMERA}, requestCode);
            } else {
                callback.onSuccess();
            }
        } else {
            callback.onSuccess();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (permissionCallbacks.containsKey(requestCode)) {
            PermissionCallback callback = permissionCallbacks.get(requestCode);
            permissionCallbacks.remove(requestCode);
            boolean executed = false;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    executed = true;
                    callback.onFailure();
                }
            }
            if (!executed) {
                callback.onSuccess();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mCameraView != null) {
            mCameraView.enableView();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mCameraView != null) {
            mCameraView.disableView();;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mCameraView != null) {
            mCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final Mat rgba = inputFrame.rgba();

        if(!detectionRunning) {
            detectionRunning = true;
            new DetectionTask().execute(rgba);
        }

        return rgba;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class DetectionTask extends AsyncTask<Mat, Void, String> {
        @Override
        protected String doInBackground(Mat... mats) {
            // Image detector
            if(mImageDetector != null) {
                mImageDetector.apply(mats[0], mats[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            detectionRunning = false;
        }
    }
}
