import json
import requests
import time
import signal
import sys
import subprocess
import os

session=requests.Session()
url_prefix = "http://127.0.0.1:8080/interactivespaces/"
activity_id = {}#{'captain':'0','serial':'0','udp':'0','mavlink':'0','generator':'0','processor':'0'}

#Change to serial to start using on serial port
comms_port ='udp'

global master_pid
global controller_pid
def signal_handler(signal, frame):
    print "Closing everything"
    exit_sequence(comms_port)
    #if master_pid:
    #    os.kill(master_pid.pid,signal)
    #if controller_pid:
    #    os.kill(controller_pid.pid,signal)
    sys.exit(0)

def make_request(url):
    live_activity=requests.get(url).content
    return json.loads(live_activity)

def url_startup(uid):
    return "liveactivity/"+uid+"/startup.json"

def url_activate(uid):
    return "liveactivity/"+uid+"/activate.json"

def url_deactivate(uid):
    return "liveactivity/"+uid+"/deactivate.json"

def url_shutdown(uid):
    return "liveactivity/"+uid+"/shutdown.json"

def url_deploy(uid):
    return "liveactivity/"+uid+"/deploy.json"

def url_status(uid):
    return "liveactivity/"+uid+"/status.json"

def process_result(result_data):
    if result_data['result']=='success':
        #print "Success"
        return 'success'
    else:
        print "Could not contact master properly"
        return 'fail'

def launch_sequence(comms_port):
    if process_result(make_request(url_prefix+url_startup(activity_id['mavlink']))) == 'success':
        print "Success in starting mavlink activity"
        time.sleep(4)
        if process_result(make_request(url_prefix+url_startup(activity_id[comms_port]))) == 'success':
            print "Success in starting communications activity"
            time.sleep(1)
            if process_result(make_request(url_prefix+url_startup(activity_id['generator']))) == 'success':
                print "Success in starting waypoint generator activity"
                time.sleep(1)
                if process_result(make_request(url_prefix+url_activate(activity_id['mavlink']))) == 'success':
                    print "Success in activating mavlink activity"
                    time.sleep(1)
                    if process_result(make_request(url_prefix+url_activate(activity_id[comms_port]))) == 'success':
                        print "Success in activating communications activity"
                        time.sleep(1)
                        if process_result(make_request(url_prefix+url_activate(activity_id['generator']))) == 'success':
                            print "Success in activating waypoint generator activity"
                            time.sleep(2)
                            if process_result(make_request(url_prefix+url_startup(activity_id['captain']))) == 'success':
                                print "Success in starting captain activity"
                                time.sleep(2)
                                if process_result(make_request(url_prefix+url_activate(activity_id['captain']))) == 'success':
                                    print "Success in activating captain activity"
                                    time.sleep(0.5)
                                    if process_result(make_request(url_prefix+url_startup(activity_id['processor']))) == 'success':
                                        print "Success in starting waypoint processor activity"
                                        time.sleep(1)
                                        if process_result(make_request(url_prefix+url_activate(activity_id['processor']))) == 'success':
                                            print "Success in activating waypoint processor activity"
                                            time.sleep(0.5)
                                            print 'Launched all the activities successfully'
                                            return
    print "Could not start all the activities"

def exit_sequence(comms_port):
    if process_result(make_request(url_prefix+url_shutdown(activity_id['processor']))) == 'success':
        print "Shutting down waypoint processor activity"
        time.sleep(0.5)
        if process_result(make_request(url_prefix+url_shutdown(activity_id['captain']))) == 'success':
            print "Shutting down captain activity"
            time.sleep(10)
            if process_result(make_request(url_prefix+url_shutdown(activity_id['generator']))) == 'success':
                print "Shutting down waypoint generator activity"
                time.sleep(0.5)
                if process_result(make_request(url_prefix+url_shutdown(activity_id[comms_port]))) == 'success':
                    print "Shutting down communications activity"
                    time.sleep(1)
                    if process_result(make_request(url_prefix+url_shutdown(activity_id['mavlink']))) == 'success':
                        print "Shutting down mavlink activity"
                        time.sleep(2)
                        subprocess._cleanup()
                        print "Everything shut down"
                        return
    print "Could not shutdown all the activities"

#url_live_activity = "http://127.0.0.1:8080/interactivespaces/liveactivity/all.json";
#live_activity=requests.get(url_prefix+"liveactivity/all.json").content
#print live_activity
data = {}
try:
    data= make_request(url_prefix+"liveactivity/all.json")
except requests.exceptions.ConnectionError:
    #os.setpgrp()
    global master_pid
    global controller_pid
    master_pid=subprocess.Popen(['gnome-terminal', '-x', './../../master/bin/isstartup'])
    time.sleep(10)
    controller_pid = subprocess.Popen(['gnome-terminal', '-x', './../../controller/bin/isstartup'])
    time.sleep(10)
    try:
        data= make_request(url_prefix+"liveactivity/all.json")
    except requests.exceptions.ConnectionError:
        time.sleep(10)
        data= make_request(url_prefix+"liveactivity/all.json")

if data['result']=='success':
    for i in range(0,len(data['data']),1):
        if data['data'][i]['activity']['identifyingName']== 'is.erle.captain':
            activity_id['captain'] = data['data'][i]['id']
        elif data['data'][i]['activity']['identifyingName']== 'is.erle.comm.serial':
            activity_id['serial'] = data['data'][i]['id']
        elif data['data'][i]['activity']['identifyingName']== 'is.erle.comms':
            activity_id['udp'] = data['data'][i]['id']
        elif data['data'][i]['activity']['identifyingName']== 'is.erle.mavlink':
            activity_id['mavlink'] = data['data'][i]['id']
        elif data['data'][i]['activity']['identifyingName']== 'is.erle.waypoint.generator':
                activity_id['generator'] = data['data'][i]['id']
        elif data['data'][i]['activity']['identifyingName']== 'is.erle.waypoint.processor':
            activity_id['processor'] = data['data'][i]['id']
    # print data['data'][9]
    #print activity_id['captain']
    if len(activity_id)>=5:
        print 'Starting all the activities'
        launch_sequence(comms_port)
        signal.signal(signal.SIGINT, signal_handler)
        print('Press Ctrl+C to exit')
        signal.pause()
    else:
        print 'Not all the activities are installed'
        print 'Only the following activities are installed, Install rest of them'
        print activity_id
else:
    print "Could not contact master properly"

