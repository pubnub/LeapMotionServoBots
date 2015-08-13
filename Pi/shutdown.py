import RPi.GPIO as GPIO
import time
import os
GPIO.setmode(GPIO.BCM)
GPIO.setup(14, GPIO.IN,pull_up_down=GPIO.PUD_UP)
GPIO.setup(26, GPIO.OUT)
GPIO.setup(19, GPIO.OUT)
GPIO.output(26, False)
GPIO.output(19, False)

# Stay here until something happens!
while True:

    # Check for button press
    time.sleep(0.25)
    if(GPIO.input(14) == False):

        # Delay and recheck - Short Press = REBOOT
        time.sleep(1)
        if(GPIO.input(14) == False):
            GPIO.output(19, True)
            
            # Final delay and recheck - Long Press = SHUTDOWN
            time.sleep(2)
            if(GPIO.input(14) == False):
                GPIO.output(19, False)
                GPIO.output(26, True)
                os.system("sudo shutdown -h now")
            else:
                os.system("sudo reboot")

            break




