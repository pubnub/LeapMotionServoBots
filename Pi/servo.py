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
GPIO.setup(4, GPIO.OUT)
GPIO.setup(15, GPIO.IN)

# Reset the connected AVR Chip (matrix driver)
def resetAVR():
    GPIO.setup(15, GPIO.OUT)
    GPIO.output(15, False)
    time.sleep(0.25)
    GPIO.setup(15, GPIO.IN)
    time.sleep(0.25)


#Kill PubNub subscription thread
def _kill(m, n):
    pubnub.unsubscribe(subchannel)


def handleLeft(left_hand):
    left_yaw = left_hand["left_yaw"]
    left_pitch = left_hand["left_pitch"]
    left_byte = left_hand["left_byte"]
    pwm.setPWM(0, 0, left_yaw)
    pwm.setPWM(1, 1, left_pitch)
    return left_byte

def handleRight(right_hand):
    right_yaw = right_hand["right_yaw"]
    right_pitch = right_hand["right_pitch"]
    right_byte = right_hand["right_byte"]
    pwm.setPWM(2, 2, right_yaw)
    pwm.setPWM(3, 3, right_pitch)
    return right_byte

# ==============================Servo & PWM Set Up==============================

# Initialise the PWM device using the default address
#pwm = PWM(0x40, debug=True)
pwm = PWM(0x40)
servoMin = 150  # Min pulse length out of 4096
servoMax = 600  # Max pulse length out of 4096

bus = smbus.SMBus(1)

pwm.setPWMFreq(60)                        # Set frequency to 60 Hz

# ==============================Define Main==================================

def main():

    resetAVR()
    print("Establishing Connections...")
    pubnub = Pubnub(publish_key   = 'Your_Pub_Key',
                subscribe_key = 'Your_Sub_Key',
                uuid = "pi")

    channel = 'leap2pi'
    GPIO.output(4, False)

    def _connect(m):
        print("Connected to PubNub!")
        GPIO.output(4, True)

    def _reconnect(m):
        print("Reconnected to PubNub!")
        GPIO.output(4, True)

    def _disconnect(m):
        print("Disconnected from PubNub!")
        GPIO.output(4, False)

    def _callback(m,n):
        left_byte = 0
        right_byte = 0
        # ==============================Left Hand==============================
        left_hand = m.get("left_hand",{})
        if left_hand != {}:
            left_byte = handleLeft(left_hand)
            #print(left_hand)

        # ==============================Right Hand=============================
        right_hand = m.get("right_hand",{})
        if right_hand != {}:
            right_byte= handleRight(right_hand)
            #print(right_hand)

        byte = left_byte | right_byte
        #print(byte)

        #====send i2c=====#
        try:
            bus.write_byte(0x47,byte)
        except IOError:
            print("Error!!!!")
            resetAVR()
            bus.write_byte(0x47,byte)
            

    #Catch and Print Error
    def _error(m):
        GPIO.output(4, False)
        print(m)
    
    #Subscribe to subchannel, set callback function to  _callback and set error fucntion to _error
    pubnub.subscribe(channels=channel, callback=_callback, error=_error, connect=_connect, reconnect=_reconnect, disconnect=_disconnect)


# ==============================Call Main====================================

main()
