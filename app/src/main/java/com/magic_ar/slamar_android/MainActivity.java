package com.magic_ar.slamar_android;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
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
    //** Image Detector
    private ImageDetectionFilter mImageDetector;
    // The camera view.
    private CameraBridgeViewBase mCameraView;
    // An adapter between the video camera and projection matrix.
    private CameraProjectionAdapter mCameraProjectionAdapter;
    // The renderer for 3D augmentations
    private ARCubeRenderer mARRenderer;

    // The OpenCV loader callback
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(final int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");
                    mCameraView.enableView();
                    //** Image Detector
                    try {
                        mImageDetector = new ImageDetectionFilter(MainActivity.this, R.drawable.starry_night, mCameraProjectionAdapter, 1.0);
                        mARRenderer.filter = mImageDetector;
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to load drawable: " + "starry_night");
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Load the OpenCV package first.
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, MainActivity.this, mLoaderCallback);

        //** OpenCV
        mCameraView = new JavaCameraView(MainActivity.this, 0);
        mCameraView.setCvCameraViewListener(MainActivity.this);
        mCameraView.setLayoutParams(
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        //** OpenGL
        final GLSurfaceView glSurfaceView = new GLSurfaceView(MainActivity.this);
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        glSurfaceView.setZOrderOnTop(true);
        glSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        //**
        mARRenderer = new ARCubeRenderer();


        requestCameraPermission(new PermissionCallback() {
            @Override
            public void onSuccess() {
                // mCameraView.setVisibility(SurfaceView.VISIBLE);
                ((ViewGroup)findViewById(R.id.preview)).addView(mCameraView);
                ((ViewGroup)findViewById(R.id.preview)).addView(glSurfaceView);

                mCameraProjectionAdapter = new CameraProjectionAdapter();
                mARRenderer.cameraProjectionAdapter = mCameraProjectionAdapter;
                // Earlier, we defined the printed image's size as 1.0 unit.
                // Define the cube to be half this size.
                mARRenderer.scale = 0.5f;
                glSurfaceView.setRenderer(mARRenderer);
                Camera camera = Camera.open(0);
                final Camera.Parameters  parameters = camera.getParameters();
                final Camera.Size size = camera.new Size(1280,720);
                camera.release();
                mCameraProjectionAdapter.setCameraParameters(parameters, size);
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
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
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
        //** Image detector
        if(mImageDetector != null) {
            Log.d(TAG, "Image detecting...");
            mImageDetector.apply(rgba, rgba);
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
}
