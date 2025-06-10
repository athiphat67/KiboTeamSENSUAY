package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {

    ArrayList<DataPaper> data = new ArrayList<>();
    private ObjectDetector mainDetector;
    private boolean areModelsReady = false;

    @Override
    protected void runPlan1() {
        // StartMissions
        api.startMission();

        try {
            moveToArea1();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        try {
//            moveTo115cm();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            moveToArea4();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            moveToAstronaut();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        api.reportRoundingCompletion();

        // Shutdown
        api.shutdownFactory();

    }

    private void moveToArea1() throws IOException {
        // point 1: oasis 1 → area 1
        Point p1 = new Point(10.9d, -9.92284d, 5.195d);
        Quaternion q1 = eulerToQuaternion(0, 0, -90);
        api.moveTo(p1, q1, false);

        // point 2: oasis 2 → area 1
        Point p1_2 = new Point(11.175, -10.03, 5.245);
        Quaternion q2 = eulerToQuaternion(0, 0, -90);
        api.moveTo(p1_2, q2, true);



        DataPaper result1 = CapturePaper(1);
        Mat imgResult = result1.getCaptureImage();
        api.saveMatImage(imgResult, "imgArea_"+ 1 +".png");

        //Prediction(1);

    }

    // move in to oasis 2
    private void moveInO2() {
        Point p23in = new Point(11.150, -8.55, 5.115);
        Quaternion q23 = eulerToQuaternion(90, 0, 0);
        api.moveTo(p23in, q23, true);

    }


    private void moveTo115cm() throws IOException {
        Point p115 = new Point(11.150, -8.45, 4.912); // ห่างจากระนาบ 115 cm
        Quaternion q23 = eulerToQuaternion(90, 0, 0);
        api.moveTo(p115, q23, true);

        SystemClock.sleep(5000);

        DataPaper result2 = CapturePaper(2);
        Mat imgResult2 = result2.getCaptureImage();
        api.saveMatImage(imgResult2, "imgArea_"+ 2 +".png");

        Prediction(2);

        DataPaper result3 = CapturePaper(3);
        Mat imgResult3 = result3.getCaptureImage();
        api.saveMatImage(imgResult3, "imgArea_"+ 3 +".png");

        Prediction(3);
    }

    private void moveOutO3() {
        Point p23out = new Point(11.150, -8.35, 5.115);
        Quaternion q4 = eulerToQuaternion(-15, 0, 180); // หันออกจอ 15 deg
        //api.moveTo(p23out, q4, true);
    }


    private void moveToArea4() throws IOException {
        // point 5: oasis 4 → area 4
        Point p4 = new Point(11.1, -6.875, 4.8);
        Quaternion q4 = eulerToQuaternion(-10, 0, 180); // หันออกจอ 15 deg
        api.moveTo(p4, q4, true);

        SystemClock.sleep(2000);

        DataPaper result4 = CapturePaper(4);
        Mat imgResult4 = result4.getCaptureImage();
        api.saveMatImage(imgResult4, "imgArea_"+ 4 +".png");

        Prediction(4);

    }

    private void moveToAstronaut() throws IOException {
        Point astroPoint = new Point(11.143d, -6.7607d, 4.9654d);
        Quaternion astroQ = new Quaternion(0f, 0f, 0.707f, 0.707f); // หันไปทางขวา (y+)
        api.moveTo(astroPoint, astroQ, false);
        api.reportRoundingCompletion();
        SystemClock.sleep(1000);

        DataPaper result5 = CapturePaper(5);
        Mat imgResult5 = result5.getCaptureImage();
        api.saveMatImage(imgResult5, "imgArea_"+ 5 +".png");

        Prediction(5);
    }

    private Quaternion eulerToQuaternion(double pitchDeg, double rollDeg, double yawDeg) {
        double pitch = Math.toRadians(pitchDeg);
        double roll = Math.toRadians(rollDeg);
        double yaw = Math.toRadians(yawDeg);

        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);

        double w = cr * cp * cy + sr * sp * sy;
        double x = sr * cp * cy - cr * sp * sy;
        double y = cr * sp * cy + sr * cp * sy;
        double z = cr * cp * sy - sr * sp * cy;

        return new Quaternion((float) x, (float) y, (float) z, (float) w);
    }

    private DataPaper CapturePaper(int paper) {

        int stop = String.valueOf(paper).length(); // ถ้า paper=7 → stop=1, ถ้า paper=23 → stop=2
        int start = 0;
        int Inputpaper = paper;
        int Check_paper = paper;
        Mat warpedFlipped = new Mat();
        int arucoid = -1;
        double[] rvec_array = new double[3];
        double[] tvec_array = new double[3];


        // ---------------------------- start setup field ----------------------------
        while (start < stop) {

            float ARUCO_LEN = 0.05f;
            start++;

            // เตรียมค่าสำหรับ sharpen kernel
            float[] data = {0, -1, 0, -1, 5, -1, 0, -1, 0};
            Mat kernel = new Mat(3, 3, CvType.CV_32F);
            kernel.put(0, 0, data);

            // ดึงค่าพารามิเตอร์กล้องจาก API
            double[][] cameraParam = api.getNavCamIntrinsics();

            // สร้าง Mat สำหรับ cameraMatrix (3×3) และ dstMatrix (1×5) ด้วยชนิดข้อมูล double
            Mat cameraMatrix = new Mat(3, 3, CvType.CV_64F);
            Mat dstMatrix = new Mat(1, 5, CvType.CV_64F);
            cameraMatrix.put(0, 0, cameraParam[0]);
            dstMatrix.put(0, 0, cameraParam[1]);

            Mat Cam = api.getMatNavCam();
            api.saveMatImage(Cam, paper+"CamCheck.png");

            Mat imgUndistort = new Mat();
            Calib3d.undistort(Cam, imgUndistort, cameraMatrix, dstMatrix);
            api.saveMatImage(Cam, paper + "Undistrort.png");

            // คราวนี้แบ่งกรอบภาพซ้าย/ขวา ตามค่า paper
            int wCam = imgUndistort.cols();
            int hCam = imgUndistort.rows();
            int halfWidth = wCam / 2;

            // --------------------------------- แยกภาพ ที่ area 2 and area 3 ----------------------------------//
            Mat imgCrop;
            if (paper == 2) {
                // ซ้าย
                imgCrop = new Mat(imgUndistort, new Rect(0, 0, halfWidth, hCam));

            } else if (paper == 3) {
                // ขวา
                imgCrop = new Mat(imgUndistort, new Rect(halfWidth, 0, wCam - halfWidth, hCam));
            } else {
                imgCrop = imgUndistort.clone(); // หรืออาจจะใช้ imgUndistort ได้เลย
            }

            // สร้าง ArUco dictionary และตัวเก็บผลลัพธ์
            Dictionary Dict = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
            Mat ids = new Mat();
            List<Mat> corners = new ArrayList<>();

            // ---------------------------- Sharp ----------------------------
            // สร้าง Mat เปล่าสำหรับเก็บภาพ sharpened และ undistort
            Mat imgSharpned = new Mat();
            Imgproc.filter2D(imgCrop, imgSharpned, -1, kernel);
            // ---------------------------- Detect Markers ----------------------------
            Aruco.detectMarkers(imgSharpned, Dict, corners, ids);

            if (corners.isEmpty()) {return new DataPaper(imgCrop, false, Inputpaper, -1); }

            arucoid = (int) ids.get(0,0)[0];

            // ---------------------------- Rotation Paper ----------------------------
            //    TL --------------------------- TR
            //    |                               |
            //    |                               |
            //    |                               |
            //    |                               |
            //    BL --------------------------- BR

            int idx = 0;

            // ดึง Mat ของมาร์กเกอร์ตัวแรก (1×4×2)
            Mat selectedCorner = corners.get(idx);

            // เตรียม List สำหรับ estimatePoseSingleMarkers
            List<Mat> CornersList = new ArrayList<>();
            CornersList.add(selectedCorner);

            // เตรียมตัวเก็บ rvecs / tvecs
            Mat rvecs = new Mat();
            Mat tvecs = new Mat();
            Aruco.estimatePoseSingleMarkers(CornersList, ARUCO_LEN, cameraMatrix, dstMatrix, rvecs, tvecs);
            rvecs.get(0,0, rvec_array);
            tvecs.get(0,0,tvec_array);

            // สร้าง MatOfPoint2f เพื่อแปลง selectedCorner → Point[]
            MatOfPoint2f cornerPoints = new MatOfPoint2f(selectedCorner);
            org.opencv.core.Point[] cornerArray = cornerPoints.toArray();

            // ---------------------------- คำนวณ pixel-to-meter ----------------------------
            double pixelDistance1 = Core.norm(new MatOfPoint2f(cornerArray[0]), new MatOfPoint2f(cornerArray[1]));
            double pixelDistance2 = Core.norm(new MatOfPoint2f(cornerArray[0]), new MatOfPoint2f(cornerArray[3]));
            double pixelDistance3 = Core.norm(new MatOfPoint2f(cornerArray[1]), new MatOfPoint2f(cornerArray[2]));
            double pixelDistance4 = Core.norm(new MatOfPoint2f(cornerArray[2]), new MatOfPoint2f(cornerArray[3]));
            double pixelDistance = (pixelDistance1 + pixelDistance2 + pixelDistance3 + pixelDistance4) / 4;

            double pixelToMRatio = pixelDistance / ARUCO_LEN;

            // ---------------------------- คำนวณมุมหมุน (roll) ----------------------------
            double xTL = cornerArray[0].x;
            double yTL = cornerArray[0].y;
            double xTR = cornerArray[1].x;
            double yTR = cornerArray[1].y;

            double roll_rad = Math.atan2(yTL - yTR, xTL - xTR);
            double roll_deg = Math.toDegrees(roll_rad);

            // ---------------------------- ดึงมุมทั้ง ----------------------------
            double[] TL = selectedCorner.get(0, 0);  // [x_TL, y_TL]
            double[] TR = selectedCorner.get(0, 1);  // [x_TR, y_TR]
            double[] BR = selectedCorner.get(0, 2);  // [x_BR, y_BR]
            double[] BL = selectedCorner.get(0, 3);  // [x_BL, y_BL]

            // ---------------------------- หาจุดศูนย์กลางของมาร์กเกอร์ ----------------------------
            double center_x = (TL[0] + BR[0]) / 2.0;
            double center_y = (TL[1] + BR[1]) / 2.0;

            org.opencv.core.Point center = new org.opencv.core.Point(center_x, center_y);

            // ---------------------------- สร้าง Rotation Matrix (2×3) ----------------------------
            Mat M = Imgproc.getRotationMatrix2D(center, roll_deg, -2.0);

            // ---------------------------- เตรียมขนาดภาพและ Mat ใหม่สำหรับผลลัพธ์ ----------------------------
            int h = 960;
            int w = 1280;

            Mat imgRotation = new Mat();

            // ---------------------------- ทำ warpAffine → หมุนภาพตามมุม roll_deg และ scale = -1 ----------------------------
            Imgproc.warpAffine(imgSharpned, imgRotation, M, new org.opencv.core.Size(w, h));

            // ---------------------------- นำ kernel มา sharpen ผลลัพธ์อีกครั้ง ----------------------------
            Imgproc.filter2D(imgRotation, imgRotation, -1, kernel);
            api.saveMatImage(imgRotation, "imgBackup_" + Inputpaper + ".png");

            // ------------- imgGray--------------------
            Mat imgGray = new Mat();
            Imgproc.cvtColor(imgRotation, imgGray, Imgproc.COLOR_BayerBG2BGR);

            List<Mat> NewCornersList = new ArrayList<>();
            Mat Newids = new Mat();
            Aruco.detectMarkers(imgGray, Dict, NewCornersList, Newids);

            // -------------- TR and TL ------------------
            Mat firstCorners = NewCornersList.get(0);
            double[] tl = firstCorners.get(0,0);
            double[] tr = firstCorners.get(0,1);
            double rollRed = Math.atan2(tl[1] - tr[1], tl[0]- tr[0]);
            double rollDeg = Math.toDegrees(rollRed);

            //-------------- x2 CameraMatrix -----------------
            double scale = -2;
            Mat K_zoom = cameraMatrix.clone();
            K_zoom.put(0,0, K_zoom.get(0,0)[0] * scale);
            K_zoom.put(1,1, K_zoom.get(1,1)[0] * scale);
            K_zoom.put(0,2, K_zoom.get(0,2)[0] * scale);
            K_zoom.put(1,2, K_zoom.get(1,2)[0] * scale);

            // ------------------- 3x3 scale = 1.0 -----------------------

            double[] dims = { imgGray.cols(), imgGray.rows() };   // [width, height]
            double newCenter_x = dims[0] / 2.0;
            double newCenter_y = dims[1] / 2.0;
            org.opencv.core.Point Newcenter = new org.opencv.core.Point(newCenter_x, newCenter_y);

            Mat M2 = Imgproc.getRotationMatrix2D(Newcenter, rollDeg, 1.0);
            Mat M3 = Mat.eye(3,3, CvType.CV_64F);
            // ใส่ส่วน 2×3 ลงในแถวบนสองแถวของ M3
            for(int r=0; r<2; r++) {
                for(int c=0; c<3; c++) {
                    M3.put(r, c, M2.get(r, c)[0]);
                }
            }

            // 7. คูณ M3 @ K_zoom -> K_new ---------------------------------------------
            Mat K_new = new Mat();
            Core.gemm(M3, K_zoom, 1.0, Mat.zeros(3,3,CvType.CV_64F), 0.0, K_new);

            // 8. Estimate pose ด้วย K_new ---------------------------------------------
            Mat Newrvecs = new Mat(), Newtvecs = new Mat();
            Aruco.estimatePoseSingleMarkers(
                    NewCornersList, 0.05f,
                    K_new, dstMatrix,
                    Newrvecs, Newtvecs
            );

            // ดึง rvec, tvec แรก
            Mat Newrvec = Newrvecs.row(0).reshape(1,3);
            Mat Newtvec = Newtvecs.row(0).reshape(1,3);

            // 9. นิยามมุมกระดาษในโลก (object points) -------------------------------
            double paperW = 0.30, paperH = 0.23;
            double offX = 0.05, offY = 0.055;
            MatOfPoint3f objectPoints = new MatOfPoint3f(
                    new Point3(offX,             offY,             0.0), // TL
                    new Point3(offX - paperW,    offY,             0.0), // TR
                    new Point3(offX - paperW,    offY - paperH + 0.01,0.0), // BR
                    new Point3(offX,             offY - paperH + 0.05,0.0)  // BL
            );

            // 10. Project 3D -> 2D ด้วย K_new ----------------------------------------
            MatOfDouble distCoeffs = new MatOfDouble(-0.164787, 0.020375, -0.001572, -0.000369, 0);
            MatOfPoint2f imagePts = new MatOfPoint2f();
            Calib3d.projectPoints(
                    objectPoints,
                    Newrvec,
                    Newtvec,
                    K_new,
                    distCoeffs,
                    imagePts
            );
            org.opencv.core.Point[] srcPtsArr = imagePts.toArray();

            // 11. เตรียม dst pts และคำนวณ Homography -------------------------------
            MatOfPoint2f dstPts = new MatOfPoint2f(
                    new org.opencv.core.Point(0,0),
                    new org.opencv.core.Point(539,0),
                    new org.opencv.core.Point(539,299),
                    new org.opencv.core.Point(0,299)
            );
            Mat H = Calib3d.findHomography(new MatOfPoint2f(srcPtsArr), dstPts);

            // 12. Warp Perspective และ flip ---------------------------------------
            Mat warped = new Mat();
            Imgproc.warpPerspective(imgGray, warped, H, new Size(540,300));
            warpedFlipped = warped;
            Core.flip(warped, warpedFlipped, 1);

        }

        return new DataPaper(warpedFlipped, true, Inputpaper, arucoid, rvec_array, tvec_array);
    }

    private void Prediction(int area) throws IOException {
        // Log เพื่อบอกจุดเริ่มต้นของกระบวนการสำหรับ Area ที่ระบุ
        Log.i("PredictionFlow", "===== Starting Prediction for Area: " + area + " =====");

        String pathFile = getImagePath(area);
        File ImgFile = new File(pathFile);
        File modelFile = getModelFile();

        // ตรวจสอบว่า getModelFile ทำงานสำเร็จและได้ไฟล์โมเดลมาจริงหรือไม่
        if (modelFile == null) {
            Log.e("PredictionFlow", "Model file is NULL. Cannot create detector. Aborting prediction.");
            return;
        }

        Bitmap bitmap;

        List<FinalAreaResult> allResultsInArea = new ArrayList<>();
        List<Detection> results;
        float threshold = 0.5f;
        ObjectDetector.ObjectDetectorOptions options = modelSetting(threshold);
        Log.i("PredictionFlow", "Success Create options");
        ObjectDetector detector = ObjectDetector.createFromFileAndOptions(modelFile, options);

        // Log เพื่อยืนยันว่าสร้าง Detector สำเร็จแล้ว
        Log.i("PredictionFlow", "Detector created for model: " + modelFile.getName());

        if (!ImgFile.exists()) {
            // Log Error เมื่อหาไฟล์ภาพที่จะใช้ทำนายผลไม่เจอ
            Log.i("PredictionFlow", "Image file does NOT exist at: " + pathFile);
            return;
        } else {
            // Log ยืนยันว่าเจอไฟล์ภาพและกำลังจะโหลด
            Log.i("PredictionFlow", "Image file found. Loading bitmap...");
            bitmap = LoadBitMapFromSDCard(pathFile);

            // ตรวจสอบว่าโหลด Bitmap สำเร็จหรือไม่
            if (bitmap == null) {
                Log.e("PredictionFlow", "Failed to load bitmap from path: " + pathFile);
                return;
            }
            // Log ยืนยันว่าโหลด Bitmap สำเร็จ
            Log.i("PredictionFlow", "Bitmap loaded successfully. Preparing for detection...");

            TensorImage imgTensor = TensorImage.fromBitmap(bitmap);
            results = detector.detect(imgTensor);

            // Log เพื่อบอกว่าการทำนายผลเสร็จสิ้น และกำลังจะเข้าสู่ขั้นตอนประมวลผล
            Log.i("PredictionFlow", "Detection complete. Processing results...");
            processResultOfDetection(results, bitmap, area, threshold, allResultsInArea, options);
        }

        // Log เพื่อบอกว่าทุกขั้นตอนสำหรับ Area นี้เสร็จสิ้นแล้ว
        Log.i("PredictionFlow", "===== Finished Prediction for Area: " + area + " =====");
    }

    private List<FinalAreaResult> processResultOfDetection(List<Detection> results,  Bitmap bitmap,
                                          int area, float threshold, List<FinalAreaResult> allResultsInArea,
                                          ObjectDetector.ObjectDetectorOptions options) throws IOException{
        // Log เพื่อติดตามว่าโปรแกรมได้เข้ามาทำงานในเมธอดนี้แล้ว
        Log.i("PredictionFlow", "Now in processResultOfDetection for Area: " + area);

        List<FinalAreaResult> finalAreaResults = new ArrayList<>();

        if (results.isEmpty()) {
            // Log เป็น Warning เมื่อโมเดลทำงานแต่ไม่เจอวัตถุใดๆ
            Log.i("PredictionFlow", "Result list is EMPTY. No objects detected.");
        } else {
            Log.i("PredictionFlow", "Result list has " + results.size() + " items. Looping through results...");

            int itemNumber = 1;
            for (Detection detectedObject : results) {
                Category topCategory = detectedObject.getCategories().get(0);
                String label = topCategory.getLabel();
                float score = topCategory.getScore();
                RectF box = detectedObject.getBoundingBox();

                // Log รายละเอียดของแต่ละวัตถุที่เจอ
                Log.i("PredictionFlow", "--- Item #" + itemNumber + " ---");
                Log.i("PredictionFlow", "Label: " + label + ", Score: " + score);

                itemNumber++;
            }
        }

        return finalAreaResults;
    }

    private ObjectDetector.ObjectDetectorOptions modelSetting(float threshold) {
        return ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .build();
    }

    private File getModelFile() throws IOException {
        String nameModel = "SensuayV2.tflite";
        // Log เพื่อดูว่ากำลังร้องขอโมเดลชื่ออะไร
        Log.i("PredictionFlow", "Requesting model file: " + nameModel);
        return convertModelFileFromAssetsToTempFile(nameModel);
    }

    private File convertModelFileFromAssetsToTempFile(String modelFileName) {
        try {
            InputStream inputStream = getAssets().open(modelFileName);
            File tempFile = File.createTempFile(modelFileName, null);
            tempFile.deleteOnExit();

            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4 * 1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // Log ยืนยันว่าการคัดลอกไฟล์โมเดลสำเร็จ
            Log.i("PredictionFlow", "Model '" + modelFileName + "' copied to temp file successfully.");
            return tempFile;

        } catch (IOException e) {
            // Log Error ให้เห็นชัดเจนเมื่อคัดลอกไฟล์ล้มเหลว
            Log.i("PredictionFlow", "Failed to copy model from assets: " + modelFileName, e);
            return null;
        }
    }

    private String getImagePath(int numImg) {
        String path = "sdcard/data/jp.jaxa.iss.kibo.rpc.thailand/immediate/DebugImages/imgArea_" + numImg + ".png";
        // Log เพื่อดู Path ของรูปภาพที่ถูกสร้างขึ้น
        Log.i("PredictionFlow", "Generated image path: " + path);
        return path;
    }

    private String getBackupImage(int numImg) {
        Log.i("PredictionFlow", "WARNING: Calling for a backup image for area " + numImg);
        return "sdcard/data/jp.jaxa.iss.kibo.rpc.thailand/immediate/DebugImages/imgBackup_" + numImg + ".png";
    }

    public static Bitmap LoadBitMapFromSDCard(String path) {
        File imgFile = new File(path);
        if (imgFile.exists()) {
            // Log เพื่อยืนยันว่ากำลังโหลด Bitmap จากไฟล์ที่เจอ
            Log.i("PredictionFlow", "Loading bitmap from existing file: " + path);
            return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        }
        // Log Error เมื่อหาไฟล์ที่จะโหลดไม่เจอ
        Log.i("PredictionFlow", "Bitmap file does NOT exist at: " + path);
        return null;
    }

    private void HandleErrorPrediction(int numImg) {

    }
}
