import os, time
from kmls_management.models import Kml
import subprocess


def sync_kmls_to_galaxy(emergency=False):
    file_path = "/tmp/kml/kmls.txt"
    server_path = "/var/www/html"

    if not emergency:
        os.system("sshpass -p 'lqgalaxy' scp faed_management/static/kml/kmls.txt lg@172.26.17.21:"+server_path)
        time.sleep(1)
    os.system("sshpass -p 'lqgalaxy' scp "+file_path+" lg@172.26.17.21:"+server_path)


def a():
    p = subprocess.Popen("ifconfig getifaddr eth0", shell=True, stdout=subprocess.PIPE)
    ip_server = p.communicate()[0]
    file = open("faed_management/static/kml/kmls.txt",'w')

    for i in Kml.objects.filter(visibility=True):
        file.write("http://172.26.17.106:8000"+i.url+"\n")


def sync_kmls_file():

    p = subprocess.Popen("ifconfig eth0 | grep 'inet:' | cut -d: -f2 | awk '{print $1}'", shell=True, stdout=subprocess.PIPE)
    ip_server = p.communicate()[0]

    os.system("rm /tmp/kml/kmls.txt")
    os.system("touch /tmp/kml/kmls.txt")
    file = open("/tmp/kml/kmls.txt",'w')

    for i in Kml.objects.filter(visibility=True):
        file.write("http://"+ str(ip_server)[0:(len(ip_server)-1)]+":8000/static/kml/"+i.name+"\n")
    # file.write("http://"+ str(ip_server)[0:(len(ip_server)-1)]+":8000/static/kml/ex_w.kml\n")

    file.close()
