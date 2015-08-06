/******************************************************************************\
* Copyright (C) 2012-2013 Leap Motion, Inc. All rights reserved.               *
* Leap Motion proprietary and confidential. Not for distribution.              *
* Use subject to the terms of the Leap Motion SDK Agreement available at       *
* https://developer.leapmotion.com/sdk_agreement, or another agreement         *
* between Leap Motion and you, your company or other organization.             *
\******************************************************************************/

import java.io.IOException;
import java.lang.Math;
import com.leapmotion.leap.*;

import com.pubnub.api.*;
import org.json.*;

public class LeapToServo implements Runnable{

    private Pubnub pubnub;
    private Controller controller;
    private boolean running;

    int oldLeftYaw    = 0;
    int oldLeftPitch  = 0;
    int oldRightYaw   = 0;
    int oldRightPitch = 0;

    public LeapToServo(String pubKey, String subKey){
        pubnub = new Pubnub(pubKey, subKey);
        pubnub.setUUID("LeapController");
    }

    public void startTracking(){
        // Create a controller
        this.controller = new Controller();
        this.running = true;
        Thread t = new Thread(this);
        t.start();

        // Keep this process running until Enter is pressed
        System.out.println("Press Enter to quit...");
        try {
            System.in.read();
            this.running=false;
            t.join();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    /**
     * Take radian reading and return degree value adjusted for our desired range/midpoint of servo range
     * @param radians Radian value to be converted
     * @return Adjusted degree value
     */
    public static int radiansToAdjustedDegrees(int radians){
        int degrees = (int) (radians * (180 / Math.PI));
        degrees = (int) Math.floor(degrees + 90);
        return degrees;
    }

    /**
     * Get a PWM value from degree closely modeled by a quadratic equation
     * @param degree pitch degree value
     * @return PWM value
     */
    public static double pitchDegreeToPWM(double degree){
        double a = 0.00061728395;
        double b = 2.38888888889;
        double c = 150;
        return a*(degree*degree) + b*degree + c;
    }

    /**
     * Get a PWM value from degree closely modeled by a quadratic equation
     * @param degree pitch degree value
     * @return PWM value
     */
    public static double yawDegreeToPWM(double degree){
        double a = 0.0;
        double b = 3.19444444;
        double c = 150;
        return a*(degree*degree) + b*degree + c;
    }

    /**
     * Force a value to be between 0 and 180 degrees for servo
     * @param value degree value returned by Leap Controller
     * @return normalized value between 0-180
     */
    public static int normalizeDegree(int value){
        value = (value > 90)  ? 90  : value;
        value = (value < -90) ? -90 : value;
        return value+90;
    }

    public static int fingersToByte(Hand hand) {
        int theByte = 0;
        int value = 0;

        if (hand.isRight()) {
            for (int j = 1; j < 5; ++j) {
                switch (j) {
                    case 4:
                        value = 0;
                        break;
                    default:
                        value = j;
                        break;
                }
                if (hand.fingers().get(j).isExtended()) {
                    theByte = theByte | (1 << value);
                }
            }
            theByte <<= 4;
        } else if (hand.isLeft()) {
            for (int i = 1; i < 5; ++i) { //  i = 4; v = 1
                switch (i) {
                    case 1:
                        value = 0;
                        break;
                    case 2:
                        value = 3;
                        break;
                    case 3:
                        value = 2;
                        break;
                    case 4:
                        value = 1;
                        break;
                    default:
                        break;
                }
                if (hand.fingers().get(i).isExtended()) {
                    theByte = theByte | (1 << value);
                }
            }
        }
        return theByte;
    }

    public JSONObject handleHand(Hand hand){
        boolean isLeft   = hand.isLeft();
        String handName  = (isLeft) ? "left" : "right";

        Vector direction = hand.direction();

        int yaw   = (int) Math.toDegrees(direction.yaw());
        int pitch = (int) Math.toDegrees(direction.pitch());

        // Normalize Yaw and Pitch
        yaw    = normalizeDegree(yaw);
        pitch *= (isLeft) ? -1 : 1;
        pitch  = normalizeDegree(pitch);

        // Get PWM Values
        yaw   = (int) yawDegreeToPWM(yaw);
        pitch = (int) pitchDegreeToPWM(pitch);

        JSONObject payloadLeft = new JSONObject();
        int theByte = fingersToByte(hand);
        int oldYaw    = (isLeft) ? oldLeftYaw   : oldRightYaw;
        int oldPitch  = (isLeft) ? oldLeftPitch : oldRightPitch;
        if( (Math.abs(oldPitch - pitch) > 5)  || (Math.abs(oldYaw - yaw) > 5) ) {
            System.out.println("Old left Yaw: " + oldYaw + " Current left yaw: " + yaw);
            System.out.println("Old left pitch: " + oldPitch + " Current left pitch: " + pitch);
            try {
                payloadLeft.put(handName + "_yaw",   yaw);
                payloadLeft.put(handName + "_pitch", pitch);
                payloadLeft.put(handName + "_byte",  theByte);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (isLeft) {
                this.oldLeftYaw   = yaw;
                this.oldLeftPitch = pitch;
            } else {
                this.oldRightYaw   = yaw;
                this.oldRightPitch = pitch;
            }
        }
        else{
            System.out.println("Beneath the threshold");
            System.out.println("Old left Yaw: " + oldYaw + " Current left yaw: " + yaw);
            System.out.println("Old left pitch: " + oldPitch + " Current left pitch: " + pitch);
            try {
                payloadLeft.put(handName + "_yaw", oldYaw);
                payloadLeft.put(handName + "_pitch", oldPitch);
                payloadLeft.put(handName + "_byte", theByte);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return payloadLeft;
    }

    public JSONObject handleLeft(Hand left_hand){
        Vector direction = left_hand.direction();

        int leftYaw   = (int) Math.toDegrees(direction.yaw());
        int leftPitch = (int) Math.toDegrees(direction.pitch());

        // Normalize Yaw and Pitch
        leftYaw    = normalizeDegree(leftYaw);
        leftPitch *= -1;
        leftPitch  = normalizeDegree(leftPitch);

        // Get PWM Values
        leftYaw   = (int) yawDegreeToPWM(leftYaw);
        leftPitch = (int) pitchDegreeToPWM(leftPitch);

        JSONObject payloadLeft = new JSONObject();
        int theByte = fingersToByte(left_hand);
        if( (Math.abs(oldLeftPitch - leftPitch) > 5)  || (Math.abs(oldLeftYaw - leftYaw) > 5) ) {
            System.out.println("Old left Yaw: " + oldLeftYaw + " Current left yaw: " + leftYaw);
            System.out.println("Old left pitch: " + oldLeftPitch + " Current left pitch: " + leftPitch);
            try {
                payloadLeft.put("left_yaw",   leftYaw);
                payloadLeft.put("left_pitch", leftPitch);
                payloadLeft.put("left_byte",  theByte);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            oldLeftYaw = leftYaw;
            oldLeftPitch = leftPitch;
        }
        else{
            System.out.println("Beneath the threshold");
            System.out.println("Old left Yaw: " + oldLeftYaw + " Current left yaw: " + leftYaw);
            System.out.println("Old left pitch: " + oldLeftPitch + " Current left pitch: " + leftPitch);
            try {
                payloadLeft.put("left_yaw", oldLeftYaw);
                payloadLeft.put("left_pitch", oldLeftPitch);
                payloadLeft.put("left_byte", theByte);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return payloadLeft;
    }

    public JSONObject handleRight(Hand right_hand){
        Vector direction = right_hand.direction();
        //Vector normal = hand.palmNormal();

        int rightYaw = (int) Math.toDegrees(direction.yaw());
        int rightPitch = (int) Math.toDegrees(direction.pitch());

        // Normalize Right Pitch and Yaw
        rightPitch = normalizeDegree(rightPitch);
        rightYaw   = normalizeDegree(rightYaw);

        // Get PWM values
        rightYaw   = (int) yawDegreeToPWM(rightYaw);
        rightPitch = (int) pitchDegreeToPWM(rightPitch);

        JSONObject payloadRight = new JSONObject();
        int theByte = fingersToByte(right_hand);
        if( (Math.abs(oldRightPitch - rightPitch) > 5)  || (Math.abs(oldRightYaw - rightYaw) > 5) ) {
            System.out.println("Old right Yaw: " + oldRightYaw + " Current right yaw: " + rightYaw);
            System.out.println("Old right pitch: " + oldRightPitch + " Current right pitch: " + rightPitch);
            try {
                payloadRight.put("right_yaw", rightYaw);
                payloadRight.put("right_pitch", rightPitch);
                payloadRight.put("right_byte", theByte);
            } catch (JSONException e) {
                System.out.println(e);
            }
            oldRightYaw = rightYaw;
            oldRightPitch = rightPitch;
        } else {
            try {
                payloadRight.put("right_yaw", rightYaw);
                payloadRight.put("right_pitch", rightPitch);
                payloadRight.put("right_byte", theByte);
            } catch (JSONException e) {
                System.out.println(e);
            }

        }
        return payloadRight;
    }

    public void captureFrame(Controller controller) {
        // Get the most recent frame and report some basic information
        Frame frame = controller.frame();
        System.out.println("**************");

        JSONObject payload = new JSONObject();
        for (Hand hand : frame.hands()) {
            try {
                if (hand.isLeft()) {
                    payload.put("left_hand", handleLeft(hand));
                } else {
                    payload.put("right_hand", handleRight(hand));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(!payload.toString().equals("{}")) {
            pubnub.publish("my_channel", payload, new Callback() { });
        }

    }

    /**
     * Implementation of the Runnable interface.
     */
    public void run(){
        for(;;) {
            if (!running) return;
            captureFrame(this.controller);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        String pubKey = "pub-c-f83b8b34-5dbc-4502-ac34-5073f2382d96";
        String subKey = "sub-c-34be47b2-f776-11e4-b559-0619f8945a4f";

        LeapToServo s = new LeapToServo(pubKey, subKey);
        s.startTracking();
    }
}
