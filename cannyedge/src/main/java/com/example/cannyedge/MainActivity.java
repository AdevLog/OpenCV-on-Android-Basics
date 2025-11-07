package com.example.cannyedge;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
/**
 * MainActivity - An OpenCV real-time image processing example based on AppCompatActivity.
 * - Structure: Manages Activity lifecycle, OpenCV initialization, and image processing modules.
 * - Functionality: Displays the camera preview and processes it in real-time using Canny edge detection.
 */
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // ============ Constants ============
    private static final String TAG = "OpenCVCannyApp";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    // ============ Variables ============
    private CameraBridgeViewBase mCameraView;   // OpenCV Camera View interface
    private boolean isOpenCvInitialized = false; // Flag to track if OpenCV has been initialized successfully

    // ============ onCreate ============
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set the layout
        setContentView(R.layout.activity_main);

        // Bind the camera view and set its listener
        mCameraView = findViewById(R.id.camera_view);
        mCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mCameraView.setCvCameraViewListener(this);

        Log.i(TAG, "onCreate: UI initialized, waiting for OpenCV initialization.");
    }

    // ============ onResume ============
    @Override
    protected void onResume() {
        super.onResume();

        // 1. Attempt to initialize the OpenCV library
        if (!isOpenCvInitialized) {
            if (OpenCVLoader.initLocal()) {
                isOpenCvInitialized = true;
                Log.i(TAG, "onResume: OpenCV loaded successfully!");
            } else {
                Log.e(TAG, "onResume: OpenCV initialization failed!");
                Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // 2. Check for camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mCameraView.setCameraPermissionGranted(); // Let OpenCV camera know that permission has been granted
            mCameraView.enableView(); // Permission granted → enable the camera view
            Log.i(TAG, "onResume: Camera permission has been granted.");
        } else {
            // Permission not granted → request it
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST
            );
        }
    }

    // ============ onRequestPermissionsResult ============
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    // Call onResume again to enable the camera.
                    onResume();
                }  else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    Toast.makeText(this, "Camera permission was denied.", Toast.LENGTH_LONG).show();
                }
                return;
        }
    }

    // ============ onPause ============
    @Override
    protected void onPause() {
        super.onPause();
        // Stop the camera preview to save resources
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        Log.i(TAG, "onPause: Camera view paused.");
    }

    // ============ onDestroy ============
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release camera resources completely
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        Log.i(TAG, "onDestroy: Camera resources released.");
    }

    // ============ OpenCV Camera Callbacks ============

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "onCameraViewStarted: Camera view started with resolution " + width + "x" + height);
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped: Camera view stopped.");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // This method is called for every frame for image processing
        Mat rgba = inputFrame.rgba();           // Get the camera frame in RGBA format
        return ImageProcessor.applyCanny(rgba); // Call the image processing module
    }

    // =====================================================================
    // ============ OpenCV Image Processing Module (extendable) ============
    // =====================================================================
    public static class ImageProcessor {

        // Parameters for Canny edge detection
        private static final double THRESHOLD1 = 80;
        private static final double THRESHOLD2 = 150;

        /**
         * Converts the input Mat to a Canny edge image.
         */
        public static Mat applyCanny(Mat input) {
            Mat gray = new Mat();   // Grayscale temporary Mat
            Mat edges = new Mat();  // Output edges Mat

            // Convert to grayscale
            Imgproc.cvtColor(input, gray, Imgproc.COLOR_RGBA2GRAY);

            // Apply Canny
            Imgproc.Canny(gray, edges, THRESHOLD1, THRESHOLD2);

            // Convert back to RGBA format for display
            Imgproc.cvtColor(edges, input, Imgproc.COLOR_GRAY2RGBA);

            // Release intermediate Mats and return the modified frame
            gray.release();
            edges.release();
            return input;
        }
    }
}
