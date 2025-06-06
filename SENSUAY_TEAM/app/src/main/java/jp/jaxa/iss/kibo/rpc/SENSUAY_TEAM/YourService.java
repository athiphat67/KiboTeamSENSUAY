package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM.DataPaper;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import android.os.SystemClock;
import android.util.Log;

import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.List;


/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {
    @Override
    protected void runPlan1() {

        // StartMissions
        api.startMission();

        // moveToArea 1 2 3 4 for capture image
        moveToArea1();

        moveInO2();
//        moveTo95cm();
//        moveTo105cm();
        moveTo115cm();
        moveOutO3();

        moveToArea4();

        // Move to Astronaut
        moveToAstronaut();
        api.reportRoundingCompletion();

        // Shutdown
        api.shutdownFactory();

    }

    private void moveToArea1() {
        // point 1: oasis 1 → area 1
        Point p1 = new Point(10.9d, -9.92284d, 5.195d);
        Quaternion q1 = eulerToQuaternion(0, 0, -90);
        api.moveTo(p1, q1, false);

        // point 2: oasis 2 → area 1
        Point p1_2 = new Point(11.175, -10.03, 5.245);
        Quaternion q2 = eulerToQuaternion(0, 0, -90);
        api.moveTo(p1_2, q2, true);

        captureAndSaveImage("area1_cap.png");
    }

    // move in to oasis 2
    private void moveInO2() {
        Point p23in = new Point(11.150, -8.55, 5.115);
        Quaternion q23 = eulerToQuaternion(90, 0, 0);
        api.moveTo(p23in, q23, true);
    }

//    private void moveTo95cm() {
//        Point p80 = new Point(11.150, -8.45, 4.712); // ห่างจากระนาบ 95 cm
//        Quaternion q23 = eulerToQuaternion(90,0,0);
//        api.moveTo(p80, q23, true);
//
//        SystemClock.sleep(5000);
//
//        captureAndSaveImage("btw23_cap_80cm.png");
//    }
//
//    private void moveTo105cm() {
//        Point p85 = new Point(11.150, -8.45, 4.812); // ห่างจากระนาบ 105 cm
//        Quaternion q23 = eulerToQuaternion(90, 0, 0);
//        api.moveTo(p85, q23, true);
//
//        SystemClock.sleep(5000);
//
//        captureAndSaveImage("btw23_cap_85cm.png");
//    }

    private void moveTo115cm() {
        Point p115 = new Point(11.150, -8.45, 4.912); // ห่างจากระนาบ 115 cm
        Quaternion q23 = eulerToQuaternion(90, 0, 0);
        api.moveTo(p115, q23, true);

        SystemClock.sleep(5000);

        captureAndSaveImage("btw23_cap_115cm.png");
    }

    private void moveOutO3() {
        Point p23out = new Point(11.150, -8.35, 5.115);
        Quaternion q4 = eulerToQuaternion(-15, 0, 180); // หันออกจอ 15 deg
        api.moveTo(p23out, q4, true);
    }

    private void moveToArea4() {
        // point 5: oasis 4 → area 4
        Point p4 = new Point(11.1, -6.875, 4.8);
        Quaternion q4 = eulerToQuaternion(-10, 0, 180); // หันออกจอ 15 deg
        api.moveTo(p4, q4, true);

        SystemClock.sleep(2000);

        captureAndSaveImage("area4_cap.png");
    }

    private void moveToAstronaut() {
        Point astroPoint = new Point(11.143d, -6.7607d, 4.9654d);
        Quaternion astroQ = new Quaternion(0f, 0f, 0.707f, 0.707f); // หันไปทางขวา (y+)
        api.moveTo(astroPoint, astroQ, false);
        api.reportRoundingCompletion();

        SystemClock.sleep(3000);

        captureAndSaveImage("target_cap.png");
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

    // capture and save image
    private void captureAndSaveImage(String filename) {
        Mat img = api.getMatNavCam(); // capture
        api.saveMatImage(img, filename); // save image
    }

    private void CapturePaper(int paper) {

        int stop = String.valueOf(paper).length(); // ถ้า paper=7 → stop=1, ถ้า paper=23 → stop=2
        int start = 0;
        int Inputpaper = paper;
        int Check_paper = paper;
        DataPaper obj = new DataPaper();

        // ---------------------------- start setup field ----------------------------
        while (start < stop) {
            Log.i("TAG", "While : Start " + start );
            Log.i("TAG", "While : Stop " + stop );

            float ARUCO_LEN = 0.05f;

            // เตรียมค่าสำหรับ sharpen kernel
            float[] data = {0, -1, 0, -1, 5, -1, 0, -1, 0};
            Mat kernel = new Mat(3, 3, CvType.CV_32F);
            kernel.put(0, 0, data);
            Log.i("TAG", "Created sharpen kernel (3×3)");

            // ดึงค่าพารามิเตอร์กล้องจาก API
            double[][] cameraParam = (paper == 5) ? api.getDockCamIntrinsics() : api.getNavCamIntrinsics();
            Log.i("TAG", "Retrieved camera intrinsics");

            // สร้าง Mat สำหรับ cameraMatrix (3×3) และ dstMatrix (1×5) ด้วยชนิดข้อมูล double
            Mat cameraMatrix = new Mat(3, 3, CvType.CV_64F);
            Mat dstMatrix = new Mat(1, 5, CvType.CV_64F);
            cameraMatrix.put(0, 0, cameraParam[0]);
            dstMatrix.put(0, 0, cameraParam[1]);
            Log.i("TAG", "Loaded cameraMatrix and dstMatrix");

            // --------------------------------- แยกภาพ ที่ area 2 and area 3 ----------------------------------//
            // เลือกกล้อง (DockCam ถ้า paper==5, ไม่งั้น NavCam)
            Mat Cam = (paper == 5) ? api.getMatDockCam() : api.getMatNavCam();
            Log.i("TAG", "ได้ภาพจาก “paper=" + paper + "” ขนาด (cols×rows)=("
                    + Cam.cols() + "×" + Cam.rows() + ")");

            if (Check_paper == 23) {
                if (start == 0) {
                    paper = Inputpaper / 10;   // ครั้งแรกจะได้หลักหน้า (2)
                } else {
                    paper = Inputpaper % 10;   // ครั้งที่สองจะได้หลักหลัง (3)
                }
                Log.i("TAG", "รอบที่ " + start + " → paper ถูกแปลงเป็น “" + paper + "”");
            }
            start++;

            // คราวนี้แบ่งกรอบภาพซ้าย/ขวา ตามค่า paper
            int wCam = Cam.cols();
            int hCam = Cam.rows();
            int halfWidth = wCam / 2;
            Log.i("TAG", "ภาพมีความกว้าง w=" + wCam + ", สูง h=" + hCam + " → halfWidth=" + halfWidth);

            if (paper == 2) {
                // ใช้ครึ่งซ้าย
                Rect leftRoi = new Rect(0, 0, halfWidth, hCam);
                Cam = new Mat(Cam, leftRoi);
                Log.i("TAG", "ตัดซ้าย: new Rect(0,0," + halfWidth + "," + hCam + ")");
                api.saveMatImage(Cam, "Pre_" + paper + "_imgLeft.png");
            } else if (paper == 3) {
                // ใช้ครึ่งขวา
                Rect rightRoi = new Rect(halfWidth, 0, wCam - halfWidth, hCam);
                Cam = new Mat(Cam, rightRoi);
                Log.i("TAG", "ตัดขวา: new Rect(" + halfWidth + ",0," + (wCam - halfWidth) + "," + hCam + ")");
                api.saveMatImage(Cam, "Pre_" + paper + "_imgRight.png");
            } else {
                // กรณีอื่นๆ ก็โหลดใหม่จาก NavCam เผื่อ paper เปลี่ยนเป็น 5 หรืออื่นๆ
                Cam = api.getMatNavCam();
                Log.i("TAG", "ไม่ได้ตัดภาพ: โหลด NavCam เต็มภาพใหม่ (paper=" + paper + ")");
                api.saveMatImage(Cam, "Pre_" + paper + "_imgNormal.png");
            }

            // สร้าง Mat เปล่าสำหรับเก็บภาพ sharpened และ undistort
            Mat imgSharpned = new Mat();
            Mat imgUndistort = new Mat();
            Log.i("TAG", "Created empty Mats for imgSharpned and imgUndistort");

            // สร้าง ArUco dictionary และตัวเก็บผลลัพธ์
            Dictionary Dict = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
            Mat ids = new Mat();
            List<Mat> corners = new ArrayList<>();
            Log.i("TAG", "Initialized Aruco dictionary and containers");

            // ---------------------------- process ----------------------------
            // ---------------------------- Sharp ----------------------------
            Calib3d.undistort(Cam, imgUndistort, cameraMatrix, dstMatrix);
            Log.i("TAG", "Performed undistort on Cam → imgUndistort");

            Imgproc.filter2D(imgUndistort, imgSharpned, -1, kernel);
            Log.i("TAG", "Applied filter2D on imgUndistort → imgSharpned");

            api.saveMatImage(imgSharpned, "Paper_" + paper + "_imgSharpned.png");
            Log.i("TAG", "Saved imgSharpned.png");

            // ---------------------------- Detect Markers ----------------------------
            Aruco.detectMarkers(imgSharpned, Dict, corners, ids);
            Log.i("TAG", "Ran Aruco.detectMarkers()");

            if (corners.size() == 0) {
                Log.i("TAG", "No marker detected!");

            }
            Log.i("TAG", "Detected " + corners.size() + " marker(s)");

            // ---------------------------- Rotation Paper ----------------------------
            //    TL --------------------------- TR
            //    |                               |
            //    |                               |
            //    |                               |
            //    |                               |
            //    BL --------------------------- BR

            int idx = 0;
            Log.i("TAG", "Selected idx = 0 for marker rotation");

            // ดึง Mat ของมาร์กเกอร์ตัวแรก (1×4×2)
            Mat selectedCorner = corners.get(idx);
            Log.i("TAG", "Got selectedCorner Mat: size=" + selectedCorner.size().toString());

            // เตรียม List สำหรับ estimatePoseSingleMarkers
            List<Mat> CornersList = new ArrayList<>();
            CornersList.add(selectedCorner);

            // เตรียมตัวเก็บ rvecs / tvecs
            Mat rvecs = new Mat();
            Mat tvecs = new Mat();
            Aruco.estimatePoseSingleMarkers(CornersList, ARUCO_LEN, cameraMatrix, dstMatrix, rvecs, tvecs);
            Log.i("TAG", "Called Aruco.estimatePoseSingleMarkers()");
            double[] rvec0 = rvecs.get(0, 0);  // [rx, ry, rz]
            double rx = rvec0[0];
            double ry = rvec0[1];
            double rz = rvec0[2];

            // ดึงค่า translation vector ของ marker ตัวแรก:
            double[] tvec0 = tvecs.get(0, 0);  // [tx, ty, tz]
            double tx = tvec0[0];
            double ty = tvec0[1];
            double tz = tvec0[2];
            Log.i("POSE", String.format(
                    "rvec0 = [%.4f, %.4f, %.4f],  tvec0 = [%.4f, %.4f, %.4f]",
                    rx, ry, rz, tx, ty, tz));

            // สร้าง MatOfPoint2f เพื่อแปลง selectedCorner → Point[]
            MatOfPoint2f cornerPoints = new MatOfPoint2f(selectedCorner);
            org.opencv.core.Point[] cornerArray = cornerPoints.toArray();
            Log.i("TAG", "Converted selectedCorner → cornerArray (length=" + cornerArray.length + ")");

            // ---------------------------- คำนวณ pixel-to-meter ----------------------------
            double pixelDistance1 = Core.norm(new MatOfPoint2f(cornerArray[0]), new MatOfPoint2f(cornerArray[1]));
            double pixelDistance2 = Core.norm(new MatOfPoint2f(cornerArray[0]), new MatOfPoint2f(cornerArray[3]));
            double pixelDistance3 = Core.norm(new MatOfPoint2f(cornerArray[1]), new MatOfPoint2f(cornerArray[2]));
            double pixelDistance4 = Core.norm(new MatOfPoint2f(cornerArray[2]), new MatOfPoint2f(cornerArray[3]));
            double pixelDistance = (pixelDistance1 + pixelDistance2 + pixelDistance3 + pixelDistance4) / 4;
            Log.i("TAG", "Calculated pixelDistance = " + pixelDistance);

            double pixelToMRatio = pixelDistance / ARUCO_LEN;
            Log.i("TAG", "Calculated pixelToMRatio = " + pixelToMRatio);

            // ---------------------------- คำนวณมุมหมุน (roll) ----------------------------
            double xTL = cornerArray[0].x;
            double yTL = cornerArray[0].y;
            double xTR = cornerArray[1].x;
            double yTR = cornerArray[1].y;
            Log.i("TAG", "TL coords = (" + xTL + ", " + yTL + "), TR coords = (" + xTR + ", " + yTR + ")");

            double roll_rad = Math.atan2(yTL - yTR, xTL - xTR);
            double roll_deg = Math.toDegrees(roll_rad);
            Log.i("TAG", "Computed roll_rad = " + roll_rad + ", roll_deg = " + roll_deg);

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
            Log.i("TAG", "Created empty imgRotation Mat");

            // ---------------------------- ทำ warpAffine → หมุนภาพตามมุม roll_deg และ scale = -1 ----------------------------
            Imgproc.warpAffine(imgUndistort, imgRotation, M, new org.opencv.core.Size(w, h));
            Log.i("TAG", "Performed warpAffine, imgRotation size = " + imgRotation.size().toString());

            // ---------------------------- นำ kernel มา sharpen ผลลัพธ์อีกครั้ง ----------------------------
            Imgproc.filter2D(imgRotation, imgRotation, -1, kernel);
            Log.i("TAG", "Applied filter2D (sharpen) on imgRotation");

            // ---------------------------- พลิกภาพ (flip code = -1) ----------------------------
            //Core.flip(imgRotation, imgRotation, -1);
            Log.i("TAG", "Flipped imgRotation with flip code -1");
            api.saveMatImage(imgRotation, "Paper_" + paper + "_imgRotation.png");

            Mat imgResult = imgRotation;

            Log.i("TAG", "Set check = true");

            // ---------------------------- บันทึกภาพสุดท้าย ----------------------------

            api.saveMatImage(imgResult, "PaperResult_" + paper + ".png");
            Log.i("TAG", "Saved rotated+sharpened image as Paper_" + paper + "_Result" + ".png");
            Log.i("TAG", "Finished CapturePaper(" + paper + ")");

            // ----------------------------- Save Data Of Paper ----------------------------
            // แยกตัวเลขหน้า/หลังถ้าจำนวนหลัก >1
            int thisDigit;
            if (Inputpaper >= 10) {
                if (start == 0) thisDigit = Inputpaper / 10;
                else thisDigit = Inputpaper % 10;
            } else {
                thisDigit = Inputpaper;
            }

            int arucoId = (int) ids.get(0, 0)[0];

            obj = new DataPaper(thisDigit, arucoId, rvec0, tvec0);

            Log.i("TAG", "Succes Save DataPaper");
            Log.i("TAG", "Paper " + paper);
            Log.i("TAG", "Star " + start);


        }
    }
}
