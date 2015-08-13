#Leap Motion Servo Bot

We use real-time data almost everyday in our lives but those interactions are often limited. We may read and write real-time data but rarely do we interact with real-time data as a physical experience.

With this project we set out to do just that. Using a Leap Motion, a Raspberry Pi and several micro-servos we created a tangible real-time experience. 

When you wave your hand over the Leap Motion, we created Servo Bots to mimic your every motion as you do them. Attached to our Servo Bots we placed 8x8 RGB LED Matrices, which react to each finger on your hand and we used PubNub to allow our Leap Motion to talk directly to our Raspberry Pi with minimal latency.

In this tutorial I am going to teach you how to create a Servo bot capable of dancing along with your hands using Leap Motion, Raspberry Pi, several micro-servos and PubNub.

Although in this example we used PubNub to drive servos along with a LED matrix with a RPi, the same techniques can be applied to control any device of any size. PubNub is simply the communication layer used to allow any two devices to speak. In this case we use PubNub to allow the Leap Motion to be a device controller, but you can imagine using PubNub to initially configure devices or for real time data collection.
