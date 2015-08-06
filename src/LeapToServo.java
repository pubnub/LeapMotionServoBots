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

class LeapToServo {
    private Pubnub pubnub;
    int old_left_yaw    = 0;
    int old_left_pitch  = 0;
    int old_right_yaw    = 0;
    int old_right_pitch  = 0;

    public LeapToServo(){
        pubnub = new Pubnub("pub-c-f83b8b34-5dbc-4502-ac34-5073f2382d96", "sub-c-34be47b2-f776-11e4-b559-0619f8945a4f");
        pubnub.setUUID("leapController");
    }

    public void doIt(){
        // Create a controller
        Controller controller = new Controller();

        Runnable r = new Runnable() {
            @Override public void run() {
                while (true) {
                    leapTest(controller);
                    try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}
                }
            }
        };
        Thread t = new Thread(r);
        t.start();

        // Keep this process running until Enter is pressed
        System.out.println("Press Enter to quit...");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        t.interrupt();
    }

    public static double leftPitchDegreeToPWM(double degree){
        double a = .00061728395;
        double b = 2.38888888889;
        double c = 150;
        return a*(degree*degree) + b*degree + c;
    }

    public static double leftYawDegreeToPWM(double degree){
        double a = -.00;
        double b = 3.19444444;
        double c = 150;
        return a*(degree*degree) + b*degree + c;
    }

    public static int fingersToByte(Hand hand){
        int theByte = 0;
        int value = 0;


        if (hand.isRight()){
                for (int j = 1; j < 5; ++j){
                    switch (j) {
                        case 1:
                            value = 1;
                            break;
                        case 2:
                            value = 2;
                            break;
                        case 3:
                            value = 3;
                            break;
                        case 4:
                            value = 0;
                            break;
                        default:
                            break;
                    }
                if(hand.fingers().get(j).isExtended()){
                    theByte = theByte | (int) Math.pow(2.0, value);
                }
            }
            theByte = theByte << 4;
        }
        else if (hand.isLeft()){
                for (int i = 1; i < 5; ++i){
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
                    if(hand.fingers().get(i).isExtended()){
                        theByte = theByte | (int) Math.pow(2.0, value);
                    }
                }
        }

        return theByte;

    }



    public JSONObject handleLeft(Hand left_hand){

        JSONObject payloadleft = new JSONObject();

        Vector direction = left_hand.direction();

        int left_yaw    = 0;
        int left_pitch  = 0;

        left_yaw = (int) Math.toDegrees(direction.yaw());
        left_pitch = (int) Math.toDegrees(direction.pitch());

        //upper bound
        left_yaw = (left_yaw > 90)  ? 90  : left_yaw;
        //lower bound
        left_yaw = (left_yaw < -90) ? -90 : left_yaw;
        //normalize
        left_yaw += 90;

        left_pitch *= -1;
        //upper bound
        left_pitch = (left_pitch > 90)  ? 90  : left_pitch;
        //lower bound
        left_pitch = (left_pitch < -90) ? -90 : left_pitch;
        //normalize
        left_pitch += 90;


        left_yaw = (int) leftYawDegreeToPWM(left_yaw);
        left_pitch = (int) leftPitchDegreeToPWM(left_pitch);

        int theByte = fingersToByte(left_hand);
        if( (Math.abs(old_left_pitch - left_pitch) > 5)  || (Math.abs(old_left_yaw - left_yaw) > 5) ) {
            System.out.println("Old left Yaw: " + old_left_yaw + " Current left yaw: " + left_yaw);
            System.out.println("Old left pitch: " + old_left_pitch + " Current left pitch: " + left_pitch);
            try {
                payloadleft.put("left_yaw", left_yaw);
                payloadleft.put("left_pitch", left_pitch);
                payloadleft.put("left_byte", theByte);
            } catch (JSONException e) {
                System.out.println(e);
            }
            old_left_yaw = left_yaw;
            old_left_pitch = left_pitch;
        }
        else{
            System.out.println("Beneath the threshold");
            System.out.println("Old left Yaw: " + old_left_yaw + " Current left yaw: " + left_yaw);
            System.out.println("Old left pitch: " + old_left_pitch + " Current left pitch: " + left_pitch);
            try {
                payloadleft.put("left_yaw", old_left_yaw);
                payloadleft.put("left_pitch", old_left_pitch);
                payloadleft.put("left_byte", theByte);
            } catch (JSONException e) {
                System.out.println(e);
            }

        }
        return payloadleft;

    }

    public JSONObject handleRight(Hand right_hand){
        JSONObject payloadright = new JSONObject();

        Vector direction = right_hand.direction();
        //Vector normal = hand.palmNormal();

        int right_yaw   = 0;
        int right_pitch = 0;

        right_yaw = (int) Math.toDegrees(direction.yaw());
        right_pitch = (int) Math.toDegrees(direction.pitch());

        //todo make java method
        //upper bound
        right_pitch = (right_pitch > 90)  ? 90  : right_pitch;
        //lower bound
        right_pitch = (right_pitch < -90) ? -90 : right_pitch;
        //normalize
        right_pitch += 90;

        //upper bound
        right_yaw = (right_yaw > 90)  ? 90  : right_yaw;
        //lower bound
        right_yaw = (right_yaw < -90) ? -90 : right_yaw;
        //normalize
        right_yaw += 90;

        right_yaw = (int) leftYawDegreeToPWM(right_yaw); //todo fix name
        right_pitch = (int) leftPitchDegreeToPWM(right_pitch); // todo fix name


        int theByte = fingersToByte(right_hand);

        if( (Math.abs(old_right_pitch - right_pitch) > 5)  || (Math.abs(old_right_yaw - right_yaw) > 5) ) {
            System.out.println("Old right Yaw: " + old_right_yaw + " Current right yaw: " + right_yaw);
            System.out.println("Old right pitch: " + old_right_pitch + " Current right pitch: " + right_pitch);
            try {
                payloadright.put("right_yaw", right_yaw);
                payloadright.put("right_pitch", right_pitch);
                payloadright.put("right_byte", theByte);
            } catch (JSONException e) {
                System.out.println(e);
            }
            old_right_yaw = right_yaw;
            old_right_pitch = right_pitch;
        }
        else{
            try {
                payloadright.put("right_yaw", right_yaw);
                payloadright.put("right_pitch", right_pitch);
                payloadright.put("right_byte", theByte);
            } catch (JSONException e) {
                System.out.println(e);
            }

        }
        return payloadright;
    }

    public void leapTest(Controller controller) {

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
                System.out.println(e);
            }

        }
        if(!payload.toString().equals("{}")) {
            pubnub.publish("my_channel", payload, new Callback() {
            });
        }

    }

    public int radiansToAdjustedDegrees(int radians){

        // take radian reading and return degree value adjusted for our desired range/midpoint of servo range
        int degrees = (int) (radians * (180 / Math.PI));
        degrees = (int) Math.floor(degrees + 90);
        return degrees;
    }

    public static void main(String[] args) {
        LeapToServo s = new LeapToServo();
        s.doIt();
    }
}
