package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import gov.nasa.arc.astrobee.Kinematics;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import gov.nasa.arc.astrobee.Result;

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
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.util.HashMap;
import java.util.Map;
import java.lang.Math;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {

    ArrayList<List<Map<String, Object>>> resultList = new ArrayList<>();
    ArrayList<DataPaper> ListDataPaper = new ArrayList<>();

    // enum class ระบุชื่อจุด
    private enum MissionTarget {
        PLAN2_CAP_A1,
        PLAN2_CAP_A23,
        PLAN2_CAP_A4,
        PLAN2_ASTRO_POS
    }

    // Maps เก็บตำแหน่งและทิศทาง
    private Map<MissionTarget, Point> targetPositions;
    private Map<MissionTarget, Quaternion> targetOrientations;

    // Constructor ที่สร้างใให้ Map มีการเก็บข้อมูลแบบ Hashmap() ลองศึกษาดูนิดนึงก็ได้หรือปล่อยผ่าน
    public YourService() {
        targetPositions = new HashMap<>();
        targetOrientations = new HashMap<>();
    }

    // method ที่ไว้ใช้ในการเดินทาง
    // position คือ point ที่เดินทางไป
    // orientation คือ มุมหรือการหมุนของ astrobee (ให้ astrobee หมุนกล้องไปด้านที่ถูก)
    // api.moveTo เป็น api ของ kibo (printRobotPosition คืออะไรไม่รู้)
    public boolean moveToArea(Point position, Quaternion orientation) throws IOException {
        Result moveResult = api.moveTo(position, orientation, false);
        int loopCount = 0;
        while (!moveResult.hasSucceeded() && loopCount < 3) {
            Log.w("MoveToArea", "Move failed, retrying... Attempt: " + (loopCount + 1));
            moveResult = api.moveTo(position, orientation, false);
            loopCount++;
        }
        if (!moveResult.hasSucceeded()) {
            Log.e("MoveToWrapper", "Move failed after multiple retries to: " + position.toString());
            return false;
        }
        return true;
    }

    // แปลงจากองศารอบแกนหมุนเป็น Quaternion (เป็นสูตรเฉยๆ)
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

    @Override
    protected void runPlan1() {
        // StartMissions
        api.startMission();

        // Add จุดใหม่ไปที่ enum ของชื่อจุดที่สร้างไว้
        // Position (x,y,x)
        targetPositions.put(MissionTarget.PLAN2_CAP_A1, new Point(11.15, -9.5, 4.9645));
        targetPositions.put(MissionTarget.PLAN2_CAP_A23, new Point(11.15, -8.45, 4.9645));
        targetPositions.put(MissionTarget.PLAN2_CAP_A4, new Point(11.143, -6.8525, 4.9645));
        targetPositions.put(MissionTarget.PLAN2_ASTRO_POS, new Point(11.143, -6.8525, 4.9645));

        // Add องศารอบแกนหมุนไปที่ชื่อจุด
        // Quaternion (pitch,roll,yaw)

        targetOrientations.put(MissionTarget.PLAN2_CAP_A1, eulerToQuaternion(-15, 0,-80));
        targetOrientations.put(MissionTarget.PLAN2_CAP_A23, eulerToQuaternion(90,0,0));
        targetOrientations.put(MissionTarget.PLAN2_CAP_A4, eulerToQuaternion(-5,0,180));
        targetOrientations.put(MissionTarget.PLAN2_ASTRO_POS, eulerToQuaternion(0,0,90));


        // move astrobee ไปที่จุดบน oasis 2 พร้อมหมุน astrobee แล้วถ่ายภาพ
        try {
            Log.i("Mission", "Moving to Area 1 Capture Position...");
            moveToArea(targetPositions.get(MissionTarget.PLAN2_CAP_A1), targetOrientations.get(MissionTarget.PLAN2_CAP_A1));
            DataPaper result1 = CapturePaper(1, targetOrientations.get(MissionTarget.PLAN2_CAP_A1));

            Mat imgResult = result1.getCaptureImage();
            api.saveMatImage(imgResult, "imgArea_" + 1 + ".png");
            ListDataPaper.add(result1);

            SystemClock.sleep(2000);

            ObjectDetector detector = new ObjectDetector(this);
            resultList.add(detector.processImage(result1));

        } catch (IOException e) {
            e.printStackTrace();
        }

        // area 2,3 capture
        // move astrobee ไปที่จุดกึ่งกลางระหว่าง oasis และ เคลื่อนที่ขึ้นตามแนวแกน -z เพื่อถ่ายรูปห่างจากระนาบเป็นระยา ~115cm
        try {
            Log.i("Mission", "Moving to Area 2,3 Capture Position...");
            moveToArea(targetPositions.get(MissionTarget.PLAN2_CAP_A23), targetOrientations.get(MissionTarget.PLAN2_CAP_A23));
            ObjectDetector detector = new ObjectDetector(this);

            // delay astrobee ค้างไว้ 4000 millisecond==4 sec เพื่อเช็คให้ชัวร์ว่านิ่งจริงๆแล้วค่อยถ่ายภาพ
            SystemClock.sleep(3000);

            DataPaper result2 = CapturePaper(2, targetOrientations.get(MissionTarget.PLAN2_CAP_A23));
            ListDataPaper.add(result2);
            Mat imgResult2 = result2.getCaptureImage();
            api.saveMatImage(imgResult2, "imgArea_" + 2 + ".png");
            resultList.add(detector.processImage(result2));

            DataPaper result3 = CapturePaper(3, targetOrientations.get(MissionTarget.PLAN2_CAP_A23));
            ListDataPaper.add(result3);
            Mat imgResult3 = result3.getCaptureImage();
            api.saveMatImage(imgResult3, "imgArea_" + 3 + ".png");
            resultList.add(detector.processImage(result3));

        } catch (IOException e) {
            e.printStackTrace();
        }


        // move to area 4
        // move astrobee เข้า area 4 (ยังอยู่ใน oasis 4) แล้วกดกล้องลง ~10 degree (ลองไปดูที่ orientation)
        try {
            Log.i("Mission", "Moving to Area 4 Capture Position...");
            moveToArea(targetPositions.get(MissionTarget.PLAN2_CAP_A4), targetOrientations.get(MissionTarget.PLAN2_CAP_A4));
            ObjectDetector detector = new ObjectDetector(this);

            SystemClock.sleep(2000);

            DataPaper result4 = CapturePaper(4, targetOrientations.get(MissionTarget.PLAN2_CAP_A4));
            ListDataPaper.add(result4);
            Mat imgResult4 = result4.getCaptureImage();
            api.saveMatImage(imgResult4, "imgArea_" + 4 + ".png");
            resultList.add(detector.processImage(result4));

        } catch (IOException e) {
            e.printStackTrace();
        }

        // move to astronaut
        try {
            Log.i("Mission", "Moving to Astronaut Interaction Position...");
            moveToArea(targetPositions.get(MissionTarget.PLAN2_ASTRO_POS), targetOrientations.get(MissionTarget.PLAN2_ASTRO_POS));

            ReportAllArea(resultList);
            api.reportRoundingCompletion();

            SystemClock.sleep(5000);

            DataPaper result5 = CapturePaper(5, targetOrientations.get(MissionTarget.PLAN2_ASTRO_POS));
            ListDataPaper.add(result5);
            Mat imgResult5 = result5.getCaptureImage();
            api.saveMatImage(imgResult5, "imgArea_" + 5 + ".png");

            ObjectDetector detector = new ObjectDetector(this);
            resultList.add(detector.processImage(result5));

        } catch (IOException e) {
            e.printStackTrace();
        }

        api.notifyRecognitionItem();

        int i = 1;
        for (DataPaper obj : ListDataPaper) {
            double[] rvec = obj.getRvec();
            double[] tvec = obj.getTvec();
            Log.i("Rvec", "rvec" + i + " : " + rvec[0] + " , " + rvec[1] + " , " + rvec[2]);
            Log.i("Tvec", "tvec" + i + " : " + tvec[0] + " , " + tvec[1] + " , " + tvec[2]);
            i++;
        }

        //move to targetArea
        int NumberResultPaper = FindPaperOfTargetItems();
        Log.i("MoveToTargetArea", "NUmberResultePaper: " + NumberResultPaper);
        DataPaper resultPaper = ListDataPaper.get(NumberResultPaper - 1);
        Log.i("MoveToTargetArea", "resultPaper :" + resultPaper.getPaperNumber());
        try {
            moveToReportArea(NumberResultPaper, resultPaper);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Shutdown
        api.shutdownFactory();

    }

    private DataPaper CapturePaper(int paper, Quaternion quaternionNow) {

        int Inputpaper = paper;
        Mat warpedFlipped;
        int arucoid;
        double[] rvec_array = new double[3];
        double[] tvec_array = new double[3];
        Mat imgRotation; // Declare outside loop to retain value
        Mat imgBackup = new Mat(); // Declare imgBackup here as well
        float ARUCO_LEN = 0.05f;

        // rvec tvec
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
        Dictionary Dict = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);

        // ------------------------- Img Undistort --------------------------------------------- //
        Mat imgUndistort = new Mat();
        Calib3d.undistort(Cam, imgUndistort, cameraMatrix, dstMatrix);

        // --------------------------- keep Rvec Tvec ------------------------------------------//

        Mat keepids = new Mat();
        List<Mat> keepcorners = new ArrayList<>();

        Mat keeprvecs = new Mat();
        Mat keeptvecs = new Mat();

        if (Inputpaper == 2) {

            Mat leftImg = blackOutHalfImage(Cam, 2);
            Aruco.detectMarkers(leftImg, Dict, keepcorners, keepids);
            Aruco.estimatePoseSingleMarkers(keepcorners, ARUCO_LEN, cameraMatrix, dstMatrix, keeprvecs, keeptvecs);

        } else if (Inputpaper == 3) {

            Mat rightImg = blackOutHalfImage(Cam, 3);
            Aruco.detectMarkers(rightImg, Dict, keepcorners, keepids);
            Aruco.estimatePoseSingleMarkers(keepcorners, ARUCO_LEN, cameraMatrix, dstMatrix, keeprvecs, keeptvecs);

        } else {
            Aruco.detectMarkers(Cam, Dict, keepcorners, keepids);
            Aruco.estimatePoseSingleMarkers(keepcorners, ARUCO_LEN, cameraMatrix, dstMatrix, keeprvecs, keeptvecs);

            api.saveMatImage(Cam, "imgNormal_" + Inputpaper + ".png");

        }

        keeprvecs.get(0, 0, rvec_array);
        keeptvecs.get(0, 0, tvec_array);

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
        Mat ids = new Mat();
        List<Mat> corners = new ArrayList<>();

        // ---------------------------- Sharp ----------------------------
        // สร้าง Mat เปล่าสำหรับเก็บภาพ sharpened และ undistort
        Mat imgSharpned = new Mat();
        Imgproc.filter2D(imgCrop, imgSharpned, -1, kernel);
        // ---------------------------- Detect Markers ----------------------------
        Aruco.detectMarkers(imgSharpned, Dict, corners, ids);

        arucoid = (int) ids.get(0, 0)[0];

        // ---------------------------- Rotation Paper ----------------------------
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

        // ---------------------------- ดึงมุมทั้ง 4 ----------------------------
        double[] TL = selectedCorner.get(0, 0);  // [x_TL, y_TL]
        double[] TR = selectedCorner.get(0, 1);  // [x_TR, y_TR]
        double[] BR = selectedCorner.get(0, 2);  // [x_BR, y_BR]
        double[] BL = selectedCorner.get(0, 3);  // [x_BL, y_BL]

        // ---------------------------- หาจุดศูนย์กลางของมาร์กเกอร์ ----------------------------
        double center_x = (TL[0] + BR[0]) / 2.0;
        double center_y = (TL[1] + BR[1]) / 2.0;

        org.opencv.core.Point center = new org.opencv.core.Point(center_x, center_y);

        // ---------------------------- สร้าง Rotation Matrix (2×3) ----------------------------
        // ยังคงใช้ -2.0 ตามคำขอครั้งก่อน
        Mat M = Imgproc.getRotationMatrix2D(center, roll_deg, -2.0);

        // ---------------------------- คำนวณขนาดภาพปลายทางที่เหมาะสมหลังการหมุน ----------------------------
        double originalWidth = imgSharpned.cols();
        double originalHeight = imgSharpned.rows();

        double absCos = Math.abs(Math.cos(Math.toRadians(roll_deg)));
        double absSin = Math.abs(Math.sin(Math.toRadians(roll_deg)));

        int newWidth = (int) Math.round(originalHeight * absSin + originalWidth * absCos);
        int newHeight = (int) Math.round(originalHeight * absCos + originalWidth * absSin);

        M.put(0, 2, M.get(0, 2)[0] + (newWidth / 2) - center_x);
        M.put(1, 2, M.get(1, 2)[0] + (newHeight / 2) - center_y);

        imgRotation = new Mat();
        Imgproc.warpAffine(imgSharpned, imgRotation, M, new org.opencv.core.Size(newWidth, newHeight), Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, new Scalar(0, 0, 0));

        // ---------------------------- นำ kernel มา sharpen ผลลัพธ์อีกครั้ง ----------------------------
        Imgproc.filter2D(imgRotation, imgRotation, -1, kernel);

        // ---------------------------- ส่วนที่เพิ่มเข้ามาสำหรับการซูม imgBackup 1.3 เท่า ----------------------------
        // คำนวณจุดศูนย์กลางของ Aruco marker ในภาพ imgRotation
        Mat tempImgGray = new Mat();
        if (imgRotation.channels() == 3) {
            Imgproc.cvtColor(imgRotation, tempImgGray, Imgproc.COLOR_BGR2GRAY);
        } else {
            imgRotation.copyTo(tempImgGray);
        }

        List<Mat> currentCorners = new ArrayList<>();
        Mat currentIds = new Mat();
        Aruco.detectMarkers(tempImgGray, Dict, currentCorners, currentIds);

        if (!currentCorners.isEmpty()) {
            Mat arucoCorner = currentCorners.get(0); // เอา Aruco marker ตัวแรก
            // หาจุดศูนย์กลางของ Aruco marker ในภาพ imgRotation
            double current_aruco_center_x = (arucoCorner.get(0, 0)[0] + arucoCorner.get(0, 2)[0]) / 2.0;
            double current_aruco_center_y = (arucoCorner.get(0, 0)[1] + arucoCorner.get(0, 2)[1]) / 2.0;

            // *** ปรับการคำนวณขนาด Crop สำหรับการซูม 1.3 เท่า ***
            double zoomFactor = 1.3;
            int targetOutputSize = 640; // ขนาดสุดท้ายของ imgBackup

            int cropWidth = (int) Math.round(targetOutputSize / zoomFactor);
            int cropHeight = (int) Math.round(targetOutputSize / zoomFactor);
            // ตรวจสอบให้แน่ใจว่า cropWidth และ cropHeight เป็นคู่
            cropWidth = (cropWidth % 2 == 0) ? cropWidth : cropWidth + 1;
            cropHeight = (cropHeight % 2 == 0) ? cropHeight : cropHeight + 1;


            // คำนวณตำแหน่งเริ่มต้นของการ Crop
            int x = (int) (current_aruco_center_x - cropWidth / 2);
            int y = (int) (current_aruco_center_y - cropHeight / 2);

            // ตรวจสอบขอบเขตของการ Crop ไม่ให้เกินภาพ imgRotation
            x = Math.max(0, x);
            y = Math.max(0, y);

            // ปรับขนาด cropWidth/Height ให้ไม่เกินขอบภาพ แต่พยายามรักษาสัดส่วน
            int actualCropWidth = Math.min(cropWidth, imgRotation.cols() - x);
            int actualCropHeight = Math.min(cropHeight, imgRotation.rows() - y);

            // หากพื้นที่ Crop ที่เป็นไปได้เล็กกว่าที่คำนวณได้มาก ให้ถอยกลับไปใช้ขนาดเต็มแล้ว resize
            if (actualCropWidth <= 0 || actualCropHeight <= 0 || actualCropWidth < (cropWidth * 0.8) || actualCropHeight < (cropHeight * 0.8)) {
                System.out.println("Warning: Crop area too small for 1.3x zoom or invalid dimensions. Falling back to normal resize for imgBackup.");
                Imgproc.resize(imgRotation, imgBackup, new Size(targetOutputSize, targetOutputSize), 0, 0, Imgproc.INTER_LINEAR);
            } else {
                Rect cropRect = new Rect(x, y, actualCropWidth, actualCropHeight);
                Mat croppedImage = new Mat(imgRotation, cropRect);
                Imgproc.resize(croppedImage, imgBackup, new Size(targetOutputSize, targetOutputSize), 0, 0, Imgproc.INTER_LINEAR);
                croppedImage.release(); // ปล่อย Mat ที่ถูก Crop เพื่อประหยัดหน่วยความจำ
            }
        } else {
            // ถ้าไม่พบ Aruco marker ใน imgRotation หลังการหมุน ก็ไม่สามารถซูมตาม marker ได้
            // จึงใช้การ resize แบบเดิม
            System.out.println("Warning: Aruco marker not found in imgRotation for zoom. Falling back to normal resize for imgBackup.");
            Imgproc.resize(imgRotation, imgBackup, new Size(640, 640), 0, 0, Imgproc.INTER_LINEAR);
        }
        // --------------------------------------------------------------------------------------------------

        api.saveMatImage(imgBackup, "ImgBackup_" + Inputpaper + ".png");

        Mat imgGray = new Mat();
        if (imgRotation.channels() == 3) {
            Imgproc.cvtColor(imgRotation, imgGray, Imgproc.COLOR_BGR2GRAY);
        } else {
            imgRotation.copyTo(imgGray);
        }


        List<Mat> NewCornersList = new ArrayList<>();
        Mat Newids = new Mat();
        Aruco.detectMarkers(imgGray, Dict, NewCornersList, Newids);


        // -------------- TR and TL ------------------
        Mat firstCorners = NewCornersList.get(0);
        double[] tl = firstCorners.get(0, 0);
        double[] tr = firstCorners.get(0, 1);
        double rollRed = Math.atan2(tl[1] - tr[1], tl[0] - tr[0]);
        double rollDeg = Math.toDegrees(rollRed);

        //-------------- x2 CameraMatrix -----------------
        // การใช้ scale ที่เป็นบวก 2.0 สำหรับ K_zoom ตามที่ตกลงกันไว้
        double K_zoom_scale = 2.0;
        Mat K_zoom = cameraMatrix.clone();
        K_zoom.put(0, 0, K_zoom.get(0, 0)[0] * K_zoom_scale);
        K_zoom.put(1, 1, K_zoom.get(1, 1)[0] * K_zoom_scale);
        K_zoom.put(0, 2, K_zoom.get(0, 2)[0] * K_zoom_scale);
        K_zoom.put(1, 2, K_zoom.get(1, 2)[0] * K_zoom_scale);

        // ------------------- 3x3 scale = 1.0 -----------------------

        double[] dims = {imgGray.cols(), imgGray.rows()};
        double newCenter_x = dims[0] / 2.0;
        double newCenter_y = dims[1] / 2.0;
        org.opencv.core.Point Newcenter = new org.opencv.core.Point(newCenter_x, newCenter_y);

        Mat M2 = Imgproc.getRotationMatrix2D(Newcenter, rollDeg, 1.0);
        Mat M3 = Mat.eye(3, 3, CvType.CV_64F);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                M3.put(r, c, M2.get(r, c)[0]);
            }
        }

        // 7. คูณ M3 @ K_zoom -> K_new ---------------------------------------------
        Mat K_new = new Mat();
        Core.gemm(M3, K_zoom, 1.0, Mat.zeros(3, 3, CvType.CV_64F), 0.0, K_new);

        // 8. Estimate pose ด้วย K_new ---------------------------------------------
        Mat Newrvecs = new Mat(), Newtvecs = new Mat();
        Aruco.estimatePoseSingleMarkers(
                NewCornersList, 0.05f,
                K_new, dstMatrix,
                Newrvecs, Newtvecs
        );

        // ดึง rvec, tvec แรก
        Mat Newrvec = Newrvecs.row(0).reshape(1, 3);
        Mat Newtvec = Newtvecs.row(0).reshape(1, 3);

        // 9. นิยามมุมกระดาษในโลก (object points) -------------------------------
        double paperW = 0.30, paperH = 0.23;
        double offX = 0.05, offY = 0.055;
        MatOfPoint3f objectPoints = new MatOfPoint3f(
                new Point3(offX, offY, 0.0), // TL
                new Point3(offX - paperW, offY, 0.0), // TR
                new Point3(offX - paperW, offY - paperH + 0.01, 0.0), // BR
                new Point3(offX, offY - paperH + 0.05, 0.0)  // BL
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
                new org.opencv.core.Point(0, 0),
                new org.opencv.core.Point(539, 0),
                new org.opencv.core.Point(539, 299),
                new org.opencv.core.Point(0, 299)
        );
        Mat H = Calib3d.findHomography(new MatOfPoint2f(srcPtsArr), dstPts);

        // 12. Warp Perspective และ flip ---------------------------------------
        Mat warped = new Mat();
        Imgproc.warpPerspective(imgGray, warped, H, new Size(540, 300));
        warpedFlipped = warped;
        Core.flip(warped, warpedFlipped, 1);


        Size originalSize = warpedFlipped.size();
        originalWidth = originalSize.width;
        originalHeight = originalSize.height;

        double scale_final = Math.min((double) 640 / originalWidth, (double) 640 / originalHeight);

        int scaledWidth = (int) Math.round(originalWidth * scale_final);
        int scaledHeight = (int) Math.round(originalHeight * scale_final);
        Size scaledSize = new Size(scaledWidth, scaledHeight);

        int dx = (640 - scaledWidth) / 2;
        int dy = (640 - scaledHeight) / 2;

        Mat scaledMat = new Mat();
        Imgproc.resize(warpedFlipped, scaledMat, scaledSize, 0, 0, Imgproc.INTER_AREA);

        Mat imgResult = new Mat(640, 640, warpedFlipped.type(), new Scalar(0, 0, 0)); // Scalar(0,0,0) คือสีดำ
        Mat submat = imgResult.submat(new Rect(dx, dy, scaledWidth, scaledHeight));
        scaledMat.copyTo(submat);

        scaledMat.release();

        Kinematics posNow = api.getRobotKinematics();

        return new DataPaper(imgResult, imgRotation, true, Inputpaper, arucoid, rvec_array, tvec_array, keepcorners,posNow, quaternionNow);
    }

    private Mat blackOutHalfImage(Mat img, int paper) {
        // 1. ดึงขนาดของรูปภาพ
        int width = img.width();   // 1280
        int height = img.height(); // 960
        int halfWidth = width / 2;

        // 2. กำหนดสีดำและประเภทการวาด (แบบทึบ)
        Scalar blackColor = new Scalar(0, 0, 0);
        int thickness = Imgproc.FILLED; // หรือ -1 ก็ได้

        // 3. สร้างเงื่อนไขตามค่า paper
        if (paper == 2) {
            // ---> เติมสีดำที่ "ครึ่งขวา"
            // กำหนดจุดเริ่มต้น (บนซ้ายของพื้นที่) และจุดสิ้นสุด (ล่างขวาของพื้นที่)
            org.opencv.core.Point topLeft = new org.opencv.core.Point(halfWidth, 0);
            org.opencv.core.Point bottomRight = new org.opencv.core.Point(width, height);

            Imgproc.rectangle(img, topLeft, bottomRight, blackColor, thickness);

            api.saveMatImage(img, "TestImgLeft.png");

        } else if (paper == 3) {
            // ---> เติมสีดำที่ "ครึ่งซ้าย"
            org.opencv.core.Point topLeft = new org.opencv.core.Point(0, 0);
            org.opencv.core.Point bottomRight = new org.opencv.core.Point(halfWidth, height);

            Imgproc.rectangle(img, topLeft, bottomRight, blackColor, thickness);
            api.saveMatImage(img, "TestImgRight.png");
        }

        // หากค่า paper ไม่ใช่ 2 หรือ 3 ก็จะไม่ทำอะไรกับภาพ

        // 4. คืนค่ารูปภาพที่แก้ไขแล้ว
        return img;
    }

    private void reportArea1(double[] tvec, DataPaper dataPaper) throws IOException {
        // เริ่มต้นเมธอด
        Log.i("ReportArea1", "Starting reportArea1 method.");

        double x = dataPaper.getPointPaper().getX();
        double z = dataPaper.getPointPaper().getZ();

        Point reportPoint = new Point(x, -9.75, z);
        boolean reportPosition = moveToArea(reportPoint, targetOrientations.get(MissionTarget.PLAN2_CAP_A1));

        // แจ้งว่าการเดินทางเสร็จสิ้น
        Log.i("ReportArea1", "Movement to report point completed for Area 1.");
    }

    private void reportArea2(double[] tvec, DataPaper dataPaper) throws IOException {
        // เริ่มต้นเมธอด
        Log.i("ReportArea2", "Starting reportArea2 method.");

        double x = dataPaper.getPointPaper().getX();
        double y = dataPaper.getPointPaper().getY();

        Point reportPoint = new Point(x, y, 4.55);
        boolean reportPosition = moveToArea(reportPoint, targetOrientations.get(MissionTarget.PLAN2_CAP_A23));

        // แจ้งว่าการเดินทางเสร็จสิ้น
        Log.i("ReportArea2", "Movement to report point completed for Area 2.");
    }

    private void reportArea3(double[] tvec, DataPaper dataPaper) throws IOException {
        // เริ่มต้นเมธอด
        Log.i("ReportArea3", "Starting reportArea3 method.");

        double x = dataPaper.getPointPaper().getX();
        double y = dataPaper.getPointPaper().getY();

        Point reportPoint = new Point(x, y, 4.55);

        boolean reportPosition = moveToArea(reportPoint, targetOrientations.get(MissionTarget.PLAN2_CAP_A23));

        // แจ้งว่าการเดินทางเสร็จสิ้น
        Log.i("ReportArea3", "Movement to report point completed for Area 3.");
    }

    private void reportArea4(double[] tvec, DataPaper dataPaper) throws IOException {
        // เริ่มต้นเมธอด
        Log.i("ReportArea4", "Starting reportArea4 method.");

        double y = dataPaper.getPointPaper().getY();
        double z = dataPaper.getPointPaper().getZ();

        Point reportPoint = new Point(10.56, y, z);

        boolean reportPosition = moveToArea(reportPoint, targetOrientations.get(MissionTarget.PLAN2_CAP_A4));


        // แจ้งว่าการเดินทางเสร็จสิ้น
        Log.i("ReportArea4", "Movement to report point completed for Area 4.");
    }

    public void moveToReportArea(int Area_num, DataPaper dataPaper) throws IOException {

        Point point = dataPaper.getPosNow().getPosition();

        Log.i("Point :", "x : " + point.getX() + " y : " + point.getY() + " z : " + point.getZ());
        Log.i("Quaternion", dataPaper.getQuaternionNow().toString());

        Log.i("PointPaper", "x : " + dataPaper.getPointPaper().getX() + " y : " + dataPaper.getPointPaper().getY() + " z : " + dataPaper.getPointPaper().getZ());

        switch (Area_num) {
            case 1:
                reportArea1(dataPaper.getTvec(), dataPaper);
                break;
            case 2:
                reportArea2(dataPaper.getTvec(), dataPaper);
                break;
            case 3:
                reportArea3(dataPaper.getTvec(), dataPaper);
                break;
            case 4:
                reportArea4(dataPaper.getTvec(), dataPaper);
                break;
        }

        CaptureImgCheckBeforetakTargetItemsSnapshot();

        api.takeTargetItemSnapshot();
    }

    private void ReportAllArea(ArrayList<List<Map<String, Object>>> resultList) {

        Log.i("DetectionResults", "กำลังแสดงผลลัพธ์การตรวจจับจาก " + resultList.size() + " รูปภาพ.");

        // วนลูปผ่านแต่ละชุดของผลลัพธ์ (แต่ละรูปภาพ)
        // ใช้ resultList.size() แทน resultList.size() - 1 เพื่อให้รวมรูปภาพสุดท้ายด้วย
        for (int imageIndex = 0; imageIndex < resultList.size(); imageIndex++) {
            List<Map<String, Object>> detectionsForImage = resultList.get(imageIndex);
            DataPaper dataPaper = ListDataPaper.get(imageIndex);

            // สร้าง Map เพื่อเก็บจำนวนของแต่ละคลาสในรูปภาพปัจจุบัน
            // Key คือ className (String), Value คือจำนวน (Integer)
            Map<String, Integer> itemCounts = new HashMap<>();

            Log.i("DetectionResults", "--- ผลลัพธ์สำหรับรูปภาพที่ " + (imageIndex + 1) + " ---");

            if (detectionsForImage.isEmpty()) {
                Log.i("DetectionResults", "    ไม่พบวัตถุใดๆ ในรูปภาพนี้");

                // หากไม่พบวัตถุใดๆ อาจจะต้องส่งข้อมูลไปที่ API ด้วยค่า 0 หรือตามเงื่อนไขที่กำหนด
                // ตัวอย่าง: api.setInfoArea(imageIndex + 1, "none", 0);
                // หรือจะข้ามไปเลยก็ได้ถ้าไม่มีวัตถุ
            } else {
                Log.i("DetectionResults", "    พบวัตถุทั้งหมด: " + detectionsForImage.size() + " ชิ้น (ก่อนรวม)");

                // วนลูปผ่านแต่ละการตรวจจับภายในรูปภาพนั้นๆ เพื่อรวมจำนวนไอเท็ม
                for (Map<String, Object> detection : detectionsForImage) {
                    String className = (String) detection.get("className");

                    if (className.equalsIgnoreCase("crystal") || className.equalsIgnoreCase("diamond") || className.equalsIgnoreCase("emerald")) {
                        dataPaper.setTargetItem(className);
                    }

                    // เพิ่มจำนวนนับสำหรับคลาสนี้
                    itemCounts.put(className, itemCounts.getOrDefault(className, 0) + 1);
                }

                Log.i("DetectionResults", "    --- สรุปจำนวนไอเท็มที่ตรวจพบ ---");
                // วนลูปผ่าน Map ที่เก็บจำนวนไอเท็ม เพื่อส่งข้อมูลไปที่ API และ Logcat
                for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                    String itemName = entry.getKey();

                    if (itemName.equalsIgnoreCase("crystal") || itemName.equalsIgnoreCase("emerald") || itemName.equalsIgnoreCase("diamond")) {
                        continue;
                    }
                    int itemNum = entry.getValue();

                    Log.i("DetectionResults",
                            String.format("    ไอเท็ม: %s, จำนวน: %d", itemName, itemNum));

                    api.setAreaInfo(imageIndex + 1, itemName, itemNum);
                }
            }
        }
    }

    private int FindPaperOfTargetItems() {

        List<Map<String, Object>> list = resultList.get(4);
        String strTargetItem = "";

        for (Map<String, Object> items : list) {
            String className = (String) items.get("className");
            if (className.equalsIgnoreCase("crystal")) {
                strTargetItem = "crystal";
            } else if (className.equalsIgnoreCase("emerald")) {
                strTargetItem = "emerald";
            } else if (className.equalsIgnoreCase("diamond")) {
                strTargetItem = "diamond";
            }
        }

        int numpaper = 0;

        for (int i = 0; i < ListDataPaper.size() - 1; i++) {
            String check = ListDataPaper.get(i).getTargetItem();
            if (check.equalsIgnoreCase(strTargetItem)) {
                numpaper = ListDataPaper.get(i).getPaperNumber();
            }
        }

        return numpaper;
    }

    private void CaptureImgCheckBeforetakTargetItemsSnapshot() {

        double[][] cameraParam = api.getNavCamIntrinsics();

        // สร้าง Mat สำหรับ cameraMatrix (3×3) และ dstMatrix (1×5) ด้วยชนิดข้อมูล double
        Mat cameraMatrix = new Mat(3, 3, CvType.CV_64F);
        Mat dstMatrix = new Mat(1, 5, CvType.CV_64F);
        cameraMatrix.put(0, 0, cameraParam[0]);
        dstMatrix.put(0, 0, cameraParam[1]);

        Mat Cam = api.getMatNavCam();

        Mat imgUndistort = new Mat();
        Calib3d.undistort(Cam, imgUndistort, cameraMatrix, dstMatrix);

        api.saveMatImage(imgUndistort, "imgCheckBeforeSnapShotTarget.png");
    }

}