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

import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import org.tensorflow.lite.*;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {
<<<<<<< HEAD
=======
>>>>>>> 7f04d5f28037776c9703be7c1e8ed17cdc7e5bdb
    @Override
    protected void runPlan1() {
        // StartMissions
        api.startMission();
        // moveToArea 1 2 3 4 for capture image
        moveToArea1();

        moveInO2();
<<<<<<< HEAD
        // moveTo95cm();
        // moveTo105cm();
        moveTo115cm();
=======
//        moveTo95cm();
//        moveTo105cm();
        moveTo115cm();
        moveOutO3();
>>>>>>> 7f04d5f28037776c9703be7c1e8ed17cdc7e5bdb

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

<<<<<<< HEAD
        Mat img = CapturePaper(1);
        api.saveMatImage(img, "Area1.png");
=======
        captureAndSaveImage("area1_cap.png");
>>>>>>> 7f04d5f28037776c9703be7c1e8ed17cdc7e5bdb
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
<<<<<<< HEAD
//ๆ
//        SystemClock.sleep(5000);
//
=======
//
//        SystemClock.sleep(5000);
//
>>>>>>> 7f04d5f28037776c9703be7c1e8ed17cdc7e5bdb
//        captureAndSaveImage("btw23_cap_85cm.png");
//    }

    private void moveTo115cm() {
        Point p115 = new Point(11.150, -8.45, 4.912); // ห่างจากระนาบ 115 cm
        Quaternion q23 = eulerToQuaternion(90, 0, 0);
        api.moveTo(p115, q23, true);

        SystemClock.sleep(5000);

<<<<<<< HEAD
        Mat img1 = CapturePaper(2);
        api.saveMatImage(img1, "Area2.png");
        Mat img2 = CapturePaper(3);
        api.saveMatImage(img2, "Area3.png");
=======
        captureAndSaveImage("btw23_cap_115cm.png");
    }

    private void moveOutO3() {
        Point p23out = new Point(11.150, -8.35, 5.115);
        Quaternion q4 = eulerToQuaternion(-15, 0, 180); // หันออกจอ 15 deg
        api.moveTo(p23out, q4, true);
>>>>>>> 7f04d5f28037776c9703be7c1e8ed17cdc7e5bdb
    }


    private void moveToArea4() {
        // point 5: oasis 4 → area 4
        Point p4 = new Point(11.1, -6.875, 4.8);
        Quaternion q4 = eulerToQuaternion(-10, 0, 180); // หันออกจอ 15 deg
        api.moveTo(p4, q4, true);
<<<<<<< HEAD
        Log.i("TAG","Success move point4");

        SystemClock.sleep(2000);

        Mat img = CapturePaper(4);
        api.saveMatImage(img, "Area4.png");
=======

        SystemClock.sleep(2000);

        captureAndSaveImage("area4_cap.png");
>>>>>>> 7f04d5f28037776c9703be7c1e8ed17cdc7e5bdb
    }

    private void moveToAstronaut() {
        Point astroPoint = new Point(11.143d, -6.7607d, 4.9654d);
        Quaternion astroQ = new Quaternion(0f, 0f, 0.707f, 0.707f); // หันไปทางขวา (y+)
        api.moveTo(astroPoint, astroQ, false);
<<<<<<< HEAD
        Log.i("TAG","Success move point5");
        api.reportRoundingCompletion();

        SystemClock.sleep(1500);

        Mat img = CapturePaper(5);
        api.saveMatImage(img, "TargetItems.png");

=======
        api.reportRoundingCompletion();

        SystemClock.sleep(3000);

        captureAndSaveImage("target_cap.png");
>>>>>>> 7f04d5f28037776c9703be7c1e8ed17cdc7e5bdb
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

<<<<<<< HEAD

    private Mat CapturePaper(int paper) {
=======
    private void CapturePaper(int paper) {
>>>>>>> 7f04d5f28037776c9703be7c1e8ed17cdc7e5bdb

        int stop = String.valueOf(paper).length(); // ถ้า paper=7 → stop=1, ถ้า paper=23 → stop=2
        int start = 0;
        int Inputpaper = paper;
        int Check_paper = paper;
        DataPaper obj = new DataPaper();
        Mat warpedFlipped = new Mat();

        // ---------------------------- start setup field ----------------------------
        while (start < stop) {

            float ARUCO_LEN = 0.05f;

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

            // --------------------------------- แยกภาพ ที่ area 2 and area 3 ----------------------------------//

            if (Check_paper == 23) {
                if (start == 0) {
                    paper = Inputpaper / 10;   // ครั้งแรกจะได้หลักหน้า (2)
                } else {
                    paper = Inputpaper % 10;   // ครั้งที่สองจะได้หลักหลัง (3)
                }
            }
            start++;

            // เลือกกล้อง (DockCam ถ้า paper==5, ไม่งั้น NavCam)
            Mat Cam = api.getMatNavCam();

            Mat imgUndistort = new Mat();
            Calib3d.undistort(Cam, imgUndistort, cameraMatrix, dstMatrix);

            // คราวนี้แบ่งกรอบภาพซ้าย/ขวา ตามค่า paper
            int wCam = imgUndistort.cols();
            int hCam = imgUndistort.rows();
            int halfWidth = wCam / 2;

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

            // ---------------------------- พลิกภาพ (flip code = -1) ----------------------------

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
            warpedFlipped = new Mat();
            Core.flip(warped, warpedFlipped, 1);

        }
<<<<<<< HEAD
        return warpedFlipped;
=======
>>>>>>> 7f04d5f28037776c9703be7c1e8ed17cdc7e5bdb
    }

    private void testTensorFlowList() {

    }

}
