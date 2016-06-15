import os
from django.core.management.base import BaseCommand, CommandError
import re
from kmls_management.models import Kml
from faed_management.models import Incidence, Hangar, DropPoint
from faed_management.static.py_func.sendtoLG import sync_kmls_file, \
    sync_kmls_to_galaxy, get_server_ip
from faed_management.static.py_func.weather import generate_weather
from kmls_management.kml_generator import create_hangar_polygon, \
    create_droppoint_marker, hangar_influence, faed_logo_kml
from faed_management_tool.settings import BASE_DIR


def write_ip(ip):
    f = open(os.path.dirname(os.path.dirname(os.path.abspath(__file__))) +
             '/ipsettings', 'w')
    f.write(ip)
    f.close()


class Command(BaseCommand):
    help = 'Set the <ip> of the galaxy Liquid system.'

    def add_arguments(self, parser):
        parser.add_argument('ip', nargs='?',
                            help='Mandatory galaxy liquid ip address')
        parser.add_argument('addrport', nargs='?',
                            help='Optional port number, or ipaddr:port')

    def handle(self, *args, **options):
        try:
            parsed_ip = options['ip']
            patternIp = re.compile(
                "^([m01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]" +
                "\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")
            patternIpAddr = re.compile(
                "^([m01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?" +
                "|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])" +
                "\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5]):(\d{1,5})$")
            if patternIp.match(parsed_ip) or patternIpAddr.match(parsed_ip):
                write_ip(parsed_ip)
                if not options['addrport']:
                    ip_server = get_server_ip()
                    app_ip = str(ip_server)[0:(len(ip_server) - 1)] + ":8000"
                else:
                    app_ip = options['addrport']
                self.stdout.write(self.style.SUCCESS(
                    'Successfully changed the ip to "%s"' % parsed_ip))
                path = BASE_DIR + "/faed_management/static/kml/"
                # Erasing the files and Databases with KMl and
                # other dynamic information created during the FAED run.
                self.create_system_files()
                os.system("rm -r " + path + "*")
                self.clear_databases()
                # Creating the KMl files with the information of the Database
                self.create_base_kml(app_ip, path)
                os.system("python manage.py runserver " + app_ip)
            else:
                self.stdout.write(
                    self.style.error(
                        'Ip "%s" have an incorrect format' % parsed_ip))
        except:
            raise CommandError('FAED cannot be raised')

    def clear_databases(self):
        self.stdout.write("Deleting data from Kml and Incidences ...")
        try:
            Kml.objects.all().delete()
            Incidence.objects.all().delete()
            sync_kmls_file()
            sync_kmls_to_galaxy()
        except Exception:
            self.stdout.write(self.style.error("Error deleting data from" +
                                               " the tables."))

    def create_system_files(self):
        self.stdout.write("Creating startUp files...")
        os.system("mkdir /tmp/kml")
        os.system("touch /tmp/kml/kmls.txt")
        os.system("touch /tmp/kml/kmls_slave.txt")

    def create_base_kml(self, app_ip, path):
        self.create_hangars(path)
        self.create_droppoints(path)
        self.create_logo(path, app_ip)
        self.stdout.write("Creating Weather Kml...")
        generate_weather(BASE_DIR + "/faed_management/static/kml/")
        self.stdout.write("KMLs done")

    def create_hangars(self, path):
        self.stdout.write("Creating Hangars Kml...")
        for item in Hangar.objects.all():
            name = "hangar_" + str(item.id) + ".kml"
            create_hangar_polygon(item, path + name)
            name_inf = hangar_influence(item, path +
                                        "hangar_" + str(item.id) + "_inf.kml")
            Kml(name=name, url=path + name).save()
            Kml(name=name_inf, url=path + name_inf).save()

    def create_droppoints(self, path):
        self.stdout.write("Creating Droppoints Kml...")
        for item in DropPoint.objects.all():
            name = "droppoint_" + str(item.id) + ".kml"
            create_droppoint_marker(item, path + name)
            Kml(name=name, url=path + name).save()

    def create_logo(self, path, app_ip):
        self.stdout.write("Creating Logo Kml...")
        name = "faed_logo"
        faed_logo_kml(path + name, "http://" + app_ip +
                      "/static/img/static_icon.png")
        Kml(name=name + ".kml", url=path + name + ".kml", visibility=0).save()
