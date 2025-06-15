package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import android.util.Log;


import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.core.Mat;

import java.util.Arrays;
import java.util.List;

/**
 * A data class to hold all relevant information from a single paper capture event.
 * This includes the processed image, success status, and pose data (rvec, tvec).
 */
public class DataPaper {

    // --- Fields ---
    private Mat captureImage;
    private Mat backupImage;
    private String targetItem;
    private boolean isSuccess;
    private String statusMessage;
    private int paperNumber;
    private int arucoId;
    private double[] rvec;
    private double[] tvec;
    private Kinematics posNow;
    private Quaternion quaternionNow;
    private Point pointPaper;
    private List<Mat> keepcorners;
    private double xAruco;
    private double yAruco;

    // --- Constructors & Other Methods... ---
    public DataPaper(Mat captureImage, Mat backupImage, boolean isSuccess, int paperNumber, int arucoId, double[] rvec, double[] tvec, List<Mat> keepcorners ,Kinematics pos, Quaternion qnow) {

        this.captureImage = captureImage;
        this.backupImage = backupImage;
        this.isSuccess = isSuccess;
        this.paperNumber = paperNumber;
        this.arucoId = arucoId;
        this.targetItem = "";
        this.posNow = pos;
        this.quaternionNow = qnow;
        this.keepcorners = keepcorners;
        calculateArucoCenter();

        if (rvec != null && rvec.length == 3) {
            this.rvec = new double[3];
            System.arraycopy(rvec, 0, this.rvec, 0, 3);
        } else {
            this.rvec = new double[]{0, 0, 0};
        }

        if (tvec != null && tvec.length == 3) {
            this.tvec = new double[3];
            System.arraycopy(tvec, 0, this.tvec, 0, 3);
        } else {
            this.tvec = new double[]{0, 0, 0};
        }
        this.pointPaper = calculateWorldPoint();
        logDataToLogcat("ShowDataPaper");
    }

    public DataPaper(String error) {
        this.isSuccess = false;
        this.statusMessage = error;
    }

    public DataPaper() {
    }

    // ... all getters and setters ...
    public Mat getCaptureImage() {
        return captureImage;
    }

