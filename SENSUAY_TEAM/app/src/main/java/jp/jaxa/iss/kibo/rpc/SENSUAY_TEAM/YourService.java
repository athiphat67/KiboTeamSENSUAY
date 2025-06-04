package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import org.opencv.core.Mat;

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
        moveToArea2();
        moveToArea3();
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
        Point p2 = new Point(11.175, -9.5, 5.245);
        Quaternion q2 = eulerToQuaternion(0, 0, -90);
        api.moveTo(p2, q2, true);

        captureAndSaveImage("area1_capture");
    }

    private void moveToArea2() {
        // point 3: oasis 2 → area 2
        Point p3 = new Point(11.175, -8.975, 5.245);
        Quaternion q3 = eulerToQuaternion(-90, 0, 0); // หันกล้องลง
        api.moveTo(p3, q3, true);

        captureAndSaveImage("area2_capture");
    }

    private void moveToArea3() {
        // point 4: oasis 3 → area 3
        Point p4 = new Point(10.7, -7.925, 5.245);
        Quaternion q4 = eulerToQuaternion(-90, 0, 0); // หันกล้องลง
        api.moveTo(p4, q4, true);

        captureAndSaveImage("area3_capture");
    }

    private void moveToArea4() {
        // point 5: oasis 4 → area 4
        Point p5 = new Point(11.175, -6.875, 4.685);
        Quaternion q5 = eulerToQuaternion(0, 0, 0); // หันหน้าไปด้านหน้า
        api.moveTo(p5, q5, true);

        captureAndSaveImage("area4_capture");
    }

    private void moveToAstronaut() {
        Point astroPoint = new Point(11.143d, -6.7607d, 4.9654d);
        Quaternion astroQ = new Quaternion(0f, 0f, 0.707f, 0.707f); // หันไปทางขวา (y+)
        api.moveTo(astroPoint, astroQ, false);
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

    public void FindPaper() {

    }
}