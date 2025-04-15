# VisionMate

VisionMate is an Android application designed to provide real-time object detection and assistive functionalities using advanced machine learning models. The project leverages TensorFlow Lite for AI processing and integrates features such as Google Maps for location-based services.

---

## Features
1. **Real-Time Object Detection**:
   - Utilizes TensorFlow Lite models for detecting objects and hands in real-time.
   - Supports custom object detection models for specific use cases.

2. **Assistive Functionalities**:
   - Designed to help users interact with the environment through object recognition.

3. **Google Maps Integration**:
   - Includes location-based services such as navigation and mapping.

4. **Camera Features**:
   - Uses Camera2 API for capturing and processing live video streams.
   - Supports both internal and external camera activities.

5. **Interaction with Flask Backend**:
   - The VisionMate Android app communicates with the Flask backend app for real-time data processing, including object and hand detection.
   - The backend provides endpoints for video streaming, object detection, and activating a tactile bracelet for guidance.

---

## Installation

### Prerequisites
- [Android Studio Arctic Fox](https://developer.android.com/studio) or later.
- An Android device with minimum SDK version **26**.
- Python 3.8 or later for running the Flask backend.

### Steps to Set Up the Flask Backend
1. Clone the Flask backend repository:
   ```bash
   git clone https://github.com/Smgunlusoy/Flask_app.git
   cd Flask_app/tactile-guidance-backend
   ```

2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

3. Run the Flask server:
   ```bash
   python app.py
   ```

   The Flask server will start on `http://0.0.0.0:8000`. You can access it locally at `http://localhost:8000`.

### Steps to Set Up the Android App
1. Clone the VisionMate repository:
   ```bash
   git clone https://github.com/monikamunusamy/VisionMate-Project.git
   cd VisionMate-Project
   ```

2. Open the project in Android Studio:
   - Select `File -> Open` and navigate to the project directory.

3. Sync Gradle:
   - Ensure that Gradle dependencies are properly synced before building the project.

4. Build and Run:
   - Connect your Android device.
   - Click on the `Run` button in Android Studio to deploy the app.

---

## Integration Between Flask and Android App

The VisionMate app communicates with the Flask backend via RESTful APIs. The key endpoints are:

1. **Video Streaming**:
   - **URL**: `/video_feed`
   - **Purpose**: Streams camera feed with real-time object and hand detection.
   - **Usage**: Accessed by the Android app to display live feeds.

2. **Object Detection**:
   - **URL**: `/detected_objects`
   - **Method**: `GET`
   - **Purpose**: Detects objects and hands in the current camera view.
   - **Response**: JSON array of detected objects (e.g., `["hand", "bottle"]`).

3. **Activate Tactile Bracelet**:
   - **URL**: `/activate_bracelet`
   - **Method**: `POST`
   - **Purpose**: Guides the user toward a specific detected object.
   - **Request Body**: 
     ```json
     {
       "object_name": "bottle"
     }
     ```
   - **Response**: Confirmation message or error details.

---

## Flask Backend Details

The Flask backend is implemented in the repository [Flask_app](https://github.com/Smgunlusoy/Flask_app). The main file is `app.py`, which contains the following features:
- **YOLO Object Detection**: Integrates YOLOv5 and YOLOv8 models for real-time object and hand detection.
- **Video Feed Service**: Streams the camera feed with detected objects highlighted.
- **Tactile Bracelet Control**: Guides a tactile bracelet to a specified object using vibration feedback.
- **RESTful API**: Provides endpoints for Android app communication.

To run the Flask backend on localhost:
1. Start the server with `python app.py`.
2. Access the default route (`http://localhost:8000/`) to verify the server is live.

---

## Contributors
- **Monika Munusamy** - Project Lead
- **Simge Ünlüsoy** - Developer and Contributor

---

## License
This project is licensed under the MIT License. See the `LICENSE` file for details.

---

## Contribution
Contributions are welcome! Please fork the repository and submit a pull request with your changes. For major changes, open an issue first to discuss what you would like to change.

---

## Credits
- **Author**: Monika Munusamy
- **Acknowledgments**: Contributors to the libraries and APIs used in this project.
