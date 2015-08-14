#Leap Motion Servo Bot

We use real-time data almost everyday in our lives but those interactions are often limited. We may read and write data in real-time but rarely do we think of interacting with real-time data as a physical experience.

With this project we set out to do just that. Using a Leap Motion, a Raspberry Pi and several micro-servos we created a tangible real-time experience. 

When you wave your hand over the Leap Motion, we created Servo Bots to mimic your every motion as you do them. Attached to our Servo Bots we placed 8x8 RGB LED Matrices, which react to each finger on your hand and we used PubNub to allow our Leap Motion to talk directly to our Raspberry Pi with minimal latency.

In this tutorial I am going to teach you how to create a Servo bot capable of dancing along with your hands using Leap Motion, Raspberry Pi, several micro-servos and PubNub.

Although in this example we use PubNub to communicate with our RPi in order to drive servos, the same techniques can be applied to control any internet connected device. PubNub is simply the communication layer used to allow any two devices to speak. In this case we use the Leap Motion as a RPi controller, but you can imagine using PubNub to initially configure devices or for real time data collection.

To recreate real-time mirroring the Leap Motion publishes messages 20x a second with information about each of your hands and all of your fingers to PubNub. On the other end, our Raspberry Pi is subscribed to the same channel and parses these messages to drive the servos and the lights.

Before we begin this tutorial, you will need several components:

