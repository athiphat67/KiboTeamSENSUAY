# 🤖 The 6th Kibo Robot Programming Challenge 
![IntroduceTeam](https://github.com/user-attachments/assets/90ccb959-8ce3-460b-b883-cdb2786649d0)

## We are SENSUAY! this name stand  from "Theepop Noodle Shop".

## 🚀 Project Overview

The "The 6th Kibo Robot Programming Challenge" project by **SENSUAY Team** aims to develop an Astrobee robot control program, entirely written in **Java**. Our core mission is twofold:

1.  **Advanced Image Processing:** Utilize sophisticated image analysis capabilities from Astrobee's camera to precisely locate hidden "treasures" within the Kibo module of the International Space Station (ISS).
2.  **Efficient Path Planning:** Develop an optimal path planning strategy to efficiently retrieve these treasures, critically considering the time constraints for Astrobee's mission operations.

We are committed to delivering a robust and accurate solution to effectively achieve the objectives of the Kibo RPC 6th competition.

## ✨ Key Features

Our project stands out with the following key features, developed using Java and its associated libraries:

-   **Machine Learning-powered Object Detection:**
    * Leverages the capabilities of **Ultralytics** (e.g., YOLOv8 models) for image analysis to quickly and accurately identify and locate the hidden treasures.
    * *(Optional: If you've integrated the model into Java in a specific way, e.g., "via ONNX Runtime for Java" or "converted for TensorFlow Lite," you can specify here.)*
-   **Advanced Image Processing with OpenCV:**
    * Employs the **OpenCV** library for various image processing tasks, including image enhancement, object segmentation, and spatial position calculation, crucial for navigation and identification.
-   **Optimized Path Planning and Navigation:**
    * Developed intelligent path planning mechanisms to enable Astrobee to move to treasure locations rapidly and with maximum efficiency, while effectively avoiding obstacles and adhering to mission time limits.
-   **Seamless Kibo RPC Simulator API Integration:**
    * Our Astrobee Agent code is meticulously designed to communicate with and control Astrobee within the simulated environment through the **Kibo RPC Simulator API**, ensuring precise and responsive actions.

## 🛠️ Technologies Used

This project has been developed using the following core technologies:

-   **Programming Language:** Java
-   **Image Processing Library:** OpenCV
-   **Machine Learning Library/Framework:** Ultralytics (with its models integrated for Java usage)
-   **API:** Kibo RPC Simulator API
-   **Development Environment (IDE):** Recommended IntelliJ IDEA or Eclipse

## ⚙️ Installation and Setup

To get our project up and running on your machine, please follow these steps:

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/your-github-username/your-repo-name.git](https://github.com/your-github-username/your-repo-name.git)
    cd your-repo-name
    ```
    *(Note: Replace `your-github-username/your-repo-name` with your actual GitHub repository link.)*

2.  **Install Java Development Kit (JDK):**
    * Ensure you have a suitable JDK version installed on your system (JDK 11 or higher is recommended).

3.  **Set up Kibo RPC Simulator:**
    * Download and install the Kibo RPC Simulator according to the official instructions from the JAXA Kibo RPC website.
    * Familiarize yourself with the Simulator's structure and how to load your Agent code (typically a `.jar` file).

4.  **Set up Project in your IDE (e.g., IntelliJ IDEA or Eclipse):**
    * Open this project in a Java-compatible IDE (e.g., import as a Gradle or Maven Project if you are using them).
    * **Add `OpenCV` and `Ultralytics` libraries to your Project dependencies:**
        * **For OpenCV:** Download the OpenCV SDK for Java (e.g., `opencv-<version>-java.zip`) and add the JAR file (e.g., `opencv-<version>.jar`) along with its native libraries (.dll/.so/.dylib) to your project's Build Path.
        * **For Ultralytics:** If you are using Ultralytics models converted to a Java-compatible format (e.g., ONNX or TensorFlow Lite), you will need to:
            * Download the converted model files (e.g., `.onnx` or `.tflite`).
            * Add the appropriate Java runtime library (e.g., ONNX Runtime for Java or TensorFlow Lite for Java) to your Project dependencies.
            * Ensure the model's path is correctly specified in your code.
    * Verify your Build Path settings and resolve any library-related errors.

5.  **Compile and Integrate Astrobee Agent with Simulator:**
    * Compile your Java project into an executable `.jar` file.
    * Place your compiled Agent's `.jar` file into the correct directory within the Kibo RPC Simulator (typically `/path/to/kibo_rpc_simulator/agents/`).

## 🚀 How to Run

Once your project is set up and the Agent `.jar` file is placed in the Kibo RPC Simulator:

1.  Launch the Kibo RPC Simulator.
2.  Navigate to the Agent settings section and select "SENSUAY Agent" (or whatever name you configured for your Agent).
3.  Start the mission within the Simulator.
4.  Our SENSUAY Team's Astrobee Agent program will begin its operation, processing camera images, identifying treasures, and executing the pre-programmed retrieval plan.

## 📊 Results / Demonstration (Optional)

If you have screenshots, GIFs, or short videos showcasing your Astrobee Agent's performance in the Simulator, you can add them here to demonstrate your project's effectiveness.

![Screenshot of Astrobee in Simulator](images/screenshot1.png)
*Our Astrobee identifying treasures within the Kibo module.*

*(Note: Replace `images/screenshot1.png` with your actual image path or remove this section if you don't have visuals.)*

## 👥 Team Members

-   **Athiphat Soonsit** - Lead Developer, AI/ML Integration
-   **Purich Ampawa** - Robotics Engineer, Path Planning & Navigation
-   **thepop aawd** - Vision Systems Specialist, System Integration

*(Note: If you have GitHub profiles for each member, you can add links like `[GitHub Profile](https://github.com/username)` next to their names.)*

## 📄 License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.

*(Note: Make sure to create a `LICENSE.md` file in the root of your repository and paste the full MIT License text inside it.)*

## 🙏 Acknowledgements

We extend our sincere thanks to:

-   The organizing committee of The 6th Kibo Robot Programming Challenge for this invaluable opportunity.
-   JAXA for developing the Kibo RPC Simulator and providing essential technical resources.
-   [If applicable, any mentors, professors, or significant external resources you'd like to acknowledge.]
