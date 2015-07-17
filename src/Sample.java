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
import com.leapmotion.leap.Gesture.State;

import com.pubnub.api.*;
import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;
import org.json.*;
import java.io.FileWriter;
import java.io.IOException;



class SampleListener extends Listener {
    int counter = 0;

    public void onInit(Controller controller) {
        System.out.println("Initialized");
    }

    public void onConnect(Controller controller) {
        System.out.println("Connected");
        controller.enableGesture(Gesture.Type.TYPE_SWIPE);
        controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
        controller.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
        controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
    }

    public void onDisconnect(Controller controller) {
        //Note: not dispatched when running in a debugger.
        System.out.println("Disconnected");
    }

    public void onExit(Controller controller) {
        System.out.println("Exited");
    }

    public void onFrame(Controller controller) {
        Pubnub pubnub = new Pubnub("pub-c-f83b8b34-5dbc-4502-ac34-5073f2382d96", "sub-c-34be47b2-f776-11e4-b559-0619f8945a4f");

        // Get the most recent frame and report some basic information
        Frame frame = controller.frame();

//        System.out.println("Frame id: " + frame.id()
//                         + ", timestamp: " + frame.timestamp()
//                         + ", hands: " + frame.hands().count()
//                         + ", fingers: " + frame.fingers().count()
//                         + ", tools: " + frame.tools().count()
//                         + ", gestures " + frame.gestures().count());

        //Get hands

        for(Hand hand : frame.hands()) {
            String handType = hand.isLeft() ? "Left hand" : "Right hand";
//            System.out.println("  " + handType + ", id: " + hand.id()
//                             + ", palm position: " + hand.palmPosition());

            // Get the hand's normal vector and direction
            Vector normal = hand.palmNormal();
            Vector direction = hand.direction();

            // Calculate the hand's pitch, roll, and yaw angles
//            System.out.println("  pitch: " + Math.toDegrees(direction.pitch()) + " degrees, "
//                    + "roll: " + Math.toDegrees(normal.roll()) + " degrees, "
//                    + "yaw: " + Math.toDegrees(direction.yaw()) + " degrees");


            FingerList extendedFingerList = hand.fingers().extended();
            FingerList indexFingerList = hand.fingers().fingerType(Finger.Type.TYPE_INDEX);

                if( indexFingerList.get(0).isExtended() && hand.isLeft() && extendedFingerList.count() == 1){
                    //System.out.println(hand.isLeft());
                    System.out.println(indexFingerList.get(0).isExtended());
                    Finger finger = indexFingerList.get(0);
                    //pubnub.publish("my_channel", "Finger is of type: " + finger.type(), new Callback() {});
                }


            // Get arm bone
            Arm arm = hand.arm();
//            System.out.println("  Arm direction: " + arm.direction()
//                             + ", wrist position: " + arm.wristPosition()
//                             + ", elbow position: " + arm.elbowPosition());

            // Get fingers
            for (Finger finger : hand.fingers()) {
//                System.out.println("    " + finger.type() + ", id: " + finger.id()
//                                 + ", length: " + finger.length()
//                                 + "mm, width: " + finger.width() + "mm");

                //Get Bones
                for(Bone.Type boneType : Bone.Type.values()) {
                    Bone bone = finger.bone(boneType);
//                    System.out.println("      " + bone.type()
//                                     + " bone, start: " + bone.prevJoint()
//                                     + ", end: " + bone.nextJoint()
//                                     + ", direction: " + bone.direction());
                }
            }
        }

        // Get tools
        for(Tool tool : frame.tools()) {
//            System.out.println("  Tool id: " + tool.id()
//                             + ", position: " + tool.tipPosition()
//                             + ", direction: " + tool.direction());
        }

        GestureList gestures = frame.gestures();
        for (int i = 0; i < gestures.count(); i++) {
            Gesture gesture = gestures.get(i);

            switch (gesture.type()) {
                case TYPE_CIRCLE:
                    CircleGesture circle = new CircleGesture(gesture);

                    // Calculate clock direction using the angle between circle normal and pointable
                    String clockwiseness;
                    if (circle.pointable().direction().angleTo(circle.normal()) <= Math.PI/2) {
                        // Clockwise if angle is less than 90 degrees
                        clockwiseness = "clockwise";
                    } else {
                        clockwiseness = "counterclockwise";
                    }

                    // Calculate angle swept since last frame
                    double sweptAngle = 0;
                    if (circle.state() != State.STATE_START) {
                        CircleGesture previousUpdate = new CircleGesture(controller.frame(1).gesture(circle.id()));
                        sweptAngle = (circle.progress() - previousUpdate.progress()) * 2 * Math.PI;
                    }

//                    System.out.println("  Circle id: " + circle.id()
//                               + ", " + circle.state()
//                               + ", progress: " + circle.progress()
//                               + ", radius: " + circle.radius()
//                               + ", angle: " + Math.toDegrees(sweptAngle)
//                               + ", " + clockwiseness);
                    break;
                case TYPE_SWIPE:
                    SwipeGesture swipe = new SwipeGesture(gesture);
//                    System.out.println("  Swipe id: " + swipe.id()
//                               + ", " + swipe.state()
//                               + ", position: " + swipe.position()
//                               + ", direction: " + swipe.direction()
//                               + ", speed: " + swipe.speed());
                    break;
                case TYPE_SCREEN_TAP:
                    ScreenTapGesture screenTap = new ScreenTapGesture(gesture);
//                    System.out.println("  Screen Tap id: " + screenTap.id()
//                               + ", " + screenTap.state()
//                               + ", position: " + screenTap.position()
//                               + ", direction: " + screenTap.direction());
                    break;
                case TYPE_KEY_TAP:
                    KeyTapGesture keyTap = new KeyTapGesture(gesture);
//                    System.out.println("  Key Tap id: " + keyTap.id()
//                               + ", " + keyTap.state()
//                               + ", position: " + keyTap.position()
//                               + ", direction: " + keyTap.direction());
                    break;
                default:
//                    System.out.println("Unknown gesture type.");
                    break;
            }
        }

        if (!frame.hands().isEmpty() || !gestures.isEmpty()) {
            //System.out.println();
        }
    }
}

