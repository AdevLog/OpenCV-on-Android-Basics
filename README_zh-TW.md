# 應用 OpenCV 於 Android 即時邊緣偵測 [**English**](README.md) | [**繁體中文**](README_zh-TW.md)
本專案示範如何在 Android 上使用相機進行即時邊緣偵測，使用 OpenCV 4.12 SDK 進行影像處理。整體設計簡潔、結構清楚，適合作為學習 OpenCV 在 Android 上應用的入門範例。

## 功能特色
*   即時邊緣偵測：應用 Canny 邊緣偵測演算法於即時的相機畫面。
*   全螢幕顯示：採用簡潔的全螢幕佈局。
*   支援手勢旋轉螢幕：應用程式畫面會根據使用者的裝置方向自動旋轉 (fullSensor)。
*   模組化影像處理架構：可依需求擴充影像演算法。
*   極簡設計：專案結構與程式碼都保持最大程度的簡潔。

## APP畫面展示
<table>
  <tr>
    <td align="center">
      <img src="/assets/camera-preview.png" width="300"/>
      <br>主相機預覽
    </td>
    <td align="center">
      <img src="/assets/canny-edge.png" width="300"/>
      <br>開啟 Canny 邊緣偵測
    </td>
  </tr>
</table>

## 設計核心
*   OpenCV for Android：用於影像處理的開源函式庫。
*   MainActivity.java 結構：繼承自`AppCompatActivity`，遵照 Android Activity 生命週期設計，影像處理演算法模組化管理。若要繼承`CameraActivity`請參考 [MainExtendsCam.java](/assets/MainExtendsCam.java)。
*   Android 最低支援版本：Android 7.0 (Nougat)。

## 開始使用
您可以按照以下步驟，在您自己的電腦上複製並執行此專案。
### 開發環境
*   Android Studio Narwhal 3
*   OpenJDK 17 — [OpenCV 官方要求](https://github.com/opencv/opencv/wiki/Custom-OpenCV-Android-SDK-and-AAR-package-build)
*   opencv-4.12.0-android-sdk — [下載檔案](https://github.com/opencv/opencv/releases)

### 專案架構
```lua
YourProject/
├── assets/
├── cannyedge/
├── opencv-sdk/     <-- 必須加入module dependency
├── README.md
└── settings.gradle
```

### 匯入與建置
*下載此專案*
1. 在 Android Studio 中以模組方式匯入本專案。
    * Click File -> New -> Import module... and select cannyedge path
2. 參考 [OpenCV 官方教學](https://docs.opencv.org/4.x/d5/df8/tutorial_dev_with_OCV_on_Android.html) 建立及匯入 OpenCV 模組。
    * Click File -> New -> Import module... and select OpenCV SDK path
3. 確保 `settings.gradle` 已正確包含以下模組：
    ```java
    include ':opencv-sdk'
    include ':cannyedge'
    ```    
4. 參考 [OpenCV 官方教學](https://docs.opencv.org/4.x/d5/df8/tutorial_dev_with_OCV_on_Android.html) 將 OpenCV 模組與本專案建立依附關係 (Module dependency)。
    * Click File -> Project structure... -> Dependencies -> All modules -> + (Add Dependency button) -> Module dependency
5. 確保 [`build.gradle`](/cannyedge/build.gradle) 已正確設定 OpenCV SDK 依附元件：
    ```java
    dependencies {
        implementation project(':opencv-sdk')
    }
    ```
6. 參考 [OpenCV 官方腳本](https://github.com/opencv/opencv/blob/4.x/samples/android/build.gradle.in)，確保 [`build.gradle`](/cannyedge/build.gradle) 包含以下設定：
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
7. 編譯與執行，即可在裝置上顯示即時 Canny 演算法畫面。

### 應用程式黑畫面問題
| 原因 | 解法 |
| ------------- |:-------------:|
| 沒有初始化 OpenCV     | 在 onResume 裡呼叫OpenCVLoader.initLocal()     |
| 相機未授權     | 用 requestPermissions() 授權     |
| 沒有呼叫 setCameraPermissionGranted()     | 在 enableView() 前加上它     |

## 致謝
本專案由 [AdevLog](https://github.com/AdevLog) 開發，文件撰寫與程式碼建議由 ChatGPT 與 Google Gemini 提供協助。

## 授權條款
此專案採用 MIT 授權條款。詳情請參閱 LICENSE 檔案。