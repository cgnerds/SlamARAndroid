package com.magic_ar.slamar_android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {
    // A tag for log output.
    private static final String TAG = MainActivity.class.getSimpleName();
    // A key for storing the index of the active camera.
    private static final String STATE_CAMERA_INDEX = "cameraIndex";
    // A key for storing the index of the active iamge size.
    private static final String STATE_IMAGE_SIZSE_INDEX = "imageSizeIndex";
    // An ID for items in the image size submenu.
    private static final int MENU_GROUP_ID_SIZE = 2;
    // The index of the active camera.
    private int mCameraIndex;
    // The index of the active image size.
    private int mImageSizeIndex;
    // The camera view.
    private CameraBridgeViewBase mCameraView;
    // The image sizes supported by the active camera.
    private List<Camera.Size> mSupportedImageSizes;

    // The OpenCV loader callback
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");
                    if(mCameraView != null) {
                        mCameraView.enableView();
                    }
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
            super.onManagerConnected(status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the OpenCV package.
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);

        if(savedInstanceState != null) {
            mCameraIndex = savedInstanceState.getInt(
                    STATE_CAMERA_INDEX, 0);
            mImageSizeIndex = savedInstanceState.getInt(
                    STATE_IMAGE_SIZSE_INDEX, 0);
        } else {
            mCameraIndex = 0;
            mImageSizeIndex = 0;
        }

        setContentView(R.layout.activity_main);
        // Request for camera permission
        MainActivityPermissionsDispatcher.setupCameraWithCheck(this);
    }

    @SuppressLint("NewApi")
    @NeedsPermission(Manifest.permission.CAMERA)
    public void setupCamera() {
        final Camera camera;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraIndex, cameraInfo);
            camera = Camera.open(mCameraIndex);
        } else {
            // Assume there is only 1 camera and it is rear-facing
            camera = Camera.open();
        }

        final Camera.Parameters parameters = camera.getParameters();
        camera.release();
        mSupportedImageSizes = parameters.getSupportedPreviewSizes();
        final Camera.Size size = mSupportedImageSizes.get(mImageSizeIndex);
        mCameraView = new JavaCameraView(this, mCameraIndex);
        mCameraView.setMaxFrameSize(size.width, size.height);
        mCameraView.setCvCameraViewListener(this);
        setContentView(mCameraView);
    }

    @Override
    public void onPause() {
        if(mCameraView != null) {
            mCameraView.disableView();;
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
    }

    @Override
    public void onDestroy() {
        if(mCameraView != null) {
            mCameraView.disableView();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
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

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final Mat rgba = inputFrame.rgba();
        return rgba;
    }
}