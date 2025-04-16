# VisionMate 👁️📱

VisionMate is an Android application designed to enable blind and visually impaired individuals to interact with their surroundings more independently. The application provides real-time object detection, navigation, shopping list preparation, and bracelet navigation based on user input. VisionMate uses advanced machine learning models to detect objects in the environment, delivering this information to users through visual, auditory, or tactile feedback — ensuring a safer and more confident daily experience.

This project was completed as part of our **Master's study project**, integrating Android development and Flask-based backend intelligence to solve real-world accessibility challenges.

---

## ✨ Features

1. **🔍 Real-Time Object Detection**

   - Utilizes TensorFlow Lite models for detecting objects and hands in real-time.
   - Supports custom object detection models for specific use cases.

2. **🤖 Assistive Functionalities**

   - Designed to help users interact with the environment through object recognition.

3. **🗺️ Google Maps Integration**

   - Includes location-based services such as navigation and mapping.

4. **📸 Camera Features**

   - Uses Camera2 API for capturing and processing live video streams.
   - **External Camera Integration**: Flask backend is used to handle external camera functionalities.

5. **🔗 Interaction with Flask Backend**

   - The VisionMate Android app communicates with the Flask backend for external camera processing and object detection.
   - The backend provides endpoints for video streaming, object detection, and activating a tactile bracelet for guidance.

---

## 💪 Installation

### 📋 Prerequisites

