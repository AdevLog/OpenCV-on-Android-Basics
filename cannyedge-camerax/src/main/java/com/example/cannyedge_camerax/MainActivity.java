package com.example.cannyedge_camerax;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MainActivity - CameraX + OpenCV real-time image processing
 * Uses manual YUV to BGR Mat conversion (OpenCV standard format CV_8UC3)
 * Complete flow: ImageProxy (YUV) → Mat (BGR) → OpenCV Processing → Mat (BGR) → Bitmap → Display
 */
public class MainActivity extends AppCompatActivity {

    // ============ Constants ============
    private static final String TAG = "CameraXOpenCVApp";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    // ============ UI Components ============
    private ImageView imageView;

    // ============ CameraX Components ============
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;

    // ============ Executor for camera operations ============
    private ExecutorService cameraExecutor;

    // ============ OpenCV initialization flag ============
    private boolean isOpenCvInitialized = false;

    // ============ onCreate - Initialize UI and executor ============
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on during camera operation
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set the layout
        setContentView(R.layout.activity_main);

        // Bind ImageView for displaying processed frames
        imageView = findViewById(R.id.image_view);

        // Create single thread executor for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor();

        Log.i(TAG, "onCreate: UI and executor initialized");
    }

    // ============ onResume - Initialize OpenCV and start camera ============
    @Override
    protected void onResume() {
        super.onResume();

        // Step 1: Initialize OpenCV library (only once)
        if (!isOpenCvInitialized) {
            if (OpenCVLoader.initLocal()) {
                isOpenCvInitialized = true;
                Log.i(TAG, "onResume: OpenCV loaded successfully");
            } else {
                Log.e(TAG, "onResume: OpenCV initialization failed");
                Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Step 2: Check camera permission
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            // Request camera permission if not granted
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST
            );
        }
    }

    // ============ Check if camera permission is granted ============
    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    // ============ Handle permission request result ============
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (allPermissionsGranted()) {
                // Permission granted - start camera
                startCamera();
            } else {
                // Permission denied - show message
                Toast.makeText(
                        this,
                        "Camera permission was denied.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // ============ Start CameraX and bind ImageAnalysis use case ============
    private void startCamera() {
        // Get CameraProvider instance asynchronously
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Obtain the CameraProvider
                cameraProvider = cameraProviderFuture.get();

                // Build ImageAnalysis use case for frame processing
                imageAnalyzer = new ImageAnalysis.Builder()
                        // Keep only latest frame to avoid backlog
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        // Use YUV_420_888 format (default) for manual conversion
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build();

                // Set the analyzer callback
                imageAnalyzer.setAnalyzer(cameraExecutor, new CannyEdgeAnalyzer());

                // Select back camera as default
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind all previous use cases before binding new ones
                cameraProvider.unbindAll();

                // Bind ImageAnalysis use case to lifecycle
                camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        imageAnalyzer
                );

                Log.i(TAG, "startCamera: Camera started successfully");

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "startCamera: Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ============ onPause - Lifecycle callback ============
    @Override
    protected void onPause() {
        super.onPause();
        // CameraX handles lifecycle automatically
        Log.i(TAG, "onPause: Activity paused");
    }

    // ============ onDestroy - Clean up resources ============
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown the executor to release resources
        cameraExecutor.shutdown();
        Log.i(TAG, "onDestroy: Executor shut down, resources released");
    }

    // ==========================================================
    // ============ CameraX ImageAnalysis Analyzer ==============
    // ==========================================================

    /**
     * CannyEdgeAnalyzer - Processes each camera frame with OpenCV
     * Manual conversion flow: ImageProxy (YUV_420_888) → Mat (BGR, CV_8UC3) → Processing → Display
     * Uses BGR format which is the standard for OpenCV algorithms
     */
    private class CannyEdgeAnalyzer implements ImageAnalysis.Analyzer {

        // Frame processing throttle to limit CPU usage
        private long lastProcessedTime = 0;
        private final long minProcessInterval = 33; // ~30 FPS (1000ms / 30 = 33ms)

        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            // Get current timestamp
            long currentTime = System.currentTimeMillis();

            // Throttle: Skip frame if processed too recently
            if (currentTime - lastProcessedTime < minProcessInterval) {
                imageProxy.close();
                return;
            }

            // Update last processed time
            lastProcessedTime = currentTime;

            try {
                // Step 1: Convert ImageProxy (YUV_420_888) to OpenCV Mat (BGR, CV_8UC3)
                // BGR is the standard format for OpenCV image processing
                Mat bgrMat = imageProxyToMat(imageProxy);

                // Step 2: Handle camera rotation if needed
                int rotation = imageProxy.getImageInfo().getRotationDegrees();
                if (rotation != 0) {
                    Mat rotatedMat = new Mat();
                    switch (rotation) {
                        case 90:
                            Core.rotate(bgrMat, rotatedMat, Core.ROTATE_90_CLOCKWISE);
                            break;
                        case 180:
                            Core.rotate(bgrMat, rotatedMat, Core.ROTATE_180);
                            break;
                        case 270:
                            Core.rotate(bgrMat, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE);
                            break;
                    }
                    bgrMat.release();
                    bgrMat = rotatedMat;
                }

                // Step 3: Apply Canny edge detection (OpenCV processing)
                Mat processedMat = ImageProcessor.applyCanny(bgrMat);

                // Step 4: Convert processed Mat (BGR) to Bitmap (ARGB_8888) for display
                Bitmap outputBitmap = matToBitmap(processedMat);

                // Step 5: Display result on UI thread
                runOnUiThread(() -> imageView.setImageBitmap(outputBitmap));

                // Step 6: Release resources to prevent memory leak
                bgrMat.release();
                processedMat.release();

                Log.d(TAG, "analyze: Frame processed successfully");

            } catch (Exception e) {
                Log.e(TAG, "analyze: Frame processing failed", e);
            } finally {
                // Always close ImageProxy to release camera buffer
                imageProxy.close();
            }
        }

        // ==============================================
        // Format Conversion Methods
        // (For production code, consider extracting to
        //  a separate ImageConverter utility class)
        // ==============================================

        /**
         * Convert ImageProxy (YUV_420_888 format) to OpenCV Mat (BGR format, CV_8UC3)
         * YUV_420_888 is the default camera output format from CameraX
         * BGR (CV_8UC3) is the standard format for OpenCV algorithms
         *
         * YUV_420_888 format structure:
         * - Y plane: Full resolution luminance (brightness) data
         * - U plane: Quarter resolution chrominance (color blue difference)
         * - V plane: Quarter resolution chrominance (color red difference)
         *
         * @param imageProxy Input image from CameraX in YUV_420_888 format
         * @return OpenCV Mat in BGR format (CV_8UC3, 3 channels)
         */
        private Mat imageProxyToMat(ImageProxy imageProxy) {
            // Get the three planes (Y, U, V) from ImageProxy
            ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();

            // Get ByteBuffers for each plane
            ByteBuffer yBuffer = planes[0].getBuffer(); // Y plane (luminance)
            ByteBuffer uBuffer = planes[1].getBuffer(); // U plane (chrominance)
            ByteBuffer vBuffer = planes[2].getBuffer(); // V plane (chrominance)

            // Calculate sizes for each plane
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            // Create byte array to hold NV21 format data
            // NV21 format: YYYYYYYY VUVUVU (Y plane followed by interleaved VU)
            byte[] nv21 = new byte[ySize + uSize + vSize];

            // Copy Y plane data
            yBuffer.get(nv21, 0, ySize);

            // Copy V plane data (note: V comes before U in NV21)
            vBuffer.get(nv21, ySize, vSize);

            // Copy U plane data
            uBuffer.get(nv21, ySize + vSize, uSize);

            // Create OpenCV Mat from NV21 data
            // Height is 1.5x because YUV420 has Y plane + 0.5x UV planes
            Mat yuvMat = new Mat(imageProxy.getHeight() + imageProxy.getHeight() / 2,
                    imageProxy.getWidth(), CvType.CV_8UC1);
            yuvMat.put(0, 0, nv21);

            // Convert YUV (NV21) to BGR format
            // BGR is OpenCV's standard format (CV_8UC3, 3 channels)
            Mat bgrMat = new Mat();
            Imgproc.cvtColor(yuvMat, bgrMat, Imgproc.COLOR_YUV2BGR_NV21);

            // Release temporary YUV Mat
            yuvMat.release();

            return bgrMat;
        }

        /**
         * Convert OpenCV Mat (BGR format, CV_8UC3) to Android Bitmap (ARGB_8888)
         * This conversion is needed for displaying the processed image in ImageView
         *
         * @param bgrMat Input Mat in BGR format (CV_8UC3, 3 channels)
         * @return Output Bitmap in ARGB_8888 format for Android display
         */
        private Bitmap matToBitmap(Mat bgrMat) {
            // Convert BGR to RGBA for bitmap conversion
            // Android Bitmap expects RGBA format, not BGR
            Mat rgbaMat = new Mat();
            Imgproc.cvtColor(bgrMat, rgbaMat, Imgproc.COLOR_BGR2RGBA);

            // Create Bitmap with same dimensions as Mat
            Bitmap bitmap = Bitmap.createBitmap(
                    rgbaMat.cols(),
                    rgbaMat.rows(),
                    Bitmap.Config.ARGB_8888
            );

            // Use OpenCV utility to convert Mat to Bitmap
            // Utils.matToBitmap handles RGBA to ARGB conversion internally
            Utils.matToBitmap(rgbaMat, bitmap);

            // Release temporary RGBA Mat
            rgbaMat.release();

            return bitmap;
        }
    }

    // ==========================================================
    // ============ OpenCV Image Processing Module ==============
    // ==========================================================

    /**
     * ImageProcessor - Contains OpenCV image processing algorithms
     * All methods expect input in BGR format (CV_8UC3), which is OpenCV's standard
     */
    public static class ImageProcessor {

        // Canny edge detection thresholds
        private static final double THRESHOLD1 = 80.0;  // Lower threshold for edge linking
        private static final double THRESHOLD2 = 150.0; // Upper threshold for edge detection

        /**
         * Apply Canny edge detection to input Mat
         * Input and output are both in BGR format (CV_8UC3)
         *
         * Canny edge detection algorithm:
         * 1. Convert to grayscale (edges don't need color information)
         * 2. Apply Gaussian blur to reduce noise
         * 3. Find intensity gradients
         * 4. Apply non-maximum suppression
         * 5. Apply double threshold to identify edges
         * 6. Track edges by hysteresis
         *
         * @param input Input Mat in BGR format (CV_8UC3)
         * @return Output Mat with edges in BGR format (white edges on black background)
         */
        public static Mat applyCanny(Mat input) {
            // Create temporary matrices
            Mat grayMat = new Mat();   // Grayscale image
            Mat edgesMat = new Mat();  // Edge detection result (single channel)

            // Step 1: Convert BGR to Grayscale
            // Canny algorithm requires single channel (grayscale) input
            Imgproc.cvtColor(input, grayMat, Imgproc.COLOR_BGR2GRAY);

            // Step 2: Apply Canny edge detection algorithm
            // THRESHOLD1: Lower threshold for edge linking
            // THRESHOLD2: Upper threshold for initial edge detection
            Imgproc.Canny(grayMat, edgesMat, THRESHOLD1, THRESHOLD2);

            // Step 3: Convert grayscale edges back to BGR (3 channels)
            // This allows the result to be displayed in color or further processed
            Mat outputMat = new Mat();
            Imgproc.cvtColor(edgesMat, outputMat, Imgproc.COLOR_GRAY2BGR);

            // Step 4: Release temporary matrices to prevent memory leak
            grayMat.release();
            edgesMat.release();

            return outputMat;
        }

        /**
         * PLACEHOLDER: Add more OpenCV processing methods here
         * All methods should work with BGR format (CV_8UC3) for consistency
         *
         * Examples:
         * - Gaussian Blur: Imgproc.GaussianBlur(input, output, kernelSize, sigma)
         * - Threshold: Imgproc.threshold(input, output, thresh, maxval, type)
         * - Morphology: Imgproc.morphologyEx(input, output, op, kernel)
         * - Contours: Imgproc.findContours(...)
         * - Color space: Imgproc.cvtColor(input, output, Imgproc.COLOR_BGR2HSV)
         */
    }
}