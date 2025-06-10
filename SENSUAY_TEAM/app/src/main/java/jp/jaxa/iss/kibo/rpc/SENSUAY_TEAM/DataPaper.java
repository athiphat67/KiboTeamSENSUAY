package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import org.opencv.core.Mat;

/**
 * A data class to hold all relevant information from a single paper capture event.
 * This includes the processed image, success status, and pose data (rvec, tvec).
 */
public class DataPaper {

    // --- Fields ---
    private Mat captureImage;
    private boolean isSuccess;
    private String statusMessage;
    private int paperNumber;
    private int arucoId;
    private double[] rvec;
    private double[] tvec;

    // --- Constructors ---

    /**
     * The main constructor for a successful capture event.
     */
    public DataPaper(Mat captureImage, boolean isSuccess, int paperNumber, int arucoId, double[] rvec, double[] tvec) {
        this.captureImage = captureImage;
        this.isSuccess = isSuccess;
        this.paperNumber = paperNumber;
        this.arucoId = arucoId;

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
}