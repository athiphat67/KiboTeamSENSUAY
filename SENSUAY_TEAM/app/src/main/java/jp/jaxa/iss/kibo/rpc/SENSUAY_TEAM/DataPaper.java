package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import org.opencv.core.Point;

public class DataPaper {
    // หมายเลขกระดาษ (เช่น 1, 2, 3 … หรือ 23 ถ้าต้องการเก็บสองหลัก)
    private int paperNumber;
    // Aruco ID ที่ตรวจเจอ (มักได้มาจาก ids.get(...)[0])
    private int arucoId;
    // เวกเตอร์การหมุน (rvec) ขนาด 3 (rx, ry, rz)
    private double[] rvec;
    // เวกเตอร์การแปล (tvec) ขนาด 3 (tx, ty, tz)
    private double[] tvec;

    /**
     * สร้าง PaperPose โดยระบุค่าทั้งหมดเลย
     * @param paperNumber  หมายเลขกระดาษ
     * @param arucoId      หมายเลข Aruco ID
     * @param rvec         double[3] (rx, ry, rz)
     * @param tvec         double[3] (tx, ty, tz)
     */
    public DataPaper(int paperNumber, int arucoId, double[] rvec, double[] tvec) {
        this.paperNumber = paperNumber;
        this.arucoId = arucoId;

        // ตรวจความยาว array ว่าถูกต้องหรือไม่
        if (rvec != null && rvec.length == 3) {
            this.rvec = new double[3];
            System.arraycopy(rvec, 0, this.rvec, 0, 3);
        } else {
            // ถ้าไม่ใช่ขนาด 3 ก็สร้าง array ค่า 0 ทั้งหมด
            this.rvec = new double[] {0, 0, 0};
        }

        if (tvec != null && tvec.length == 3) {
            this.tvec = new double[3];
            System.arraycopy(tvec, 0, this.tvec, 0, 3);
        } else {
            this.tvec = new double[] {0, 0, 0};
        }
    }

    /** สร้าง PaperPose เปล่าที่รอใส่ค่า later */
    public DataPaper() {
        this.paperNumber = 0;
        this.arucoId = 0;
        this.rvec = new double[] {0, 0, 0};
        this.tvec = new double[] {0, 0, 0};
    }

    // ------- Getters & Setters -------

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

    @Override
    public String toString() {
        return "PaperPose{" +
                "paperNumber=" + paperNumber +
                ", arucoId=" + arucoId +
                ", rvec=[" + rvec[0] + ", " + rvec[1] + ", " + rvec[2] + "]" +
                ", tvec=[" + tvec[0] + ", " + tvec[1] + ", " + tvec[2] + "]" +
                '}';
    }
}