class Sample {
    public static void leapTest(Controller controller) {
        Pubnub pubnub = new Pubnub("pub-c-f83b8b34-5dbc-4502-ac34-5073f2382d96", "sub-c-34be47b2-f776-11e4-b559-0619f8945a4f");
        int right_yaw   = 0;
        int left_yaw    = 0;
        int right_pitch = 0;
        int left_pitch  = 0;

        // Get the most recent frame and report some basic information
        Frame frame = controller.frame();

        HandList allHands = frame.hands();
        Hand left_hand = frame.hands().leftmost();
        Hand right_hand = frame.hands().rightmost();

        for (Hand hand : frame.hands()) {

            // Get the hand's normal vector and direction
            Vector normal = hand.palmNormal();
            Vector direction = hand.direction();

            if (hand.isLeft()){
                left_yaw = (int) Math.toDegrees(direction.yaw());
                left_pitch = (int) Math.toDegrees(direction.pitch());
            }
            else{
                right_yaw = (int) Math.toDegrees(direction.yaw());
                right_pitch = (int) Math.toDegrees(direction.pitch());
            }

            // Calculate the hand's pitch, roll, and yaw angles
//            System.out.println("  pitch: " + Math.toDegrees(direction.pitch()) + " degrees, "
//                    + "roll: " + Math.toDegrees(normal.roll()) + " degrees, "
//                    + "yaw: " + Math.toDegrees(direction.yaw()) + " degrees");

            JSONObject payload = new JSONObject();
            try{
                payload.put("left_yaw",left_yaw);
                payload.put("left_pitch",left_pitch);
                payload.put("right_yaw",right_yaw);
                payload.put("right_pitch",right_pitch);
            }
            catch (JSONException e){
                System.out.println(e);
            }

            pubnub.publish("my_channel", payload, new Callback() {});


            FingerList extendedFingerList = hand.fingers().extended();
            FingerList indexFingerList = hand.fingers().fingerType(Finger.Type.TYPE_INDEX);

//            if (indexFingerList.get(0).isExtended() && hand.isLeft() && extendedFingerList.count() == 1) {
//                System.out.println(indexFingerList.get(0).isExtended());
//                Finger finger = indexFingerList.get(0);
//            }
        }

    }
    public static int radiansToAdjustedDegrees(int radians){
        // take radian reading and return degree value adjusted for our desired range/midpoint of servo range
        int degrees = radians * (int) (180 / Math.PI);
        degrees = (int) Math.floor(degrees + 90);
        return degrees;
    }



    public static void main(String[] args) {

        // Create a sample listener and controller
        SampleListener listener = new SampleListener();
        Controller controller = new Controller();

        // Have the sample listener receive events from the controller
        //controller.addListener(listener);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Sample.leapTest(controller);
                    try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}
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
        // Remove the sample listener when done
        controller.removeListener(listener);
    }
}