* [Leap Motion](http://www.amazon.com/Leap-Motion-Controller-Packaging-Software/dp/B00HVYBWQO/ref=sr_1_1?ie=UTF8&qid=1438968456&sr=8-1&keywords=leap+motion) + [Leap Motion Java SDK](https://developer.leapmotion.com/documentation/java/index.html)
* [Raspberry Pi B+](http://www.amazon.com/Raspberry-Pi-Model-B/dp/B00Q91X3GM/ref=sr_1_3?s=toys-and-games&ie=UTF8&qid=1438968387&sr=1-3&keywords=raspberry+pi+b%2B)
* 4x [Tower Pro Micro Servo](http://www.amazon.com/TowerPro-SG90-Mini-Servo-Accessories/dp/B001CFUBN8) 
* [Adafruit PWM Servo Driver](https://www.adafruit.com/products/815)

If you are interested in how we created a custom driver for the LED Matrices or for questions about assembling the Servo Bots check out [here](www.google.com) or the completed code located in my GitHub repo [here](https://github.com/justinplatz/LeapMotionServoBots).

##Publishing To PubNub With Leap Motion

Leap Motion is an extremely powerful device equipped with two monochromatic IR cameras and three infrared LEDs. In this project, the Leap is just going to capture the pitch and yaw of our hands and publish them to a channel. Lucky for us, attributes like pitch, yaw and roll of hands are all pre-built into the Leap SDK. 

You will need to open up your favorite Java IDE and create a new project. We used [IntelliJ](https://www.jetbrains.com/idea/) with JDK8.

If you have never worked with a Leap Motion in Java before then I recommend you check out [here](https://developer.leapmotion.com/documentation/java/devguide/Project_Setup.html) to get a project set up with the Leap Motion Java SDK. 

Next you will want to install the PubNub Java SDK which you can grab from Maven. Get the most recent version (in this case we used 3.7.4).

If your project has all the proper imports at the top of the file you should see the following:
	
	import java.io.IOException;
	import java.lang.Math;
	import com.leapmotion.leap.*;
	import com.pubnub.api.*;
	import org.json.*;

Now that you have a project with both Leap Motion and PubNub SDKs installed we can get to it! It is crucial that you make your project implement Runnable so that we can have all Leap activity operate in its own thread.

Lets begin by setting up our project main, an implementation of the Runnable interface and initializing of global variables we will be using later like so:

        public class LeapToServo implements Runnable{
      
          //Define Channel name
          public static final String CHANNEL = "my_channel";
          //Create PubNub instance
          private Pubnub pubnub;
          //Create Leap Controller instance
          private Controller controller;
          //is Runnable running?
          private boolean running;
      
          //Last value of Left Yaw
          int oldLeftYaw    = 0;
          //Last value of Left Pitch
          int oldLeftPitch  = 0;
          //Last value of Right Yaw
          int oldRightYaw   = 0;
          //Last value of Right Pitch
          int oldRightPitch = 0;
      
          //Takes in Pubkey and Subkey and sets up PubNub Configuration
          public LeapToServo(String pubKey, String subKey){
              pubnub = new Pubnub(pubKey, subKey);
              pubnub.setUUID("LeapController");
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
              String pubKey = "demo";
              String subKey = "demo";
      
              LeapToServo s = new LeapToServo(pubKey, subKey);
              s.startTracking();
          }
        }


The Leap Motion captures about 300 frames each second. Within each frame we have access to tons of information about our hands such as the number of fingers extended, pitch, yaw, hand gestures and so much more.  Our servos move in a sweeping motion with 180 degrees of rotation. In order to simulate a hands motions we use two servos where one servo monitors the pitch (rotation around X-axis) of the hand and the other monitors the yaw (rotation around Y-axis). The result is a Servo Bot which can mimic most of a hands movements.

![image](https://developer.valvesoftware.com/w/images/7/7e/Roll_pitch_yaw.gif)

We will use a function to start tracking our hands which will initialize a new Leap `Controller`, start a new thread, and then process all the information about our hands. My function named `startTracking()` looked like this:

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

When our thread starts running it is going to call `captureFrame()` which will look at the most recent frame for any hands. If we find a hand we call a function `handleHand()` which will get the values of the hand's pitch & yaw which our servo will need. If our frame does in fact have hands we publish a message containing all relevant hand information. 

This was implemented like so:

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

The values our Leap outputs for pitch and yaw are in radians, however our Servos are expecting a pulse width modulation (or PWM) between 150 and 600MHz. Thus we do some fancy conversions to take our radians and convert them into degrees and then normalize our degrees into their corresponding PWM value. These functions look like this:

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

The last piece of code we need is the cleanup for when we press enter to kill the program and center our Servos. This code looks like this:

      public void cleanup(){
        try {
            JSONObject payload = new JSONObject();
            JSONObject left    = new JSONObject();
            JSONObject right   = new JSONObject();
            left.put("left_yaw",  400);
            left.put("left_pitch",400);
            right.put("right_yaw",  400);
            right.put("right_pitch",400);
            payload.put("left_hand",  left);
            payload.put("right_hand", right);
            this.pubnub.publish(CHANNEL, payload, new Callback() {});
        } catch (JSONException e){
            e.printStackTrace();
        }
      }

Try running your program and check out your debug console to view what your Leap is publishing. If all is correct you should see JSON that looks something like this: 

	{
		"right_hand":{
						"right_yaw":450,
						"right_pitch":300
					},
		"left_hand":{
						"left_yaw":450,
						"left_pitch":300
					}
	}
	
Now that our Leap os publishing all we need to do is set up our RPi to subscribe and parse the retrieved data to drive the Servos.

##Driving Servos With A RPi

Out of the box a RPi has native support for PWM. However, there is only one PWM channel available to users at GPIO18. In this tutorial we want to drive 4 Servos simultaneously, so we will need a different solution. Thankfully ... the PI does have HW I2C available, which we can use to communicate with a PWM driver like the Adafruit 16-channel 12-bit PWM/Servo Driver.

In order to use the PWM Servo Driver you will need to configure your Pi for I2C for help doing so you should check out the Adafruit tutorial [here](https://learn.adafruit.com/adafruit-16-channel-servo-driver-with-raspberry-pi). Do not worry about having the Adafruit Cobbler as you will not need it for this tutorial. If you did this part correctly then you should be able to run the example program and see your Servo spin!

Our program needs to do the following things:

1. Subscribe to PubNub and receive messages published from the Leap
2. Parse the JSON
3. Drive the Servos using our new values 

####Connecting the RPi, PWM Driver and Servos
Lets begin by connecting our PWM driver to our Rpi, and then our Servos to our PWM driver. Look at the following schematic for help.

Firstly, note that we require an external 5V power source for our servos. The RPi cannot draw enough power to power all 4 Servos so to do so we attach an external 5V power source.

Secondly, in this schematic I attached a Servo to channel 1 of the PWM driver, in my code I skip channel 0 and use channels 1-4. 

In my project I set up the Servos like so: Channel 1 is Left Yaw, Channel 2 is Left Pitch, Channel 3 is Right Yaw, Channel 4 is Right Pitch.

####Setting up your RPi with PubNub

Lets begin by getting all the necessary imports for our project by adding the following:

	 from Pubnub import Pubnub                                                                                                                            
     from Adafruit_PWM_Servo_Driver import PWM
     
     import RPi.GPIO as GPIO
     import time
     import sys, os
     import json, httplib
     import base64
     import serial
     import smbus
     
     GPIO.setmode(GPIO.BCM)
     
 We also add the following code which will subscribe us to  PubNub, and also defines all of our callbacks:
     
        #Kill PubNub subscription thread
         def _kill(m, n):
            pubnub.unsubscribe(subchannel)
            
         # ==============================Define Main==================================
          def main():
              print("Starting Main")
              pubnub = Pubnub(publish_key   = 'demo',
                          subscribe_key = 'demo',
                          uuid = "pi")
          
              channel = 'my_channel'                
          
              def _callback(m,n):
                  # ==============================Left Hand==============================
                  left_hand = m.get("left_hand",{})                                                                                                            
                  if left_hand != {}:
                      handleLeft(left_hand)
                  # ==============================Right Hand=============================
                  right_hand = m.get("right_hand",{})
                  if right_hand != {}:
                      handleRight(right_hand)
                  
              #Catch and Print Error
              def _error(m):
                  print(m)
                      
              #Subscribe to subchannel, set callback function to  _callback and set error fucntion to _error
              pubnub.subscribe(channels=channel, callback=_callback, error=_error, connect=_error)
          
          
          # ==============================Call Main====================================
          main()

 In our callback we call handleLeft and handleRight which will parse our JSON object and use the values to drive our Servos. In order for this to work we need to initialize the PWM device. We can do all of this like so:
 
       def handleLeft(left_hand):
            left_yaw = left_hand["left_yaw"]
            left_pitch = left_hand["left_pitch"]
            pwm.setPWM(1, 1, left_yaw)
            pwm.setPWM(2, 2, left_pitch)
      
        def handleRight(right_hand):
            right_yaw = right_hand["right_yaw"]
            right_pitch = right_hand["right_pitch"]
            pwm.setPWM(3, 3, right_yaw)
            pwm.setPWM(4, 4, right_pitch)
      
        # ==============================Servo & PWM Set Up==============================
        
        # Initialise the PWM device using the default address
        #pwm = PWM(0x40, debug=True)
        pwm = PWM(0x40) 
        servoMin = 150  # Min pulse length out of 4096
        servoMax = 600  # Max pulse length out of 4096
        pwm.setPWMFreq(60)                        # Set frequency to 60 Hz

 That is all we need to do! Go ahead and power up your RPi and Leap and try it out for yourself!
 
My project used a custom built driver to speak to our 8x8 LED matrices, for instructions on how to set that up look [here]() and to view the entire finished project code visit the repo.

##Taking It Further

Although this is where our tutorial ends, you can certainly use our code and take this project even further. One could use PubNub History to have the Robots record and then playback a specific dancing sequence. Adding a third servo, once could allow the robots to move along the Roll-axis as well. PubNub is the communication layer between your any two devices, so you are limited only by your own imagination. 
