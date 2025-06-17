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

    private enum MissionTarget {
        PLAN2_CAP_A1,
        PLAN2_CAP_A23,
        PLAN2_CAP_A4,
        PLAN2_ASTRO_POS
    }

    private Map<MissionTarget, Point> targetPositions;
    private Map<MissionTarget, Quaternion> targetOrientations;

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
        targetOrientations.put(MissionTarget.PLAN2_CAP_A1, eulerToQuaternion(-15, 0, -80));
        targetOrientations.put(MissionTarget.PLAN2_CAP_A23, eulerToQuaternion(90, 0, 0));
        targetOrientations.put(MissionTarget.PLAN2_CAP_A4, eulerToQuaternion(-5, 0, 180));
        targetOrientations.put(MissionTarget.PLAN2_ASTRO_POS, eulerToQuaternion(0, 0, 90));

        // ---------------------------------- Test move Point1 ----------------------------------------------------//
        //------------------------- setup -----------------------------------//
        double[][] cameraNavParam = api.getNavCamIntrinsics();
        double[][] cameraDockParam = api.getDockCamIntrinsics();
        Mat NavcameraMatrix = new Mat(3, 3, CvType.CV_64F);
        Mat NavdstMatrix = new Mat(1, 5, CvType.CV_64F);
        NavcameraMatrix.put(0, 0, cameraNavParam[0]);
        NavdstMatrix.put(0, 0, cameraNavParam[1]);
        Mat DockcameraMatrix = new Mat(3, 3, CvType.CV_64F);
        Mat DockdstMatrix = new Mat(1, 5, CvType.CV_64F);
        DockcameraMatrix.put(0, 0, cameraDockParam[0]);
        DockdstMatrix.put(0, 0, cameraDockParam[1]);
        // ---------------------- end setup ----------------------------//

        // ------------ Add Move Point 1 right here vvv ------------------//

        // ------------------------ ^^^ --------------------------------//
        // -------------------------------------------------------------//
        Mat NavCam = api.getMatNavCam();
        Mat DockCam = api.getMatDockCam();
        api.saveMatImage(NavCam, "NavCamNormal_Area1.png");// สลับเลขเอาถ้าอยากย้ายกล้อง
        api.saveMatImage(DockCam, "DockCamNormal_Area2.png");// สลับเลขเอาถ้าอยากย้ายกล้อง
        Mat NavCamUndis = new Mat();
        Mat DockcamUdis = new Mat();
        Calib3d.undistort(NavCam, NavCamUndis, NavcameraMatrix, NavdstMatrix);
        Calib3d.undistort(DockCam, DockcamUdis, DockcameraMatrix, DockdstMatrix);
        api.saveMatImage(NavCamUndis, "NavCamUndis_Area1.png");// สลับเลขเอาถ้าอยากย้ายกล้อง
        api.saveMatImage(DockcamUdis, "DockCamUndis_Area2.png");// สลับเลขเอาถ้าอยากย้ายกล้อง
        // ---------------------- end Save Img  ----------------------------//

        // ------------ Add Move Point 2 right here vvv ------------------//

        // ------------------------ ^^^ --------------------------------//
        // -------------------------------------------------------------//
        NavCam = api.getMatNavCam();
        DockCam = api.getMatDockCam();
        api.saveMatImage(NavCam, "NavCamNormal_Area3.png");// สลับเลขเอาถ้าอยากย้ายกล้อง
        api.saveMatImage(DockCam, "DockCamNormal_Area4.png");// สลับเลขเอาถ้าอยากย้ายกล้อง
        NavCamUndis = new Mat();
        DockcamUdis = new Mat();
        Calib3d.undistort(NavCam, NavCamUndis, NavcameraMatrix, NavdstMatrix);
        Calib3d.undistort(DockCam, DockcamUdis, DockcameraMatrix, DockdstMatrix);
        api.saveMatImage(NavCamUndis, "NavCamUndis_Area3.png");// สลับเลขเอาถ้าอยากย้ายกล้อง
        api.saveMatImage(DockcamUdis, "DockCamUndis_Point4.png"); // สลับเลขเอาถ้าอยากย้ายกล้อง
        // ---------------------- end Save Img  ----------------------------//

        // ------------ Add Move Point astronaut right here vvv ------------//

        // ------------------------ ^^^ --------------------------------//
        // -------------------------------------------------------------//
        NavCam = api.getMatNavCam();
        DockCam = api.getMatDockCam();
        api.saveMatImage(NavCam, "NavCamNormal_Area5.png");// สลับเลขเอาถ้าอยากย้ายกล้อง
        api.saveMatImage(DockCam, "DockCamNormal_Area5.png");// สลับเลขเอาถ้าอยากย้ายกล้อง
        NavCamUndis = new Mat();
        DockcamUdis = new Mat();
        Calib3d.undistort(NavCam, NavCamUndis, NavcameraMatrix, NavdstMatrix);
        Calib3d.undistort(DockCam, DockcamUdis, DockcameraMatrix, DockdstMatrix);
        api.saveMatImage(NavCamUndis, "NavCamUndis_Area5.png");// สลับเลขเอาถ้าอยากย้ายกล้อง
        api.saveMatImage(DockcamUdis, "DockCamUndis_Area5.png");// สลับเลขเอาถ้าอยากย้ายกล้อง

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

            Mat leftImg = blackOutHalfImage(imgUndistort, 2);
            Aruco.detectMarkers(leftImg, Dict, keepcorners, keepids);
            Aruco.estimatePoseSingleMarkers(keepcorners, ARUCO_LEN, cameraMatrix, dstMatrix, keeprvecs, keeptvecs);

        } else if (Inputpaper == 3) {

            Mat rightImg = blackOutHalfImage(imgUndistort, 3);
            Aruco.detectMarkers(rightImg, Dict, keepcorners, keepids);
            Aruco.estimatePoseSingleMarkers(keepcorners, ARUCO_LEN, cameraMatrix, dstMatrix, keeprvecs, keeptvecs);

        } else {
            Aruco.detectMarkers(imgUndistort, Dict, keepcorners, keepids);
            Aruco.estimatePoseSingleMarkers(keepcorners, ARUCO_LEN, cameraMatrix, dstMatrix, keeprvecs, keeptvecs);

            api.saveMatImage(imgUndistort, "imgNormal_" + Inputpaper + ".png");

        }

        keeprvecs.get(0, 0, rvec_array);
        keeptvecs.get(0, 0, tvec_array);

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

        return new DataPaper(imgResult, imgRotation, true, Inputpaper, arucoid, rvec_array, tvec_array, keepcorners, posNow, quaternionNow);
    }

    private Mat blackOutHalfImage(Mat img, int paper) {

        int width = img.width();   // 1280
        int height = img.height(); // 960
        int halfWidth = width / 2;

        Scalar blackColor = new Scalar(0, 0, 0);
        int thickness = Imgproc.FILLED; // หรือ -1 ก็ได้

        if (paper == 2) {

            org.opencv.core.Point topLeft = new org.opencv.core.Point(halfWidth, 0);
            org.opencv.core.Point bottomRight = new org.opencv.core.Point(width, height);

            Imgproc.rectangle(img, topLeft, bottomRight, blackColor, thickness);

            api.saveMatImage(img, "imgUndistort_2.png");

        } else if (paper == 3) {

            org.opencv.core.Point topLeft = new org.opencv.core.Point(0, 0);
            org.opencv.core.Point bottomRight = new org.opencv.core.Point(halfWidth, height);

            Imgproc.rectangle(img, topLeft, bottomRight, blackColor, thickness);
            api.saveMatImage(img, "imgUndistort_3.png");
        }

        return img;
    }

    public void moveToReportArea(int Area_num, DataPaper dataPaper) throws IOException {
        boolean reportPosition = false;
        double[] tvec = dataPaper.getTvec();
        double[] rvec = dataPaper.getRvec();

        switch (Area_num) {
            case 1:

                double x1 = dataPaper.getPointPaper().getX();
                double z1 = dataPaper.getPointPaper().getZ();

                Point moveReportPoint1 = targetPositions.get(MissionTarget.PLAN2_CAP_A1);
                Point translationPoint1 = new Point(x1, -9.85, z1);
                reportPosition = moveToArea(translationPoint1, targetOrientations.get(MissionTarget.PLAN2_CAP_A1));
                break;

            case 2 | 3:

                double x23 = dataPaper.getPointPaper().getX();
                double y23 = dataPaper.getPointPaper().getY();

                Point moveReportPoint2 = targetPositions.get(MissionTarget.PLAN2_CAP_A23);
                Point translationPoint2 = new Point(x23, y23, 4.57);
                reportPosition = moveToArea(translationPoint2, targetOrientations.get(MissionTarget.PLAN2_CAP_A23));
                break;

            case 4:

                double z4 = dataPaper.getPointPaper().getZ();
                double y4 = dataPaper.getPointPaper().getY();

                Point moveReportPoint4 = targetPositions.get(MissionTarget.PLAN2_CAP_A4);
                Point translationPoint4 = new Point(10.58, y4, z4);
                reportPosition = moveToArea(translationPoint4, targetOrientations.get(MissionTarget.PLAN2_CAP_A4));
                break;
        }

        CaptureImgCheckBeforetakTargetItemsSnapshot();

        api.takeTargetItemSnapshot();
    }

    private void ReportAllArea(ArrayList<List<Map<String, Object>>> resultList) {

        Log.i("DetectionResults", "กำลังแสดงผลลัพธ์การตรวจจับจาก " + resultList.size() + " รูปภาพ.");

        for (int imageIndex = 0; imageIndex < resultList.size(); imageIndex++) {
            List<Map<String, Object>> detectionsForImage = resultList.get(imageIndex);
            DataPaper dataPaper = ListDataPaper.get(imageIndex);

            Map<String, Integer> itemCounts = new HashMap<>();

            Log.i("DetectionResults", "--- ผลลัพธ์สำหรับรูปภาพที่ " + (imageIndex + 1) + " ---");

            if (detectionsForImage.isEmpty()) {
                Log.i("DetectionResults", "    ไม่พบวัตถุใดๆ ในรูปภาพนี้");

            } else {
                Log.i("DetectionResults", "    พบวัตถุทั้งหมด: " + detectionsForImage.size() + " ชิ้น (ก่อนรวม)");

                for (Map<String, Object> detection : detectionsForImage) {
                    String className = (String) detection.get("className");

                    if (className.equalsIgnoreCase("crystal") || className.equalsIgnoreCase("diamond") || className.equalsIgnoreCase("emerald")) {
                        dataPaper.setTargetItem(className);
                    }

                    itemCounts.put(className, itemCounts.getOrDefault(className, 0) + 1);
                }

                Log.i("DetectionResults", "    --- สรุปจำนวนไอเท็มที่ตรวจพบ ---");

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