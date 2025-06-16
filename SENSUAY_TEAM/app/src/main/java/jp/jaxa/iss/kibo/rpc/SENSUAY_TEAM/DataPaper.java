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

    public DataPaper(Mat captureImage, Mat backupImage, boolean isSuccess, int paperNumber, int arucoId, double[] rvec, double[] tvec, List<Mat> keepcorners, Kinematics pos, Quaternion qnow) {

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
        this.pointPaper = FindPointPaper();
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

    private Point FindPointPaper() {

        double NewX;
        double NewY;
        double NewZ;

        if ( paperNumber == 1) {

            double[] NewTvec = {tvec[0], tvec[2], tvec[1]};
            NewX = posNow.getPosition().getX() + NewTvec[0];
            NewY = -10.58;
            NewZ = posNow.getPosition().getZ() + NewTvec[2];

        } else if ( paperNumber == 2 || paperNumber == 3){

            double[] NewTvec = {tvec[1], tvec[0], tvec[2]};
            NewX = posNow.getPosition().getX() + NewTvec[0];
            NewY = posNow.getPosition().getY() + NewTvec[1];
            NewZ = 3.76203;

        } else {

            double[] NewTvec = {tvec[2], -1 * tvec[0], tvec[1]};
            NewX = 9.886984;
            NewY = posNow.getPosition().getY() + NewTvec[1];
            NewZ = posNow.getPosition().getZ() + NewTvec[2];

        }

        return new Point(NewX, NewY, NewZ);
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