    public void setCaptureImage(Mat captureImage) {
        this.captureImage = captureImage;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public int getPaperNumber() {
        return paperNumber;
    }

    public void setPaperNumber(int paperNumber) {
        this.paperNumber = paperNumber;
    }

    public int getArucoId() {
        return arucoId;
    }

    public void setArucoId(int arucoId) {
        this.arucoId = arucoId;
    }

    public double[] getRvec() {
        return rvec;
    }

    public String getTargetItem() {
        return targetItem;
    }

    public void setTargetItem(String targetItem) {
        this.targetItem = targetItem;
    }

    public Mat getBackupImage() {
        return backupImage;
    }

    public void setBackupImage(Mat backupImage) {
        this.backupImage = backupImage;
    }

    public Kinematics getPosNow() {
        return posNow;
    }

    public void setPosNow(Kinematics posNow) {
        this.posNow = posNow;
    }

    public Quaternion getQuaternionNow() {
        return quaternionNow;
    }

    public void setQuaternionNow(Quaternion quaternionNow) {
        this.quaternionNow = quaternionNow;
    }

    public void setRvec(double[] rvec) {
        if (rvec != null && rvec.length == 3) {
            System.arraycopy(rvec, 0, this.rvec, 0, 3);
        }
    }

    public double[] getTvec() {
        return tvec;
    }

    public void setTvec(double[] tvec) {
        if (tvec != null && tvec.length == 3) {
            System.arraycopy(tvec, 0, this.tvec, 0, 3);
        }
    }

    public Point getPointPaper() {
        return pointPaper;
    }

    public void setPointPaper(Point pointPaper) {
        this.pointPaper = pointPaper;
    }

    public double getxAruco() {
        return xAruco;
    }

    public double getyAruco() {
        return yAruco;
    }

    public List<Mat> getKeepcorners() {
        return keepcorners;
    }

    @Override
    public String toString() {
        return "DataPaper{...}";
    }

    private Quaternion multiplyQuaternions(Quaternion qA, Quaternion qB) {
        float wA = qA.getW(), xA = qA.getX(), yA = qA.getY(), zA = qA.getZ();
        float wB = qB.getW(), xB = qB.getX(), yB = qB.getY(), zB = qB.getZ();

        float w_res = wA * wB - xA * xB - yA * yB - zA * zB;
        float x_res = wA * xB + xA * wB + yA * zB - zA * yB;
        float y_res = wA * yB - xA * zB + yA * wB + zA * xA;
        float z_res = wA * zB + xA * yB - yA * xA + zA * wB;

        return new Quaternion(x_res, y_res, z_res, w_res);
    }

    private Quaternion getQuaternionConjugate(Quaternion q) {
        return new Quaternion(-q.getX(), -q.getY(), -q.getZ(), q.getW());
    }

    private Point calculateWorldPoint() {

        // 1. ดึงข้อมูลที่จำเป็นออกมา
        // ทิศทางของหุ่นยนต์ในพิกัดโลก
        Quaternion cameraQuaternion = this.getQuaternionNow();
        // ตำแหน่งของหุ่นยนต์ในพิกัดโลก
        Point cameraPosition = this.posNow.getPosition();
        // เวกเตอร์จากกล้องไปยัง ArUco marker (ในระบบพิกัดของ OpenCV)
        double[] tvecOpenCV = this.getTvec();

        // 2. *** ขั้นตอนสำคัญ: จัดแนวแกนของ tvec ***
        // แปลงเวกเตอร์ tvec จากระบบพิกัด OpenCV ให้สอดคล้องกับแกนของพิกัดโลก
        // - tvec_Z (ไปข้างหน้า) ของ OpenCV คือแกน +X ของโลก
        // - tvec_X (ไปทางขวา) ของ OpenCV คือแกน +Y ของโลก
        // - tvec_Y (ชี้ลง) ของ OpenCV คือแกน -Z ของโลก (เพราะ +Z ของโลกคือชี้ขึ้น)
        double alignedX =  tvecOpenCV[2]; // World X = OpenCV Z
        double alignedY =  tvecOpenCV[0]; // World Y = OpenCV X
        double alignedZ = -tvecOpenCV[1]; // World Z = - (OpenCV Y)

        // 3. สร้าง Pure Quaternion (p') จากเวกเตอร์ที่จัดแนวแกนแล้ว
        Quaternion p_aligned = new Quaternion(
                (float) alignedX,
                (float) alignedY,
                (float) alignedZ,
                0f
        );

        // 4. หมุนเวกเตอร์ p' ด้วยทิศทางของหุ่นยนต์ (cameraQuaternion)
        // โดยใช้สูตรมาตรฐาน: v_rotated = q * v * q_conjugate
        Quaternion cameraQuaternion_conjugate = getQuaternionConjugate(cameraQuaternion);
        Quaternion temp_rotated = multiplyQuaternions(cameraQuaternion, p_aligned);
        Quaternion p_rotated = multiplyQuaternions(temp_rotated, cameraQuaternion_conjugate);

        // 5. นำเวกเตอร์ผลลัพธ์ที่หมุนแล้วไปบวกกับตำแหน่งของหุ่นยนต์
        // เพื่อให้ได้ตำแหน่งของ ArUco marker ในพิกัดโลก
        double worldX = cameraPosition.getX() + p_rotated.getX();
        double worldY = cameraPosition.getY() + p_rotated.getY();
        double worldZ = cameraPosition.getZ() + p_rotated.getZ();

        return new Point(worldX, worldY, worldZ);
    }

    private void calculateArucoCenter() {
        //-- ตรวจสอบว่ามีข้อมูลมุมอยู่จริงและไม่ว่างเปล่า
        if (this.keepcorners == null || this.keepcorners.isEmpty()) {
            this.xAruco = 0;
            this.yAruco = 0;
            return;
        }

        //-- ดึงข้อมูลมุมของ Marker ตัวแรก
        Mat corners = this.keepcorners.get(0);

        //-- ตรวจสอบว่ามี 4 มุมครบถ้วน
        if (corners.total() < 4) {
            this.xAruco = 0;
            this.yAruco = 0;
            return;
        }

        //-- ดึงพิกัด (x, y) ของแต่ละมุม
        // .get(row, col) จะคืนค่า double[2] {x, y}
        double[] corner1 = corners.get(0, 0); // Top-left
        double[] corner2 = corners.get(0, 1); // Top-right
        double[] corner3 = corners.get(0, 2); // Bottom-right
        double[] corner4 = corners.get(0, 3); // Bottom-left

        //-- คำนวณค่าเฉลี่ยตามสมการของคุณ
        this.xAruco = (corner1[0] + corner2[0] + corner3[0] + corner4[0]) / 4.0;
        this.yAruco = (corner1[1] + corner2[1] + corner3[1] + corner4[1]) / 4.0;
    }

    public void logDataToLogcat(String tag) {
        // พิมพ์ Header เพื่อให้แยกแยะข้อมูลได้ง่ายใน Logcat
        Log.i(tag, "========== DataPaper Log ==========");

        // ข้อมูลสถานะและความสำเร็จ
        Log.i(tag, "isSuccess: " + isSuccess);
        Log.i(tag, "statusMessage: '" + statusMessage + "'");

        // ข้อมูลเกี่ยวกับเป้าหมาย
        Log.i(tag, "paperNumber: " + paperNumber);
        Log.i(tag, "arucoId: " + arucoId);
        Log.i(tag, "targetItem: '" + targetItem + "'");

        // ข้อมูล Pose และตำแหน่ง
        Log.i(tag, "rvec: " + Arrays.toString(rvec));
        Log.i(tag, "tvec: " + Arrays.toString(tvec));
        Log.i(tag, "pointPaper: " + (pointPaper != null ? pointPaper.toString() : "null"));
        Log.i(tag, String.format("Aruco Center (x, y): (%.2f, %.2f)", xAruco, yAruco));

        // ข้อมูลตำแหน่งของหุ่นยนต์
        Log.i(tag, "posNow: " + (posNow != null ? posNow.toString() : "null"));
        Log.i(tag, "quaternionNow: " + (quaternionNow != null ? quaternionNow.toString() : "null"));

        // ข้อมูลสรุปของรูปภาพ
        String captureInfo = (captureImage != null) ? "Mat[width=" + captureImage.width() + ", height=" + captureImage.height() + "]" : "null";
        Log.i(tag, "captureImage: " + captureInfo);

        String cornersInfo = (keepcorners != null) ? "List<Mat>[size=" + keepcorners.size() + "]" : "null";
        Log.i(tag, "keepcorners: " + cornersInfo);

        // พิมพ์ Footer
        Log.i(tag, "===================================");
    }

    public Point getArucoMarkerWorldPosition() {
        if (this.rvec == null || this.tvec == null || this.rvec.length < 3 || this.tvec.length < 3 || this.posNow == null || this.quaternionNow == null) {
            System.out.println("Error: Insufficient data to calculate Aruco marker world position.");
            return null;
        }

        // 1. แปลง rvec และ tvec ให้เป็น Rotation Matrix (R_camera_marker)
        Mat rotationVector = new MatOfDouble(rvec);
        Mat translationVector = new MatOfDouble(tvec);
        Mat rotationMatrix = new Mat(3, 3, CvType.CV_64FC1);
        Calib3d.Rodrigues(rotationVector, rotationMatrix);

        // 2. สร้าง Transformation Matrix จาก Camera ไปยัง Marker (T_camera_marker)
        Mat T_camera_marker = Mat.eye(4, 4, CvType.CV_64FC1); // Identity matrix 4x4
        // ใส่ Rotation Matrix
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                T_camera_marker.put(i, j, rotationMatrix.get(i, j));
            }
        }
        // ใส่ Translation Vector
        T_camera_marker.put(0, 3, tvec[0]);
        T_camera_marker.put(1, 3, tvec[1]);
        T_camera_marker.put(2, 3, tvec[2]);

