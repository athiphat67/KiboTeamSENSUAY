# 🤖 The 6th Kibo Robot Programming Challenge 
![IntroduceTeam](https://github.com/user-attachments/assets/90ccb959-8ce3-460b-b883-cdb2786649d0)

## We are SENSUAY! this name stand  from "Theepop's Noodle Shop"
## 👥 Team Members

-   **Athiphat Soonsit** - Team leader (Operation planing, Image processing, AI Machine Learning)
-   **Purich Ampawa** - AI Machine Learning Model, Path Planning & Navigation
-   **Theepop Rattanasubsiri** - Dataset Preparer for AI Machine Learning, Path Planning & Navigation

## 🛰️ Kibo-RPC 6th Mission Overview
**Mission Brief: The Space Treasure Hunt**

For Kibo-RPC #6, competitors will be given an exciting simulated mission: "Space pirates have hidden treasure in the Kibo module, and the astronauts have clues but don't know the exact location." Participating teams will need to write programs to control the Astrobee free-flying robot, a robot actually used on the ISS, to complete the following tasks:

- **Explore the Area:** Astrobee must move to designated observation points throughout the Kibo module, where fake treasures may be scattered.
- **Collect Data and Report:** Record data and report findings at each explored point.
- **Analyze Clues:** Use clues provided by astronauts during the competition to identify the true treasure's location.
- **Search and Photograph:** Travel to the true treasure's location and take a confirmation photo.
- **Activate Signal**: Activate a light signal to inform the astronauts of the treasure's location, completing the mission.

## 🚀 Project overview
we split the whole project into 4 parts. 
First part is Path planning. In this part we search for the best route for saving time.
Secomd is image processing. In this part we use Opencv libraries for image processing.
Third part is machine learning model creation. In this part we do datasets preparing and machine learning model training. 
we train our model to be able to detect items correctly(we use yolov8n). 
the last part is reporting route planning. this part we search for method that will make reportation correct according to the rules.


### First part: Path Navigation
- Developed using **Android Studio** for building and testing the navigation logic.
- Two navigation strategies were considered:
  1. Maximize the number of Oasis Zone passes.
  2. Minimize travel time.
- After simulation tests, the fastest path yielded the highest score, so it was selected for the final submission.

### Second part: Image Processing

### 🤖 Third Part: Machine Learning Object Detection Pipeline

We created a custom dataset of 2,500 annotated images using **LabelImg**, which allowed us to define the objects for training. While our initial goal was to train directly with **TensorFlow Lite**, we faced technical limitations during the training process.

As a solution, we switched to using **YOLOv8n**, a lightweight and efficient model. After training, we successfully **converted the model to TensorFlow Lite (.tflite)** format for deployment on our Android application.

### Fourth part:
In this pahse we adapt tvec values which we get from Opencv to be correspond with axis in the simulation. After the tvec is correspond with simulation's axis, we use vector translation to get the reporting point in 3d space then move astrobee to be within that point. Eventually the part of reporting is finishes

    
