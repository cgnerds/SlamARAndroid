package com.magic_ar.slamar_android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
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
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.security.Policy;
import java.util.List;

// Use the deprecated Camera class.
@SuppressWarnings("deprecation")
public class CameraActivity extends AppCompatActivity
  implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Permissions
    private static final int REQUEST_CODE = 0;
    static final String[] PERMISSIONS = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    // private PermissionChecker permissionChecker;


    // A tag for log output.
    private static final String TAG = CameraActivity.class.getSimpleName();
    // A key for storing the index of the active camera.
    private static final String STATE_CAMERA_INDEX = "cameraIndex";
    // A key fro storing the index of the active iamge size.
    private static final String STATE_IMAGE_SIZSE_INDEX = "imageSizeIndex";
    // An ID for items in the image size submenu.
    private static final int MENU_GROUP_ID_SIZE = 2;
    // The index of the active camera.
    private int mCameraIndex;
    // The index of the active image size.
    private int mImageSizeIndex;
    // Whether the active camera is front-facting.
    // If so, the camera view should be mirrored.
    private boolean mIsCameraFrontFacing;
    // The number of cameras on the device.
    private int mNumCameras;
    // The camera view.
    private CameraBridgeViewBase mCameraView;
    // The image sizes supported by the active camera.
    private List<Camera.Size> mSupportedImageSizes;
    // Whether the next camera frame should be saved as a photo.
    private boolean mIsPhotoPening;
    // A matrix that is used when saving photos.
    private Mat mBgr;
    // Whether an asynchrous menu action is in progress.
    // If so, menu interaction should be diaabled.
    private boolean mIsMenuLocked;
    // The OpenCV loader callback
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");
                    mCameraView.enableView();
                    mBgr = new Mat();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
            super.onManagerConnected(status);
        }
    };

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    // Suppress backward incompatibility errors because we provide backward-compatible fallbacks.
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(savedInstanceState != null) {
            mCameraIndex = savedInstanceState.getInt(
                    STATE_CAMERA_INDEX, 0);
            mImageSizeIndex = savedInstanceState.getInt(
                    STATE_IMAGE_SIZSE_INDEX, 0);
        } else {
            mCameraIndex = 0;
            mImageSizeIndex = 0;
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE);
        } else {
            setupCamera();
        }
    }

    public void setupCamera() {
        final Camera camera;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraIndex, cameraInfo);
            mIsCameraFrontFacing = (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
            mNumCameras = Camera.getNumberOfCameras();
            camera = Camera.open(mCameraIndex);
        } else {
            // Assume there is only 1 camera and it is rear-facing
            mIsCameraFrontFacing = false;
            mNumCameras = 1;
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

    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the current camera index.
        savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);
        // Save the current image size index.
        savedInstanceState.putInt(STATE_IMAGE_SIZSE_INDEX, mImageSizeIndex);

        super.onSaveInstanceState(savedInstanceState);
    }

    // Suppress backward incompatibility errors because we provide backward-compatible fallbacks.
    @SuppressLint("NewApi")
    @Override
    public void recreate() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.recreate();
        } else {
            finish();
            startActivity(getIntent());
        }
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

        // If lacks permissions
        //if(permissionChecker.lacksPermissions(PERMISSIONS)) {
            // startPermissionActivity();
        //}
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        mIsMenuLocked = false;
    }

    @Override
    public void onDestroy() {
        if(mCameraView != null) {
            mCameraView.disableView();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == REQUEST_CODE) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                Toast.makeText(CameraActivity.this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    //private void startPermissionActivity() {
    //    PermissionActivity.startActivityForResult(this, REQUEST_CODE, PERMISSIONS);
    //}

    //@Override
    //protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    //    super.onActivityResult(requestCode, resultCode, data);
    //    if(requestCode == REQUEST_CODE && resultCode == PermissionActivity.PERMISSION_DENIED) {
    //        finish();
    //    }
    //}

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_camera, menu);
        if(mNumCameras < 2) {
            // Remove the option to switch cameras, since there is only 1.
            menu.removeItem(R.id.menu_next_camera);
        }
        int numSupportedImageSizes = mSupportedImageSizes.size();
        if(numSupportedImageSizes > 1) {
            final SubMenu sizeSubMenu = menu.addSubMenu(
                    R.string.menu_image_size);
            for(int i = 0; i < numSupportedImageSizes; ++i) {
                final Camera.Size size = mSupportedImageSizes.get(i);
                sizeSubMenu.add(MENU_GROUP_ID_SIZE, i, Menu.NONE, String.format("%dx%d", size.width, size.height));
            }
        }

        return true;
    }

    // Suppress backward incompatibility errors bacause we provide backward-compatible fallbacks (for recreate).
    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if(mIsMenuLocked) {
            return true;
        }
        if(item.getGroupId() == MENU_GROUP_ID_SIZE) {
            mImageSizeIndex = item.getItemId();
            recreate();

            return true;
        }

        switch (item.getItemId())  {
            case R.id.menu_next_camera:
                mIsMenuLocked = true;
                // With another camera index, recreate the activity.
                mCameraIndex++;
                if(mCameraIndex == mNumCameras) {
                    mCameraIndex = 0;
                }
                mImageSizeIndex = 0;
                recreate();
                return true;
            case R.id.menu_take_photo:
                mIsMenuLocked = true;
                // Next frame, take the photo.
                mIsPhotoPening = true;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final Mat rgba = inputFrame.rgba();
        if(mIsPhotoPening) {
            mIsPhotoPening = false;
            takePhoto(rgba);
        }

        if(mIsCameraFrontFacing) {
            // Mirror (horizontally flip) the preview.
            Core.flip(rgba, rgba, -1);
        }

        return rgba;
    }

    private void takePhoto(final Mat rgba)
    {
        // Determine the path and metadata for the photo.
        final long currentTimeMillis = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + File.separator + appName;
        final String photoPath = albumPath + File.separator + currentTimeMillis + LabActivity.PHOTO_FILE_EXTENTION;
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(MediaStore.Images.Media.MIME_TYPE, LabActivity.PHOTO_MIME_TYPE);
        values.put(MediaStore.Images.Media.TITLE, appName);
        values.put(MediaStore.Images.Media.DESCRIPTION, appName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, currentTimeMillis);

        // Ensure that the album directory exists.
        File album = new File(albumPath);
        if(!album.isDirectory() && !album.mkdir()) {
            Log.e(TAG, "Failed to create album directory at " + albumPath);
            onTakePhotoFailed();
            return;
        }

        // Try to create the photo.
        Imgproc.cvtColor(rgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if(!Imgcodecs.imwrite(photoPath, mBgr)) {
            Log.e(TAG, "Failed to save photo to " + photoPath);
            onTakePhotoFailed();
        }

        Log.d(TAG, "Photo saved successfully to " + photoPath);

        // Try to insert the photo into the MediaStore.
        Uri uri;
        try {
            uri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Exception e) {
            Log.e(TAG, "Failed to insert photo into MediaStore");
            e.printStackTrace();
            // Since the insertion failed, delete the photo.
            File photo = new File(photoPath);
            if(!photo.delete()) {
                Log.e(TAG, "Failed to delete non-inserted photo");
            }

            onTakePhotoFailed();
            return;
        }

        // Open the photo in LabActivity.
        final Intent intent = new Intent(this, LabActivity.class);
        intent.putExtra(LabActivity.EXTRA_PHOTO_URI, uri);
        intent.putExtra(LabActivity.EXTRA_PHOTO_DATA_PATH, photoPath);
        startActivity(intent);
    }

    private void onTakePhotoFailed() {
        mIsMenuLocked = false;

        // Show an error message
        final String errorMessage = getString(R.string.photo_error_message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
