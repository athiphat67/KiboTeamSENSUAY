package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import android.util.Log;

import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
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
        //this.pointPaper = calculateWorldPoint();
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

    public List<Mat> getKeepcorners() {
        return keepcorners;
    }

    @Override
    public String toString() {
        return "DataPaper{...}";
    }

    // แนะนำให้ประกาศ TAG ไว้ที่ด้านบนของคลาสเพื่อความเป็นระเบียบ
    private static final String TAG = "WorldPointCalcs";

    private Point calculateWorldPoint() throws IllegalArgumentException {

        // 1. ดึงข้อมูลอินพุต
        Quaternion cameraQuaternion = this.getQuaternionNow();
        double qx = cameraQuaternion.getX();
        double qy = cameraQuaternion.getY();
        double qz = cameraQuaternion.getZ();
        double qw = cameraQuaternion.getW();
        Log.i(TAG, "Input Quaternion (x,y,z,w): [" + qx + ", " + qy + ", " + qz + ", " + qw + "]");

        Point cameraPosition = this.posNow.getPosition();
        double Px = cameraPosition.getX();
        double Py = cameraPosition.getY();
        double Pz = cameraPosition.getZ();
        Log.i(TAG, "Input Camera Position (X,Y,Z): [" + Px + ", " + Py + ", " + Pz + "]");

        double tvec0 = tvec[0] / 10.0 ;
        double tvec1 = tvec[1] / 10.0 ;
        double tvec2 = tvec[2] / 10.0 ;
        Log.i(TAG, "Input TVec (from camera): [" + tvec0 + ", " + tvec1 + ", " + tvec2 + "]");

        Mat rotationVector = new Mat(3, 1, CvType.CV_64F);

        // จัดการกรณีพิเศษที่ w มีค่าเกือบเป็น 1 (ไม่มีการหมุน) เพื่อหลีกเลี่ยงการหารด้วยศูนย์
        if (qw * qw < 1.0 - 1e-8) {
            double angle = 2 * Math.acos(qw);
            double scale = Math.sqrt(1 - qw * qw);

            rotationVector.put(0, 0, angle * qx / scale);
            rotationVector.put(1, 0, angle * qy / scale);
            rotationVector.put(2, 0, angle * qz / scale);
        } else {
            // Identity quaternion, no rotation. Rotation vector is (0,0,0)
            rotationVector.put(0, 0, new double[]{0, 0, 0});
        }
        Log.i(TAG, "Intermediate Rotation Vector: " + rotationVector.dump());


        // ใช้ Rodrigues เพื่อแปลง Rotation Vector เป็น Rotation Matrix
        Mat rotationMatrix = new Mat(3, 3, CvType.CV_64F);
        Calib3d.Rodrigues(rotationVector, rotationMatrix);

        double r11 = rotationMatrix.get(0, 0)[0];
        double r12 = rotationMatrix.get(0, 1)[0];
        double r13 = rotationMatrix.get(0, 2)[0];

        double r21 = rotationMatrix.get(1, 0)[0];
        double r22 = rotationMatrix.get(1, 1)[0];
        double r23 = rotationMatrix.get(1, 2)[0];

        double r31 = rotationMatrix.get(2, 0)[0];
        double r32 = rotationMatrix.get(2, 1)[0];
        double r33 = rotationMatrix.get(2, 2)[0];

        // 3. คำนวณพิกัดของ Marker ใน World Frame
        double worldX = r11 * tvec0 + r12 * tvec1 + r13 * tvec2 + Px;
        double worldY = r21 * tvec0 + r22 * tvec1 + r23 * tvec2 + Py;
        double worldZ = r31 * tvec0 + r32 * tvec1 + r33 * tvec2 + Pz;
        Log.i(TAG, "==> Calculated World Point (X,Y,Z): [" + worldX + ", " + worldY + ", " + worldZ + "]");

        if (paperNumber == 1 ) {

            worldZ -= 0.15 ;

        } else if (paperNumber == 2) {

            worldX -= 0.15;
            worldY -= 0.25;

        } else if (paperNumber == 3) {

            worldX -= 0.15;

        } else if (paperNumber == 4) {

            worldZ += 0.15;

        }


        // 4. ตรวจสอบว่าพิกัดอยู่ในช่วงของ KIZ 1 หรือไม่
        final double KIZ1_X_MIN = 10.3;
        final double KIZ1_X_MAX = 11.55;
        final double KIZ1_Y_MIN = -10.2;
        final double KIZ1_Y_MAX = -6.0;
        final double KIZ1_Z_MIN = 4.32;
        final double KIZ1_Z_MAX = 5.57;

        if (worldX < KIZ1_X_MIN || worldX > KIZ1_X_MAX ||
                worldY < KIZ1_Y_MIN || worldY > KIZ1_Y_MAX ||
                worldZ < KIZ1_Z_MIN || worldZ > KIZ1_Z_MAX) {

            // สร้างข้อความสำหรับ Log และ Exception
            String errorMessage = "Calculated point [" + worldX + ", " + worldY + ", " + worldZ + "] is outside the KIZ 1 zone.";

            // Log เป็น Error เพื่อให้เห็นใน Logcat เป็นสีแดง
            Log.e(TAG, "ExceptionKIZ: " + errorMessage);

            // โยน Exception พร้อมข้อความ
            throw new IllegalArgumentException(errorMessage);
        }

        // 5. สร้างและส่งคืนผลลัพธ์
        Log.d(TAG, "Point is within KIZ 1 bounds. Returning result.");
        Point pointResult = new Point(worldX, worldY, worldZ); // สมมติว่า Point มี constructor แบบนี้

        return pointResult;
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



}