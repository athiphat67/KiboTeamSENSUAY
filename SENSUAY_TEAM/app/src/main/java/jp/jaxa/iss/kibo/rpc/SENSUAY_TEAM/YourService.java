package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {
    @Override
    protected void runPlan1() {

        // StartMissions
        api.startMission();

        // Move to Area1
        moveToArea1();

        // Move to Area2
        moveToArea2();

        // Move to Area3
        moveToArea3();

        // Move to Area4
        moveToArea4();

        // Move to Astronaut
        moveToAstronaut();

        // Shutdown
        api.shutdownFactory();

    }

    public void moveToArea1() {
        Point point = new Point(10.95, -9.88, 5.195);
        Quaternion quaternion = new Quaternion(0f, 0f, 0.707f, 0.707f);
        api.moveTo(point, quaternion, true);
    }

    public void moveToArea2() {
        Point point = new Point(10.925, -8.525, 4.76203);
        Quaternion quaternion = new Quaternion(0f, 0.707f, 0f, 0.707f);
        api.moveTo(point, quaternion, true);
    }

    public void moveToArea3() {
        Point point = new Point(10.925, -7.825, 4.76093);
        Quaternion quaternion = new Quaternion(0f, 0.707f, 0f, 0.707f);
        api.moveTo(point, quaternion, true);
    }

    public void moveToArea4() {
        Point point = new Point(11.143d, -6.7607d, 4.9654d);
        Quaternion quaternion = new Quaternion(0f, 0f, 0f, 1f);
        api.moveTo(point, quaternion, true);
    }

    public void moveToAstronaut() {
        Point point = new Point(11.143d, -6.7607d, 4.9654d);
        Quaternion quaternion = new Quaternion(0f, 0f, 0.707f, 0.707f);
        api.moveTo(point, quaternion, true);
        api.reportRoundingCompletion();
    }

    public void FindPaper() {

    }

}
