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

GPIO.setup(16, GPIO.IN, pull_up_down=GPIO.PUD_UP)
GPIO.setup(20, GPIO.IN, pull_up_down=GPIO.PUD_UP)

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


# Servo Modes
SERVO_MIN = 200
SERVO_MAX = 600

DISABLED = 0    # Disable Servos
CLONE = 1       # Clone Movements ( R <-> R )
MIRROR = 2      # Mirror Movments ( R <-> L )

servo_mode = DISABLED

# ==============================Servo & PWM Set Up==============================
pwm = PWM(0x40)
bus = smbus.SMBus(1)
pwm.setPWMFreq(60)

# ==============================Define Main==================================
def main():

    resetAVR()
    print("Establishing Connections...")
    pubnub = Pubnub(publish_key   = 'pub-c-f83b8b34-5dbc-4502-ac34-5073f2382d96',
                subscribe_key = 'sub-c-34be47b2-f776-11e4-b559-0619f8945a4f',
                uuid = "pi")

    channel = 'leap2pi'
    GPIO.output(4, False)

    def checkValue(value):
        if (value < SERVO_MIN):
            value = SERVO_MIN
        elif(value > SERVO_MAX):
            value = SERVO_MAX
        return value

    def invertValue(value):
        value = checkValue(value)
        return SERVO_MAX - value + SERVO_MIN

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
        yaw = 0
        pitch = 0
        byte = 0

        # Handle message
        left_hand = m.get("left_hand",{})
        right_hand = m.get("right_hand",{})

        if (servo_mode == MIRROR):
            # Leap Motion: Left hand, Servos: Stage Left
            if left_hand != {}:
                yaw = left_hand["left_yaw"]
                yaw = checkValue(yaw)
                pitch = left_hand["left_pitch"]
                pitch = checkValue(pitch)
                left_byte = left_hand["left_byte"]
                pwm.setPWM(0, 0, yaw)
                pwm.setPWM(1, 1, pitch)
            # Leap Motion: Left hand, Servos: Stage Right
            if right_hand != {}:
                yaw = right_hand["right_yaw"]
                yaw = checkValue(yaw)
                pitch = right_hand["right_pitch"]
                pitch = checkValue(pitch)
                right_byte = right_hand["right_byte"] >> 4
                pwm.setPWM(2, 2, yaw)
                pwm.setPWM(3, 3, pitch)

            byte = left_byte | (right_byte << 4)

        elif (servo_mode == CLONE):
            # Leap Motion: Left hand, Servos: Stage Right
            if left_hand != {}:
                yaw = left_hand["left_yaw"]
                yaw = invertValue(yaw)
                pitch = left_hand["left_pitch"]
                pitch = invertValue(pitch)
                left_byte = left_hand["left_byte"]
                pwm.setPWM(2, 2, yaw)
                pwm.setPWM(3, 3, pitch)
            # Leap Motion: Left hand, Servos: Stage Left
            if right_hand != {}:
                yaw = right_hand["right_yaw"]
                yaw = invertValue(yaw)
                pitch = right_hand["right_pitch"]
                pitch = invertValue(pitch)
                right_byte = right_hand["right_byte"] >> 4
                pwm.setPWM(0, 0, yaw)
                pwm.setPWM(1, 1, pitch)

            byte = (left_byte << 4) | right_byte

        else:
            byte = -1

        #====send i2c=====#
        if (byte >= 0):
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

    # Loop here forever
    while True:
        # Check for switch position
        time.sleep(1)
        if(GPIO.input(16) == False):
            servo_mode = CLONE
        elif (GPIO.input(20) == False):
            servo_mode = MIRROR
        else:
            servo_mode = DISABLED

# ==============================Call Main====================================
main()
