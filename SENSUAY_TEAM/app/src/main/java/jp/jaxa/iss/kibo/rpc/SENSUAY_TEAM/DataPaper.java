package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import gov.nasa.arc.astrobee.types.Point;

import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.types.Quaternion;

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
    private Point pointPaper ;
    private Mat dstMatrix;
    private Mat cameraMatrix;

    // --- Constructors ---

    /**
     * The main constructor for a successful capture event.
     */
    public DataPaper(Mat captureImage, Mat backupImage, boolean isSuccess, int paperNumber, int arucoId, double[] rvec, double[] tvec, Kinematics pos, Quaternion qnow , Mat cameraMatrix, Mat dstMatrix) {
        this.captureImage = captureImage;
        this.backupImage = backupImage;
        this.isSuccess = isSuccess;
        this.paperNumber = paperNumber;
        this.arucoId = arucoId;
        this.targetItem = "";
        this.posNow = pos;
        this.quaternionNow = qnow;
        this.cameraMatrix = cameraMatrix;
        this.dstMatrix = dstMatrix;

        // Ensure rvec is a 3-element array
        if (rvec != null && rvec.length == 3) {
            this.rvec = new double[3];
            System.arraycopy(rvec, 0, this.rvec, 0, 3);
        } else {
            this.rvec = new double[]{0, 0, 0};
        }

        // Ensure tvec is a 3-element array
        if (tvec != null && tvec.length == 3) {
            this.tvec = new double[3];
            System.arraycopy(tvec, 0, this.tvec, 0, 3);
        } else {
            this.tvec = new double[]{0, 0, 0};
        }

        this.pointPaper = getArucoMarkerWorldPosition();

    }

    /**
     * Constructor for a failed capture event.
     */
    public DataPaper(String error) {
        this.captureImage = new Mat(); // Return an empty Mat
        this.isSuccess = false;
        this.statusMessage = error;
        this.arucoId = -1; // -1 indicates no ID was found
        this.rvec = new double[0];
        this.tvec = new double[0];
    }

    /**
     * Legacy constructor.
     */
    public DataPaper(Mat mat, boolean b, int inputpaper, int i) {
        this.captureImage = mat;
        this.isSuccess = b;
        this.paperNumber = inputpaper;
        this.arucoId = i;
        this.rvec = new double[0];
        this.tvec = new double[0];
    }

    /** * สร้าง PaperPose เปล่าที่รอใส่ค่า later
     */
    public DataPaper() {
        this.paperNumber = 0;
        this.arucoId = 0;
        this.rvec = new double[]{0, 0, 0};
        this.tvec = new double[]{0, 0, 0};
    }

    // --- Getters & Setters ---

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

    public Mat getCameraMatrix() {
        return cameraMatrix;
    }

    public Mat getDstMatrix() {
        return dstMatrix;
    }

    public Point getPointPaper() {
        return pointPaper;
    }

    public void setCameraMatrix(Mat cameraMatrix) {
        this.cameraMatrix = cameraMatrix;
    }

    public void setDstMatrix(Mat dstMatrix) {
        this.dstMatrix = dstMatrix;
    }

    public void setPointPaper(Point pointPaper) {
        this.pointPaper = pointPaper;
    }

    /**
     * กำหนดค่า rvec ใหม่ (ต้องมีความยาว 3)
     * @param rvec double[3]
     */
    public void setRvec(double[] rvec) {
        if (rvec != null && rvec.length == 3) {
            System.arraycopy(rvec, 0, this.rvec, 0, 3);
        }
    }

    public double[] getTvec() {
        return tvec;
    }

    /**
     * กำหนดค่า tvec ใหม่ (ต้องมีความยาว 3)
     * @param tvec double[3]
     */
    public void setTvec(double[] tvec) {
        if (tvec != null && tvec.length == 3) {
            System.arraycopy(tvec, 0, this.tvec, 0, 3);
        }
    }

    // --- Overridden Methods ---

    @Override
    public String toString() {
        return "DataPaper{" +
                "paperNumber=" + paperNumber +
                ", isSuccess=" + isSuccess +
                ", arucoId=" + arucoId +
                ", rvec=[" + rvec[0] + ", " + rvec[1] + ", " + rvec[2] + "]" +
                ", tvec=[" + tvec[0] + ", " + tvec[1] + ", " + tvec[2] + "]" +
                '}';
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