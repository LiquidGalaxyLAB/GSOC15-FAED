import os
from django.core.management.base import BaseCommand, CommandError
import re
from kmls_management.models import Kml
from faed_management.models import Incidence, Hangar, DropPoint
from faed_management.static.py_func.sendtoLG import sync_kmls_file, sync_kmls_to_galaxy

from faed_management.static.py_func.weather import generate_weather, generate_weather_image

from kmls_management.kml_generator import create_hangar_polygon

from faed_management_tool.settings import BASE_DIR

from kmls_management.kml_generator import create_droppoint_marker


def write_ip(ip):
    f = open(os.path.dirname(os.path.dirname(os.path.abspath(__file__))) + '/ipsettings', 'w')
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
            patternIp = re.compile("^([m01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                                   "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")
            patternIpAddr = re.compile("^([m01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                                       "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5]):" +
                                       "(\d{1,5})$")
            if patternIp.match(parsed_ip) or patternIpAddr.match(parsed_ip):
                write_ip(parsed_ip)
                if not options['addrport']:
                    app_ip = "127.0.0.1:8000"
                else:
                    app_ip = options['addrport']
                self.stdout.write(self.style.SUCCESS('Successfully changed the ip to "%s"' % parsed_ip))
                self.clear_databases()
                self.create_system_files()
                self.create_base_kml(app_ip)
                os.system("python manage.py runserver " + app_ip)
            else:
                self.stdout.write(self.style.error('Ip "%s" have an incorrect format' % parsed_ip))
        except:
            raise CommandError('FAED cannot be raised')

    def clear_databases(self):
        self.stdout.write("Deleting data from Kml and Incidences ...")
        try:
            Kml.objects.all().delete()
            Incidence.objects.all().delete()
        except:
            self.stdout.write(self.style.error("Error deleting data from the tables."))

    def create_system_files(self):
        self.stdout.write("Creating startUp files...")
        os.system("mkdir /tmp/kml")
        os.system("touch /tmp/kml/kmls.txt")

    def create_base_kml(self, app_ip):
        path = BASE_DIR + "/faed_management/static/kml/"
        self.create_hangars(path)
        self.create_droppoints(path)
        self.stdout.write("Creating Weather Kml...")
        generate_weather_image(BASE_DIR + "/faed_management", app_ip)
        self.stdout.write("KMLs done")
        sync_kmls_file()
        sync_kmls_to_galaxy()

    def create_hangars(self, path):
        self.stdout.write("Creating Hangars Kml...")
        for item in Hangar.objects.all():
            name = "hangar" + str(item.id) + ".kml"
            Kml(name=name, url=path + name).save()
            create_hangar_polygon(item, path + name)

    def create_droppoints(self, path):
        self.stdout.write("Creating Droppoints Kml...")
        for item in DropPoint.objects.all():
            name = "droppoint" + str(item.id) + ".kml"
            Kml(name=name, url=path + name).save()
            create_droppoint_marker(item, path + name)