        // 3. สร้าง Transformation Matrix จาก World ไปยัง Camera (T_world_camera)
        // ใช้ตำแหน่งและการหมุนของ Astrobee (กล้อง)
        double camera_x = posNow.getPosition().getX();
        double camera_y = posNow.getPosition().getY();
        double camera_z = posNow.getPosition().getZ();

        // แปลง Quaternion เป็น Rotation Matrix (R_world_camera)
        double qx = quaternionNow.getX();
        double qy = quaternionNow.getY();
        double qz = quaternionNow.getZ();
        double qw = quaternionNow.getW();

        Mat R_world_camera = new Mat(3, 3, CvType.CV_64FC1);
        R_world_camera.put(0, 0, 1 - 2*qy*qy - 2*qz*qz);
        R_world_camera.put(0, 1, 2*qx*qy - 2*qz*qw);
        R_world_camera.put(0, 2, 2*qx*qz + 2*qy*qw);

        R_world_camera.put(1, 0, 2*qx*qy + 2*qz*qw);
        R_world_camera.put(1, 1, 1 - 2*qx*qx - 2*qz*qz);
        R_world_camera.put(1, 2, 2*qy*qz - 2*qx*qw);

        R_world_camera.put(2, 0, 2*qx*qz - 2*qy*qw);
        R_world_camera.put(2, 1, 2*qy*qz + 2*qx*qw);
        R_world_camera.put(2, 2, 1 - 2*qx*qx - 2*qy*qy);

        Mat T_world_camera = Mat.eye(4, 4, CvType.CV_64FC1);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                T_world_camera.put(i, j, R_world_camera.get(i, j));
            }
        }
        T_world_camera.put(0, 3, camera_x);
        T_world_camera.put(1, 3, camera_y);
        T_world_camera.put(2, 3, camera_z);


        // 4. คำนวณ Transformation Matrix จาก World ไปยัง Marker (T_world_marker)
        // T_world_marker = T_world_camera * T_camera_marker
        Mat T_world_marker = new Mat(4, 4, CvType.CV_64FC1);
        Core.gemm(T_world_camera, T_camera_marker, 1, new Mat(), 0, T_world_marker);

        // 5. ดึงตำแหน่งของ Marker ในพิกัดโลก
        double marker_world_x = T_world_marker.get(0, 3)[0];
        double marker_world_y = T_world_marker.get(1, 3)[0];
        double marker_world_z = T_world_marker.get(2, 3)[0];

        // ปล่อย Mat ที่ไม่ใช้แล้วเพื่อป้องกัน Memory Leaks
        rotationVector.release();
        translationVector.release();
        rotationMatrix.release();
        T_camera_marker.release();
        R_world_camera.release();
        T_world_camera.release();
        T_world_marker.release();

        // สร้าง Point จาก Astrobee types หรือ OpenCV Point3f ตามที่คุณต้องการ
        Point pt = new Point(marker_world_x, marker_world_y, marker_world_z);

        return pt;
    }

}