- [Android Studio Arctic Fox](https://developer.android.com/studio) or later.
- An Android device with minimum SDK version **26**.
- Python 3.8 or later for running the Flask backend.

### ⚙️ Set Up the Flask Backend

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

   

### 📲 Set Up the Android App

1. Clone the VisionMate repository:

   ```bash
   git clone https://github.com/monikamunusamy/VisionMate-Project.git
   cd VisionMate-Project
   ```

2. Open the project in Android Studio:

   - `File -> Open` → Select the project directory.

3. Sync Gradle:

   - Make sure all Gradle dependencies sync successfully.

4. Build and Run:

   - Connect an Android device.
   - Hit the `Run` button in Android Studio.

---

## 🔄 Flask ↔️ Android Integration

The VisionMate app communicates with the Flask backend via RESTful APIs for **external camera** tasks:

### 📺 `/video_feed`

- Streams real-time camera feed with object overlays.

### 🧐 `/detected_objects`

- Method: `GET`
- Returns a JSON array of detected objects.
  Example:
  ```json
  ["hand", "bottle"]
  ```

### 🧴 `/activate_bracelet`

- Method: `POST`
- Guides the bracelet toward a selected object.
- Example request:
  ```json
  {
    "object_name": "bottle"
  }
  ```

---

## 🔍 Flask Backend Details

Backend repo: [Flask_app](https://github.com/Smgunlusoy/Flask_app)

### Key Contributions and Technologies Used:

- **Flask Web Framework**: Hosts all the RESTful API endpoints.
- **YOLOv5 & YOLOv8 Integration**: Real-time object and hand detection using pre-trained models.
- **OpenCV**: Handles external camera streams and frame processing.
- **SocketIO / HTTP Streams**: Streams live video feed to the Android device.
- **Threading & Multiprocessing**: Ensures fast response during real-time processing.
- **BraceletController Module**: Custom-designed to provide tactile feedback using object coordinates.
- **Experimental Hand/Object Navigation Logic**: Detects object and hand location to guide user movement toward target objects.

---

## 👥 Contributors

| Name                      | Role & Contributions                                                                 |
| ------------------------- | ---------------------------------------------------------------------------------- |
| **Monika Munusamy** 👩‍💻 | Android App Development: CameraX integration, UI/UX design, Text-to-Speech, Navigation, and internal camera setup. |
| **Simge Ünlüsoy** 👩‍🔬   | Flask Backend Development: YOLO object detection, API design, external camera, real-time video streaming, and tactile bracelet activation. |

---

## ⚖️ License

This project is licensed under the **MIT License**. See the `LICENSE` file for details.

---

## 🤝 Contribution Guidelines

Contributions are welcome! 💪

- Fork the repo
- Create a new branch
- Commit your changes
- Submit a pull request

For big changes, open an issue to discuss first!

---

## 🙏 Credits

- **Authors**: Monika Munusamy & Simge Ünlüsoy
- **Project Origin**: Developed as part of our **# VisionMate 👁️📱

VisionMate is an Android application designed to enable blind and visually impaired individuals to interact with their surroundings more independently. The application provides real-time object detection, navigation, shopping list preparation, and bracelet navigation based on user input. VisionMate uses advanced machine learning models to detect objects in the environment, delivering this information to users through visual, auditory, or tactile feedback — ensuring a safer and more confident daily experience.

This project was completed as part of our **Master's study project**, integrating Android development and Flask-based backend intelligence to solve real-world accessibility challenges.

---

## ✨ Features

1. **🔍 Real-Time Object Detection**

   - Utilizes TensorFlow Lite models for detecting objects and hands in real-time.
   - Supports custom object detection models for specific use cases.

2. **🤖 Assistive Functionalities**

   - Designed to help users interact with the environment through object recognition.

3. **🗺️ Google Maps Integration**

   - Includes location-based services such as navigation and mapping.

4. **📸 Camera Features**

   - Uses Camera2 API for capturing and processing live video streams.
   - **External Camera Integration**: Flask backend is used to handle external camera functionalities.

5. **🔗 Interaction with Flask Backend**

   - The VisionMate Android app communicates with the Flask backend for external camera processing and object detection.
   - The backend provides endpoints for video streaming, object detection, and activating a tactile bracelet for guidance.

---

## 💪 Installation

### 📋 Prerequisites

- [Android Studio Arctic Fox](https://developer.android.com/studio) or later.
- An Android device with minimum SDK version **26**.
- Python 3.8 or later for running the Flask backend.

### ⚙️ Set Up the Flask Backend

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

   Flask server starts at `http://0.0.0.0:8000` (or `http://localhost:8000`).

### 📲 Set Up the Android App

1. Clone the VisionMate repository:

   ```bash
   git clone https://github.com/monikamunusamy/VisionMate-Project.git
   cd VisionMate-Project
   ```

2. Open the project in Android Studio:

   - `File -> Open` → Select the project directory.

3. Sync Gradle:

   - Make sure all Gradle dependencies sync successfully.

4. Build and Run:

   - Connect an Android device.
   - Hit the `Run` button in Android Studio.

---

## 🔄 Flask ↔️ Android Integration

The VisionMate app communicates with the Flask backend via RESTful APIs for **external camera** tasks:

### 📺 `/video_feed`

- Streams real-time camera feed with object overlays.

### 🧐 `/detected_objects`

- Method: `GET`
- Returns a JSON array of detected objects.
  Example:
  ```json
  ["hand", "bottle"]
  ```

### 🧴 `/activate_bracelet`

- Method: `POST`
- Guides the bracelet toward a selected object.
- Example request:
  ```json
  {
    "object_name": "bottle"
  }
  ```

---

---

## 📽️ Demo Video

Watch VisionMate in action!  
▶️ [Click here to view on YouTube](https://youtube.com/shorts/ovVDhRDd8_o)

Or preview below:

[![Watch the demo](https://img.youtube.com/vi/ovVDhRDd8_o/0.jpg)](https://youtube.com/shorts/ovVDhRDd8_o)

---


## 🔍 Flask Backend Details

Backend repo: [Flask_app](https://github.com/Smgunlusoy/Flask_app)

### Key Contributions and Technologies Used:

- **Flask Web Framework**: Hosts all the RESTful API endpoints.
- **YOLOv5 & YOLOv8 Integration**: Real-time object and hand detection using pre-trained models.
- **OpenCV**: Handles external camera streams and frame processing.
- **SocketIO / HTTP Streams**: Streams live video feed to the Android device.
- **Threading & Multiprocessing**: Ensures fast response during real-time processing.
- **BraceletController Module**: Custom-designed to provide tactile feedback using object coordinates.
- **Experimental Hand/Object Navigation Logic**: Detects object and hand location to guide user movement toward target objects.

---

## 👥 Contributors

| Name                      | Role & Contributions                                                                 |
| ------------------------- | ---------------------------------------------------------------------------------- |
| **Monika Munusamy** 👩‍💻 | Android App Development: CameraX integration, UI/UX design, Text-to-Speech, Navigation, and internal camera setup. |
| **Simge Ünlüsoy** 👩‍🔬   | Flask Backend Development: YOLO object detection, API design, external camera, real-time video streaming, and tactile bracelet activation. |

---

## ⚖️ License

This project is licensed under the **MIT License**. See the `LICENSE` file for details.

---

## 🤝 Contribution Guidelines

Contributions are welcome! 💪

- Fork the repo
- Create a new branch
- Commit your changes
- Submit a pull request

For big changes, open an issue to discuss first!

---

## 🙏 Credits

- **Authors**: Monika Munusamy & Simge Ünlüsoy
- **Project Origin**: Developed as part of our **Cognitive Science - Master of Science**, under the Study Project module.
- **Special thanks** to open-source libraries, academic mentors, and the accessibility tech community 💙

**, under the Study Project module.
- **Special thanks** to open-source libraries, academic mentors, and the accessibility tech community 💙

