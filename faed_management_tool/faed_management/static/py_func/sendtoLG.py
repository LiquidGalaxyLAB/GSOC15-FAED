import os
import time
from kmls_management.models import Kml
from faed_management_tool.settings import BASE_DIR
import subprocess

ip_file = BASE_DIR + '/faed_management/management/ipsettings'


def read_file():
    f = open(ip_file, 'r')
    ip_galaxy = f.read()
    f.close()
    return ip_galaxy


def sync_kmls_to_galaxy(emergency=False):
    file_path = "/tmp/kml/kmls.txt"
    file_path_slave = "/tmp/kml/kmls_4.txt"
    server_path = "/var/www/html"
    if not emergency:
        # os.system(
        #     "sshpass -p lqgalaxy scp faed_management/static/kml/kmls.txt " +
        #     "lg@" + read_file() + ":" + server_path)
        os.system(
        "sshpass -p 'lqgalaxy' scp " + file_path + " lg@" + read_file() +
        ":" + server_path)
        os.system(
        "sshpass -p 'lqgalaxy' scp " + file_path_slave + " lg@" + read_file() +
        ":" + server_path)
        time.sleep(1)
    # print "sshpass -p 'lqgalaxy' scp " + file_path + " lg@" + read_file() +
    # ":" + server_path
    os.system(
        "sshpass -p 'lqgalaxy' scp " + file_path + " lg@" + read_file() +
        ":" + server_path)
    # os.system("echo 1234 | sudo -S cp "+file_path+" /var/www/html")


def a():
    subprocess.Popen("ifconfig getifaddr eth0", shell=True,
                     stdout=subprocess.PIPE)
    # ip_server = p.communicate()[0]
    file = open("faed_management/static/kml/kmls.txt", 'w')

    for i in Kml.objects.filter(visibility=True):
        file.write("http://172.26.17.106:8000" + i.url + "\n")


def sync_kmls_file():
    ip_server = get_server_ip()
    os.system("rm /tmp/kml/kmls.txt")
    os.system("touch /tmp/kml/kmls.txt")
    file = open("/tmp/kml/kmls.txt", 'w')
    for i in Kml.objects.filter(visibility=1):
        file.write("http://" + str(ip_server)[0:(len(ip_server) - 1)] +
                   ":8000/static/kml/" + i.name + "\n")
    file.close()
    sync_kmls_4_file()


def sync_kmls_4_file():
    ip_server = get_server_ip()
    os.system("rm /tmp/kml/kmls_4.txt")
    os.system("touch /tmp/kml/kmls_4.txt")
    file = open("/tmp/kml/kmls_4.txt", 'w')
    for i in Kml.objects.filter(visibility=0):
        file.write("http://" + str(ip_server)[0:(len(ip_server) - 1)] +
                   ":8000/static/kml/" + i.name + "\n")
    file.close()


def get_ip():
    return read_file()


def get_server_ip():
    f = os.popen("hostname -I")
    data = f.read().strip() + ' '
    return data
