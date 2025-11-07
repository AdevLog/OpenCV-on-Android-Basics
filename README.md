# Applying OpenCV for Real-Time Edge Detection on Android [**English**](README.md) | [**繁體中文**](README_zh-TW.md)
This project shows how to use the camera on Android for real-time edge detection with the OpenCV 4.12 SDK. It features a clean and simple design, making it a great starting point for learning OpenCV integration on Android.

## Features
*   Real-time Edge Detection: Live camera frames are processed using the Canny algorithm.
*   Full-Screen Display: Simple and clean layout showing only the camera view.
*   Auto-Rotation: The screen automatically rotates based on device orientation (fullSensor).
*   Modular Image Processing: Image algorithms are organized for easy extension.
*   Ultra-Simple Design: Code and structure are kept as minimal and clean as possible.

## App Screenshots
<table>
  <tr>
    <td align="center">
      <img src="/assets/camera-preview.png" width="300"/>
      <br>Camera preview
    </td>
    <td align="center">
      <img src="/assets/canny-edge.png" width="300"/>
      <br>Canny edge detection
    </td>
  </tr>
</table>

## Core Design
*   OpenCV for Android: Open-source library for image processing.
*   MainActivity.java Structure: Inherits from `AppCompatActivity`, follows the Android Activity lifecycle, and separates image processing into clean, reusable methods. For inheriting from `CameraActivity`, please refer to [MainExtendsCam.java](/assets/MainExtendsCam.java).
*   Minimum Android Version: Android 7.0 (Nougat).

## Getting Started
You can follow the steps below to clone and run this project on your own system.

### Development Environment
*   Android Studio Narwhal 3
*   OpenJDK 17 — [OpenCV Prerequisite](https://github.com/opencv/opencv/wiki/Custom-OpenCV-Android-SDK-and-AAR-package-build).
*   opencv-4.12.0-android-sdk — [Download File](https://github.com/opencv/opencv/releases).

### Project Structure
```lua
YourProject/
├── assets/
├── cannyedge/
├── opencv-sdk/     <-- add module dependency
├── README.md
└── settings.gradle
```

### Importing and Building the Project
*Clone the project*
1. Import this project as a module in Android Studio.
   * Click File -> New -> Import module... and select cannyedge path
2. Follow the [OpenCV official tutorial](https://docs.opencv.org/4.x/d5/df8/tutorial_dev_with_OCV_on_Android.html) to import the OpenCV SDK as a module.
   * Click File -> New -> Import module... and select OpenCV SDK path
3. Ensure that `settings.gradle` correctly includes the modules:
    ```java
    include ':opencv-sdk'
    include ':cannyedge'
    ```  
4. Follow the [OpenCV official tutorial](https://docs.opencv.org/4.x/d5/df8/tutorial_dev_with_OCV_on_Android.html) to add the OpenCV module to this project as a Module dependency.
   * Click File -> Project structure... -> Dependencies -> All modules -> + (Add Dependency button) -> Module dependency
5. Ensure that [`build.gradle`](/cannyedge/build.gradle) properly sets the OpenCV SDK as a module dependency:
    ```
    dependencies {
        implementation project(':opencv-sdk')
    }
    ```
6. Follow the [OpenCV sample script](https://github.com/opencv/opencv/blob/4.x/samples/android/build.gradle.in), and make sure the [`build.gradle`](/cannyedge/build.gradle) includes the following configuration:
    ```
    buildTypes {
        debug {
            packagingOptions {
                doNotStrip '**/*.so'  // controlled by OpenCV CMake scripts
            }
        }
        release {
            packagingOptions {
                doNotStrip '**/*.so'  // controlled by OpenCV CMake scripts
            }
        }
    }
    ``` 
7. Build and run — the app will display the real-time Canny edge detection output on your device.


### Application Black Screen Issues
Cause | Solution
--- | ---
OpenCV not initialized | Call OpenCVLoader.initLocal() in onResume()
Camera permission not granted | Request permission using requestPermissions()
setCameraPermissionGranted() not called | Add it before enableView()

## Acknowledgements
This project was developed by [AdevLog](https://github.com/AdevLog). Documentation and code suggestions were provided by ChatGPT and Google Gemini.

## License
This project is licensed under the MIT License. See the LICENSE file for details.