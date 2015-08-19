#Full tutorial: http://www.pubnub.com/blog/motion-controlled-servos-with-leap-motion-raspberry-pi/

The ability to make a physical object mirror the movement of your hands is something out of a science fiction movie. But here at PubNub, we just made it a reality using Leap Motion, Raspberry Pi, several micro-servos and PubNub Data Streams. And even better, you can control the physical object from anywhere on Earth.

#Project Overview

The user controls the servos using the Leap Motion. The two servos mirror the movement of the user’s two individual hands. Attached to the servos are 8×8 RGB LED Matrices, which react to each finger movement on your hand. The Leap Motion communicates directly with the Raspberry Pi via PubNub Data Streams with minimal latency, and the Raspberry Pi then drives the servos.

Here’s the final project in-action:

![gif](https://raw.github.com/justinplatz/LeapMotionServoBots/master/_pubnub_leapmotion_RaspberryPi_servo_iot_DIY.gif)

Overview:

![image](https://raw.github.com/justinplatz/LeapMotionServoBots/master/raspberry-pi-leap-motion-controller-servos-1.png)
