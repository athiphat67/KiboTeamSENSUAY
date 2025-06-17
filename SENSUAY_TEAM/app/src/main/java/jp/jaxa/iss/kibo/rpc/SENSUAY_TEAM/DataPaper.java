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
        Log.i(tag, "========== DataPaper Log ==========");

        Log.i(tag, "isSuccess: " + isSuccess);
        Log.i(tag, "statusMessage: '" + statusMessage + "'");

        Log.i(tag, "paperNumber: " + paperNumber);
        Log.i(tag, "arucoId: " + arucoId);
        Log.i(tag, "targetItem: '" + targetItem + "'");

        Log.i(tag, "rvec: " + Arrays.toString(rvec));
        Log.i(tag, "tvec: " + Arrays.toString(tvec));
        Log.i(tag, "pointPaper: " + (pointPaper != null ? pointPaper.toString() : "null"));

        Log.i(tag, "posNow: " + (posNow != null ? posNow.toString() : "null"));
        Log.i(tag, "quaternionNow: " + (quaternionNow != null ? quaternionNow.toString() : "null"));

        String captureInfo = (captureImage != null) ? "Mat[width=" + captureImage.width() + ", height=" + captureImage.height() + "]" : "null";
        Log.i(tag, "captureImage: " + captureInfo);

        String cornersInfo = (keepcorners != null) ? "List<Mat>[size=" + keepcorners.size() + "]" : "null";
        Log.i(tag, "keepcorners: " + cornersInfo);

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
            NewZ = posNow.getPosition().getZ() + NewTvec[2] ;

        }

        return new Point(NewX, NewY, NewZ);
    }

    private double FindAngleForAreaTwoOrThree(DataPaper dataPaper) {

        double xPos, yPos, zPos, x0, y0, z0, xPaper, yPaper, zPaper;
        double tanTheta;
        double resultTheta = 0;
        double A;
        double B;
        int numTurn;

        xPos = dataPaper.getPosNow().getPosition().getX();
        yPos = dataPaper.getPosNow().getPosition().getY();
        zPos = dataPaper.getPosNow().getPosition().getZ();
        x0 = dataPaper.getPosNow().getPosition().getX();
        y0 = dataPaper.getPointPaper().getY();
        z0 = dataPaper.getPointPaper().getZ();
        xPaper = dataPaper.getPointPaper().getX();
        yPaper = dataPaper.getPointPaper().getY();
        zPaper = dataPaper.getPointPaper().getZ();

        if (xPaper < xPos) {

            A = Math.abs(xPos - xPaper);
            B = Math.abs(yPos - yPaper);

            if (B == 0) resultTheta = 0;

            tanTheta = Math.atan(A / B);

            numTurn = 1;

        } else if (xPaper > xPos) {

            A = Math.abs(xPaper - xPos);
            B = Math.abs(yPos - yPaper);

            if (B == 0) resultTheta = 0;

            tanTheta = Math.atan(A / B);

            numTurn = -1;

        } else {

            tanTheta = 0;
            numTurn = 0;
        }

        resultTheta = Math.toDegrees(tanTheta);
        resultTheta *= numTurn;

        return  resultTheta;
    }

    public static Point findClosestPointOnConeBase(Point a, Point b) {

        final double BASE_Z = 4.57; // ระนาบ Z ของฐานกรวย
        final double ALPHA_RADIANS = Math.toRadians(30.0); // มุมครึ่งหนึ่งของกรวยในหน่วยเรเดียน (30 องศา)

        double pX = a.getX(), pY = a.getY(), pZ = a.getZ();
        double refX = b.getX(), refY = b.getY(), refZ = b.getZ();

        // 1. หารัศมีของฐานกรวย (R)
        double height = Math.abs(pZ - BASE_Z); // ความสูงของกรวย (H)
        double radius = height * Math.atan(ALPHA_RADIANS); // รัศมีของฐานกรวย (R)

        // 2. จุดศูนย์กลางของฐานกรวย
        // เนื่องจากแกนกรวยขนานกับแกน Z, จุดศูนย์กลางฐานคือ (pX, pY, BASE_Z)
        double centerX = pX;
        double centerY = pY;

        // 3. หาเวกเตอร์จากจุดศูนย์กลางฐานไปยังจุดอ้างอิง (V) ในระนาบ XY
        double vecX = refX - centerX;
        double vecY = refY - centerY;

        // 4. คำนวณขนาดของเวกเตอร์ (magnitude |V|)
        double magnitudeV = Math.sqrt(vecX * vecX + vecY * vecY);

        // 5. หาจุดที่ใกล้ที่สุด (xN, yN, zN)
        double closestX;
        double closestY;

        // ตรวจสอบกรณีที่จุดอ้างอิงอยู่ตรงกับจุดศูนย์กลางฐาน เพื่อหลีกเลี่ยงการหารด้วยศูนย์
        if (magnitudeV == 0) {
            // ถ้าจุดอ้างอิงอยู่ตรงกลางฐาน ให้เลือกจุดใดๆ บนขอบวงกลมได้
            // เช่น เลือกจุดที่ (centerX + R, centerY)
            closestX = centerX + radius;
            closestY = centerY;
        } else {
            // หาเวกเตอร์หน่วยในทิศทางนั้น แล้วคูณด้วยรัศมี R
            closestX = centerX + radius * (vecX / magnitudeV);
            closestY = centerY + radius * (vecY / magnitudeV);
        }

        double closestZ = BASE_Z; // จุดนี้อยู่บนฐานกรวย

        return new Point(closestX , closestY, closestZ);
    }
}