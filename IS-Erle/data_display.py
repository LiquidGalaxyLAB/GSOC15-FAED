import subprocess
import time
import signal
import sys
import os
def kill(proc_pid):
    os.kill(proc_pid,signal.SIGTSTP)
def signal_handler(signal, frame):
    print "Closing everything"
    kill(pid1.pid)
    kill(pid2.pid)
    kill(pid3.pid)
    kill(pid4.pid)
    kill(pid5.pid)
    kill(pid6.pid)
    sys.exit(0)

pid1 = subprocess.Popen(['gnome-terminal', '-e','/bin/bash -c \'rostopic echo /controller1/mavlink/sensors/gps\'',])
time.sleep(0.1)
pid2 = subprocess.Popen(['gnome-terminal', '-e', '/bin/bash -c \'rostopic echo /controller1/mavlink/sensors/imu\''])
time.sleep(0.1)
pid3 = subprocess.Popen(['gnome-terminal', '-e', '/bin/bash -c \'rostopic echo /controller1/mavlink/sensors/pressure\''])
time.sleep(0.1)
pid4 = subprocess.Popen(['gnome-terminal', '-e', '/bin/bash -c \'rostopic echo /controller1/mavlink/hud\''])
time.sleep(0.1)
pid5 = subprocess.Popen(['gnome-terminal', '-e', '/bin/bash -c \'rostopic echo /controller1/mavlink/controller/nav\''])
time.sleep(0.1)
pid6 = subprocess.Popen(['gnome-terminal', '-e', '/bin/bash -c \'rostopic echo /controller1/mavlink/servoOutput\''])
time.sleep(0.1)
pid7 = subprocess.Popen(['gnome-terminal', '-e', '/bin/bash -c \'rostopic echo /controller1/mavlink/rcInput\''])
time.sleep(0.1)
signal.signal(signal.SIGINT, signal_handler)
print('Press Ctrl+C to exit')
signal.pause()
