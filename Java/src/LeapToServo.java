/******************************************************************************\
 * Author: Justin Platz : @JustinMPlatz                                        *
 * This code is completely open-source. Do with it as you please.              *
\******************************************************************************/

import java.io.IOException;
import java.lang.Math;
import com.leapmotion.leap.*;
import com.pubnub.api.*;
import org.json.*;

public class LeapToServo implements Runnable{
    public static final String CHANNEL = "leap2pi";
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



        try {

            pubnub.subscribe("leap2pi", new Callback() {
                        @Override
                        public void connectCallback(String channel, Object message) {
                        }

                        @Override
                        public void disconnectCallback(String channel, Object message) {
                            System.out.println("SUBSCRIBE : DISCONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                        }

                        public void reconnectCallback(String channel, Object message) {
                            System.out.println("SUBSCRIBE : RECONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                        }

                        @Override
                        public void successCallback(String channel, Object message) {

                        }

                        @Override
                        public void errorCallback(String channel, PubnubError error) {
                            System.out.println("SUBSCRIBE : ERROR on channel " + channel
                                    + " : " + error.toString());
                        }
                    }
            );
        } catch (PubnubException e) {
            System.out.println(e.toString());
        }
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
            cleanup();
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

        JSONObject payload = new JSONObject();
        int theByte = fingersToByte(hand);
        int oldYaw    = (isLeft) ? oldLeftYaw   : oldRightYaw;
        int oldPitch  = (isLeft) ? oldLeftPitch : oldRightPitch;
        if( (Math.abs(oldPitch - pitch) > 5)  || (Math.abs(oldYaw - yaw) > 5) ) {
            try {
                payload.put(handName + "_yaw",   yaw);
                payload.put(handName + "_pitch", pitch);
                payload.put(handName + "_byte",  theByte);
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
            try {
                payload.put(handName + "_yaw", oldYaw);
                payload.put(handName + "_pitch", oldPitch);
                payload.put(handName + "_byte", theByte);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return payload;
    }

    public void captureFrame(Controller controller) {
        // Get the most recent frame and report some basic information
        Frame frame = controller.frame();
        JSONObject payload = new JSONObject();
        for (Hand hand : frame.hands()) {
            try {
                if (hand.isLeft()) {
                    payload.put("left_hand", handleHand(hand));
                } else {
                    payload.put("right_hand", handleHand(hand));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(!payload.toString().equals("{}")) {
            pubnub.publish(CHANNEL, payload, new Callback() { });
        }

    }

    public void cleanup(){
        try {
            JSONObject payload = new JSONObject();
            JSONObject left    = new JSONObject();
            JSONObject right   = new JSONObject();
            left.put("left_yaw",  400);
            left.put("left_pitch",400);
            left.put("left_byte", (1 << 4) - 1);
            right.put("right_yaw",  400);
            right.put("right_pitch",400);
            right.put("right_byte", ((1 << 4) - 1) << 4);
            payload.put("left_hand",  left);
            payload.put("right_hand", right);
            this.pubnub.publish(CHANNEL, payload, new Callback() {});
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * Implementation of the Runnable interface.
     */
    public void run(){
        for(;;) {
            if (!running) break;
            captureFrame(this.controller);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        String pubKey = "Your_Pub_Key";
        String subKey = "Your_Sub_Key";

        LeapToServo s = new LeapToServo(pubKey, subKey);
        s.startTracking();
    }
}
