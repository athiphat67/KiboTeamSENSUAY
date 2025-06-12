package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM.ObjectDetector;
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

    // enum class ระบุชื่อจุด
    private enum MissionTarget {
        AREA1_POINT1,
        AREA1_POINT2,
        AREA2_ENTRANCE,
        AREA23_CAPTURE,
        AREA3_EXIT,
        AREA4_CAPTURE,
        ASTRONAUT_INTERACTION_POS
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
    public boolean moveToArea(Point position, Quaternion orientation) throws IOException{
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
        targetPositions.put(MissionTarget.AREA1_POINT1, new Point(10.9d, -9.92284d, 5.195d)); // area1 : point 1
        targetPositions.put(MissionTarget.AREA1_POINT2, new Point(11.175, -10.03, 5.245d)); // area1 : point2 (capture)
        targetPositions.put(MissionTarget.AREA2_ENTRANCE, new Point(11.150, -8.55, 5.115d)); // area2 : move out oasis2's point
        targetPositions.put(MissionTarget.AREA23_CAPTURE, new Point(11.150, -8.45, 4.912d)); // area2,3 : capture
        targetPositions.put(MissionTarget.AREA3_EXIT, new Point(11.150, -8.35, 5.115d)); // area3 : move out oasis3's point
        targetPositions.put(MissionTarget.AREA4_CAPTURE, new Point(11.1d, -6.875, 4.8d)); // area4 : point4 (capture)
        targetPositions.put(MissionTarget.ASTRONAUT_INTERACTION_POS, new Point(11.143d, -6.7607d, 4.9654d)); // astroPoint ใน moveToAstronaut

        // Add องศารอบแกนหมุนไปที่ชื่อจุด
        // Quaternion (pitch,roll,yaw)
        targetOrientations.put(MissionTarget.AREA1_POINT1, eulerToQuaternion(0, 0, -90)); // deg area1
        targetOrientations.put(MissionTarget.AREA1_POINT2, eulerToQuaternion(0, 0, -90)); // deg area1 capture
        targetOrientations.put(MissionTarget.AREA2_ENTRANCE, eulerToQuaternion(90, 0, 0)); // deg area2
        targetOrientations.put(MissionTarget.AREA23_CAPTURE, eulerToQuaternion(90, 0, 0)); // deg area2,3 capture
        targetOrientations.put(MissionTarget.AREA3_EXIT, eulerToQuaternion(-10, 0, 180)); //  deg area3
        targetOrientations.put(MissionTarget.AREA4_CAPTURE, eulerToQuaternion(-10, 0, 180)); // deg area4 capture
        targetOrientations.put(MissionTarget.ASTRONAUT_INTERACTION_POS, eulerToQuaternion(0, 0, 90)); // astroQ ใน moveToAstronaut

        // move to area 1
        // move astrobee ไปที่จุดที่ 1
        try {
            Log.i("Mission", "Moving to Area 1");
            moveToArea(targetPositions.get(MissionTarget.AREA1_POINT1), targetOrientations.get(MissionTarget.AREA1_POINT1));;
        } catch (IOException e) {
            e.printStackTrace();
        }

        // move astrobee ไปที่จุดบน oasis 2 พร้อมหมุน astrobee แล้วถ่ายภาพ
        try {
            Log.i("Mission", "Moving to Area 1 Capture Position...");
            moveToArea(targetPositions.get(MissionTarget.AREA1_POINT2), targetOrientations.get(MissionTarget.AREA1_POINT2));
            DataPaper result1 = CapturePaper(1);
            Mat imgResult = result1.getCaptureImage();
            api.saveMatImage(imgResult, "imgArea_"+ 1 +".png");

            ObjectDetector detector = new ObjectDetector(this);
            resultList.add(detector.processImage(result1));

        } catch (IOException e) {
            e.printStackTrace();
        }

        // move to area 2
        // move astrobee ไปที่ oasis 2 (ก่อนจะเคลื่อนที่ตามแนวแกน -z เพื่อเข้าไปถ่ายรูป)
        try {
            Log.i("Mission", "Moving in of Oasis 2...");
            moveToArea(targetPositions.get(MissionTarget.AREA2_ENTRANCE), targetOrientations.get(MissionTarget.AREA2_ENTRANCE));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // area 2,3 capture
        // move astrobee ไปที่จุดกึ่งกลางระหว่าง oasis และ เคลื่อนที่ขึ้นตามแนวแกน -z เพื่อถ่ายรูปห่างจากระนาบเป็นระยา ~115cm
        try {
            Log.i("Mission", "Moving to Area 2,3 Capture Position...");
            moveToArea(targetPositions.get(MissionTarget.AREA23_CAPTURE), targetOrientations.get(MissionTarget.AREA23_CAPTURE));
            ObjectDetector detector = new ObjectDetector(this);

            // delay astrobee ค้างไว้ 4000 millisecond==4 sec เพื่อเช็คให้ชัวร์ว่านิ่งจริงๆแล้วค่อยถ่ายภาพ
            SystemClock.sleep(4000);

            DataPaper result2 = CapturePaper(2);
            Mat imgResult2 = result2.getCaptureImage();
            api.saveMatImage(imgResult2, "imgArea_"+ 2 +".png");
            resultList.add(detector.processImage(result2));

            DataPaper result3 = CapturePaper(3);
            Mat imgResult3 = result3.getCaptureImage();
            api.saveMatImage(imgResult3, "imgArea_"+ 3 +".png");
            resultList.add(detector.processImage(result3));

        } catch (IOException e) {
            e.printStackTrace();
        }

        // move astrobee ออกจาก oasis 3 (*อาจจะต้องแก้ เดี๋ยวดูก่อน)
        try {
            Log.i("Mission", "Moving out of Oasis 3...");
            moveToArea(targetPositions.get(MissionTarget.AREA3_EXIT), targetOrientations.get(MissionTarget.AREA3_EXIT));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // move to area 4
        // move astrobee เข้า area 4 (ยังอยู่ใน oasis 4) แล้วกดกล้องลง ~10 degree (ลองไปดูที่ orientation)
        try {
            Log.i("Mission", "Moving to Area 4 Capture Position...");
            moveToArea(targetPositions.get(MissionTarget.AREA4_CAPTURE), targetOrientations.get(MissionTarget.AREA4_CAPTURE));
            ObjectDetector detector = new ObjectDetector(this);

            SystemClock.sleep(2000);

            DataPaper result4 = CapturePaper(4);
            Mat imgResult4 = result4.getCaptureImage();
            api.saveMatImage(imgResult4, "imgArea_"+ 4 +".png");
            resultList.add(detector.processImage(result4));

        } catch (IOException e) {
            e.printStackTrace();
        }

        // move to astronaut
        try {
            Log.i("Mission", "Moving to Astronaut Interaction Position...");
            moveToArea(targetPositions.get(MissionTarget.ASTRONAUT_INTERACTION_POS), targetOrientations.get(MissionTarget.ASTRONAUT_INTERACTION_POS));

            api.reportRoundingCompletion();

            SystemClock.sleep(3000);

            DataPaper result5 = CapturePaper(5);
            Mat imgResult5 = result5.getCaptureImage();
            api.saveMatImage(imgResult5, "imgArea_"+ 5 +".png");

            ObjectDetector detector = new ObjectDetector(this);
            resultList.add(detector.processImage(result5));

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Shutdown
        api.shutdownFactory();

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

            Mat imgUndistort = new Mat();
            Calib3d.undistort(Cam, imgUndistort, cameraMatrix, dstMatrix);

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
            api.saveMatImage(imgRotation, "ImgCheckArea_" + Inputpaper + ".png");

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

        Size originalSize = warpedFlipped.size();
        double originalWidth = originalSize.width;
        double originalHeight = originalSize.height;

        double scale = Math.min((double) 640 / originalWidth, (double) 640 / originalHeight);

        int scaledWidth = (int) Math.round(originalWidth * scale);
        int scaledHeight = (int) Math.round(originalHeight * scale);
        Size scaledSize = new Size(scaledWidth, scaledHeight);

        int dx = (640 - scaledWidth) / 2;
        int dy = (640 - scaledHeight) / 2;

        Mat scaledMat = new Mat();
        Imgproc.resize(warpedFlipped, scaledMat, scaledSize, 0, 0, Imgproc.INTER_AREA);

        Mat imgResult = new Mat(640, 640, warpedFlipped.type(), new Scalar(0, 0, 0)); // Scalar(0,0,0) คือสีดำ
        Mat submat = imgResult.submat(new Rect(dx, dy, scaledWidth, scaledHeight));
        scaledMat.copyTo(submat);

        scaledMat.release();

        return new DataPaper(imgResult, true, Inputpaper, arucoid, rvec_array, tvec_array);
    }

}